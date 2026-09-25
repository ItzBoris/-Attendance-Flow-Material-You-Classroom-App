package com.example.util

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer

class QrCodeAnalyzer(
    private val onQrCodeScanned: (String) -> Unit,
    private val onInvalidQrDetected: ((String) -> Unit)? = null
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        val hints = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)
        )
        setHints(hints)
    }

    private var lastFrameTimestamp = 0L
    private var lastScannedText: String? = null
    private var lastScannedTimestamp = 0L
    private var lastInvalidTimestamp = 0L

    // Throttle frame processing to ~30 FPS (30ms) for fast 1-second recognition speed
    private val FRAME_THROTTLE_MS = 30L

    // Debounce cooldown for identical QR payloads (3 seconds)
    private val DEBOUNCE_COOLDOWN_MS = 3000L

    // Debounce cooldown for invalid QR notifications (2.5 seconds)
    private val INVALID_DEBOUNCE_COOLDOWN_MS = 2500L

    // Minimum interval between distinct QR scan callbacks (800ms)
    private val MIN_SCAN_GAP_MS = 800L

    override fun analyze(image: ImageProxy) {
        val currentTime = System.currentTimeMillis()

        // 1. Frame throttle
        if (currentTime - lastFrameTimestamp < FRAME_THROTTLE_MS) {
            image.close()
            return
        }
        lastFrameTimestamp = currentTime

        if (image.format in YUV_FORMATS) {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val bytes = buffer.toByteArray()
            val rowStride = plane.rowStride

            // Correct PlanarYUVLuminanceSource parameters with rowStride
            var source: LuminanceSource = PlanarYUVLuminanceSource(
                bytes,
                rowStride,
                image.height,
                0,
                0,
                image.width,
                image.height,
                false
            )

            // Account for camera sensor rotation
            val rotation = image.imageInfo.rotationDegrees
            if (rotation == 90 && source.isRotateSupported) {
                source = source.rotateCounterClockwise()
            } else if (rotation == 180 && source.isRotateSupported) {
                source = source.rotateCounterClockwise().rotateCounterClockwise()
            } else if (rotation == 270 && source.isRotateSupported) {
                source = source.rotateCounterClockwise().rotateCounterClockwise().rotateCounterClockwise()
            }

            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

            try {
                val result = reader.decodeWithState(binaryBitmap)
                val qrText = result?.text?.trim()

                if (!qrText.isNullOrBlank()) {
                    // Only process frame if it represents a valid classroom session QR payload
                    val validation = QrCodeGenerator.validateToken(qrText)
                    if (validation is QrCodeGenerator.ScanValidationResult.Success) {
                        // Debouncing check: Ignore identical or too-frequent frame scans
                        val isDuplicate = (qrText == lastScannedText) &&
                                (currentTime - lastScannedTimestamp < DEBOUNCE_COOLDOWN_MS)
                        val isTooFrequent = (currentTime - lastScannedTimestamp < MIN_SCAN_GAP_MS)

                        if (!isDuplicate && !isTooFrequent) {
                            lastScannedText = qrText
                            lastScannedTimestamp = currentTime
                            onQrCodeScanned(qrText)
                        }
                    } else {
                        // Invalid QR code detected in frame
                        val isInvalidDuplicate = (currentTime - lastInvalidTimestamp < INVALID_DEBOUNCE_COOLDOWN_MS)
                        if (!isInvalidDuplicate) {
                            lastInvalidTimestamp = currentTime
                            onInvalidQrDetected?.invoke(qrText)
                        }
                    }
                }
            } catch (e: NotFoundException) {
                // No QR code found in this frame
            } catch (e: Exception) {
                // Ignore decoding errors
            } finally {
                reader.reset()
                image.close()
            }
        } else {
            image.close()
        }
    }

    fun resetDebounce() {
        lastScannedText = null
        lastScannedTimestamp = 0L
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()
        val data = ByteArray(remaining())
        get(data)
        return data
    }

    companion object {
        private val YUV_FORMATS = listOf(
            android.graphics.ImageFormat.YUV_420_888,
            android.graphics.ImageFormat.YUV_422_888,
            android.graphics.ImageFormat.YUV_444_888
        )
    }
}


