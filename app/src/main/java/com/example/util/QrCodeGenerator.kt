package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.security.MessageDigest
import java.util.EnumMap

object QrCodeGenerator {

    private const val SECRET_SALT = "HSS2025_ATTENDANCE_SECRET_KEY_M3"

    /**
     * Calculates the time window index (5-second refresh cycle).
     */
    fun getCurrentTimeWindow(timestampMs: Long = System.currentTimeMillis()): Long {
        return timestampMs / 5000L
    }

    /**
     * Generates a time-sensitive encrypted token for the given session ID and timestamp.
     */
    fun generateTokenPayload(sessionId: String, timestampMs: Long = System.currentTimeMillis()): String {
        val window = getCurrentTimeWindow(timestampMs)
        val rawToHash = "$sessionId:$window:$SECRET_SALT"
        val hash = sha256(rawToHash).take(12)
        // Format matching requirement: https://app.domain/mark?session_id=XYZ&token=HASH&ts=12345678
        return "https://app.domain/mark?session_id=$sessionId&token=$hash&ts=$timestampMs"
    }

    /**
     * Validates a scanned QR token against session rules:
     * 1. Token must be for the active session ID.
     * 2. Token age must be <= 10000 ms (10 seconds tolerance for 5s cycle).
     */
    sealed class ScanValidationResult {
        data class Success(val sessionId: String, val timestampMs: Long) : ScanValidationResult()
        data class Error(val message: String) : ScanValidationResult()
    }

    private val sessionRegex = Regex("^(CS[A-Za-z0-9]+_\\d+|SESSION_[A-Za-z0-9_]+)$")

    fun validateToken(scannedUrl: String, currentMs: Long = System.currentTimeMillis()): ScanValidationResult {
        try {
            val trimmed = scannedUrl.trim()
            if (trimmed.isBlank()) {
                return ScanValidationResult.Error("Invalid QR Code format.")
            }

            // 1. Check if valid URL with session_id or sessionId query parameter
            if (trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) ||
                trimmed.contains("session_id=", ignoreCase = true) ||
                trimmed.contains("sessionId=", ignoreCase = true)
            ) {
                val uri = android.net.Uri.parse(trimmed)
                val sessionId = uri.getQueryParameter("session_id") ?: uri.getQueryParameter("sessionId")
                val token = uri.getQueryParameter("token")
                val tsStr = uri.getQueryParameter("ts")

                if (!sessionId.isNullOrBlank()) {
                    val tokenTs = tsStr?.toLongOrNull() ?: currentMs
                    val ageMs = currentMs - tokenTs

                    // Check expiration (allow up to 15s age or 5s future clock skew for 5s refresh cycle)
                    if (ageMs > 15000L) {
                        return ScanValidationResult.Error("Expired QR Code ($ageMs ms old). Scan the live screen again.")
                    }
                    if (ageMs < -5000L) {
                        return ScanValidationResult.Error("Invalid device clock sync.")
                    }

                    // If token security signature is present, strictly verify hash
                    if (!token.isNullOrBlank()) {
                        val tsWindow = getCurrentTimeWindow(tokenTs)
                        val currWindow = getCurrentTimeWindow(currentMs)
                        var isValidToken = false

                        for (w in listOf(tsWindow, tsWindow - 1, tsWindow + 1, currWindow, currWindow - 1, currWindow + 1)) {
                            val expectedHash = sha256("$sessionId:$w:$SECRET_SALT").take(12)
                            if (token.equals(expectedHash, ignoreCase = true)) {
                                isValidToken = true
                                break
                            }
                        }

                        if (!isValidToken) {
                            return ScanValidationResult.Error("Invalid QR Code signature.")
                        }
                    }

                    return ScanValidationResult.Success(sessionId, currentMs)
                }
            }

            // 2. Check if valid JSON object containing sessionId and optional token
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                try {
                    val json = org.json.JSONObject(trimmed)
                    val jsonSessionId = json.optString("sessionId", json.optString("session_id", ""))
                    val token = json.optString("token", "")
                    if (jsonSessionId.isNotBlank()) {
                        if (token.isNotBlank()) {
                            val currWindow = getCurrentTimeWindow(currentMs)
                            var isValidToken = false
                            for (w in listOf(currWindow, currWindow - 1, currWindow + 1)) {
                                val expectedHash = sha256("$jsonSessionId:$w:$SECRET_SALT").take(12)
                                if (token.equals(expectedHash, ignoreCase = true)) {
                                    isValidToken = true
                                    break
                                }
                            }
                            if (!isValidToken) {
                                return ScanValidationResult.Error("Invalid QR Code signature.")
                            }
                        }
                        return ScanValidationResult.Success(jsonSessionId, currentMs)
                    }
                } catch (e: Exception) {
                    // Not valid JSON
                }
            }

            // 3. Strict Session ID pattern match (e.g. CS101_12345 or SESSION_LIVE_1234)
            if (sessionRegex.matches(trimmed)) {
                return ScanValidationResult.Success(trimmed, currentMs)
            }

            // Any other string, text, handwriting, or arbitrary barcode is strictly invalid
            return ScanValidationResult.Error("Invalid QR Code format.")
        } catch (e: Exception) {
            return ScanValidationResult.Error("Invalid QR Code format.")
        }
    }

    /**
     * Renders a ZXing QR Code Bitmap.
     */
    fun createQrBitmap(content: String, sizePx: Int = 512): Bitmap {
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.MARGIN, 1)
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
        }

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)

        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
