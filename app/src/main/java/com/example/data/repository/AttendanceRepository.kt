package com.example.data.repository

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.example.data.local.AttendanceDao
import com.example.data.local.AttendanceRecord
import com.example.data.local.AttendanceSession
import com.example.data.local.StudentProfile
import com.example.util.QrCodeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AttendanceRepository(
    private val dao: AttendanceDao,
    private val context: Context
) {

    private val googleScriptUrl = "https://script.google.com/macros/s/AKfycbztbE3jZSnBCjcptoVkwrEGjMgP7r2YfjdgqP7kJEtCa5BiFMee9y6NCC8lmsQP4VXEsA/exec"

    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val activeStudentProfileFlow: Flow<StudentProfile?> = dao.getActiveStudentProfileFlow()
    val activeSessionFlow: Flow<AttendanceSession?> = dao.getActiveSessionFlow()

    fun getRecordsForSessionFlow(sessionId: String): Flow<List<AttendanceRecord>> {
        return dao.getRecordsForSessionFlow(sessionId)
    }

    fun getPresentCountFlow(sessionId: String): Flow<Int> {
        return dao.getPresentCountFlow(sessionId)
    }

    /**
     * Unique Device Identifier for Anti-Proxy / Device Lock constraint.
     */
    fun getDeviceId(): String {
        return try {
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {
                androidId
            } else {
                "DEV_${Build.MODEL}_${Build.FINGERPRINT.hashCode()}"
            }
        } catch (e: Exception) {
            "DEV_LOCAL_UNKNOWN"
        }
    }

    // --- Teacher Auth ---
    fun verifyTeacherCredentials(username: String, pass: String): Boolean {
        return username.trim() == "admin" && pass.trim() == "HSS2025"
    }

    // --- Student Auth & OTP ---
    suspend fun registerStudent(
        fullName: String,
        rollNumber: String,
        phoneNumber: String
    ): StudentProfile {
        val profile = StudentProfile(
            rollNumber = rollNumber.trim().uppercase(),
            fullName = fullName.trim(),
            phoneNumber = phoneNumber.trim(),
            deviceId = getDeviceId()
        )
        dao.saveStudentProfile(profile)
        return profile
    }

    suspend fun signOutStudent() {
        dao.clearStudentProfile()
    }

    suspend fun getActiveStudentProfile(): StudentProfile? {
        return dao.getActiveStudentProfile()
    }

    // --- Classroom Sessions ---
    suspend fun startSession(subjectCode: String, courseName: String): AttendanceSession {
        // End any active sessions first
        val currentActive = dao.getActiveSession()
        if (currentActive != null) {
            dao.endSession(currentActive.sessionId)
        }

        val cleanCode = subjectCode.trim().uppercase().ifEmpty { "CS101" }
        val cleanName = courseName.trim().ifEmpty { "Mobile App Development" }
        val sessionId = "${cleanCode}_${System.currentTimeMillis() % 100000}"

        val session = AttendanceSession(
            sessionId = sessionId,
            subjectCode = cleanCode,
            courseName = cleanName,
            startTime = System.currentTimeMillis(),
            isActive = true,
            teacherUsername = "admin"
        )
        dao.saveSession(session)
        return session
    }

    suspend fun endSession(sessionId: String) {
        dao.endSession(sessionId)
    }

    suspend fun getActiveSession(): AttendanceSession? {
        return dao.getActiveSession()
    }

    // --- Post to Google Apps Script Backend ---
    suspend fun submitToGoogleSheet(
        rollNumber: String,
        studentName: String,
        deviceId: String,
        sessionId: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = JSONObject().apply {
                put("rollNumber", rollNumber)
                put("studentName", studentName)
                put("deviceId", deviceId)
                put("sessionId", sessionId)
            }.toString()

            val mediaType = "text/plain;charset=utf-8".toMediaType()
            val requestBody = jsonPayload.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(googleScriptUrl)
                .header("Content-Type", "text/plain;charset=utf-8")
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (responseBody.isNotBlank()) {
                val jsonResponse = JSONObject(responseBody)
                val result = jsonResponse.optString("result")
                val message = jsonResponse.optString("message")

                if (result.equals("success", ignoreCase = true)) {
                    val returnMsg = if (message.isNotBlank()) message else "✅ Attendance Recorded in Google Sheet!"
                    return@withContext Pair(true, returnMsg)
                } else if (result.equals("error", ignoreCase = true)) {
                    val returnMsg = if (message.isNotBlank()) message else "Attendance already marked for this session!"
                    return@withContext Pair(false, returnMsg)
                }
            }

            if (response.isSuccessful) {
                Pair(true, "✅ Attendance Recorded in Google Sheet!")
            } else {
                Pair(false, "Google Sheet HTTP Error (${response.code})")
            }
        } catch (e: Exception) {
            Pair(false, "Submission Error: ${e.localizedMessage ?: "Network connection failed"}")
        }
    }

    // --- Fetch from Google Apps Script Backend ---
    suspend fun fetchRecordsFromGoogleSheet(sessionId: String): Result<List<AttendanceRecord>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(googleScriptUrl)
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (responseBody.isNotBlank()) {
                val jsonResponse = JSONObject(responseBody)
                val recordsArray = jsonResponse.optJSONArray("records")
                    ?: jsonResponse.optJSONObject("data")?.optJSONArray("records")

                if (recordsArray != null) {
                    val list = mutableListOf<AttendanceRecord>()
                    val courseCode = sessionId.substringBefore("_").ifBlank { sessionId }

                    for (i in 0 until recordsArray.length()) {
                        val item = recordsArray.getJSONObject(i)
                        val itemSessionId = item.optString("sessionId", item.optString("session_id", ""))

                        val itemSubjectCode = itemSessionId.split("_", "-", " ").firstOrNull()?.trim()?.uppercase() ?: ""
                        val targetCourseCode = courseCode.split("_", "-", " ").firstOrNull()?.trim()?.uppercase() ?: ""

                        val matchesSession = sessionId.isBlank() ||
                                itemSessionId.isBlank() ||
                                itemSessionId.equals(sessionId, ignoreCase = true) ||
                                (targetCourseCode.isNotBlank() && itemSubjectCode == targetCourseCode)

                        if (matchesSession) {
                            val roll = item.optString("rollNumber")
                                .ifBlank { item.optString("roll_number") }
                                .ifBlank { item.optString("roll") }
                                .ifBlank { item.optString("Roll Number") }
                                .ifBlank { item.optString("Roll") }
                                .ifBlank { "N/A" }

                            val name = item.optString("studentName")
                                .ifBlank { item.optString("student_name") }
                                .ifBlank { item.optString("name") }
                                .ifBlank { item.optString("Name") }
                                .ifBlank { item.optString("Student Name") }
                                .ifBlank { "N/A" }

                            val device = item.optString("deviceId")
                                .ifBlank { item.optString("device_id") }
                                .ifBlank { item.optString("Device ID") }
                                .ifBlank { "ANDROID_DEV" }

                            val timeStr = item.optString("timestamp")
                                .ifBlank { item.optString("scannedTimestamp") }
                                .ifBlank { item.optString("Timestamp") }

                            val ts = timeStr.toLongOrNull() ?: System.currentTimeMillis()

                            if (roll != "N/A" || name != "N/A") {
                                list.add(
                                    AttendanceRecord(
                                        id = (i + 1).toLong(),
                                        sessionId = if (itemSessionId.isNotBlank()) itemSessionId else sessionId,
                                        studentRollNumber = roll,
                                        studentName = name,
                                        studentPhone = "",
                                        deviceId = device,
                                        scannedTimestamp = ts,
                                        status = "PRESENT",
                                        tokenSignature = ""
                                    )
                                )
                            }
                        }
                    }
                    return@withContext Result.success(list)
                }
            }
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- QR Scan Processing & Anti-Proxy Verification ---
    sealed class AttendanceScanResult {
        data class Success(
            val studentName: String,
            val rollNumber: String,
            val timestampMs: Long,
            val sessionId: String = "",
            val message: String = "✅ Attendance Recorded in Google Sheet!"
        ) : AttendanceScanResult()

        data class Error(val message: String) : AttendanceScanResult()
    }

    fun parseSessionId(scannedUrl: String): String? {
        try {
            val uri = android.net.Uri.parse(scannedUrl)
            val querySessionId = uri.getQueryParameter("session_id") ?: uri.getQueryParameter("sessionId")
            if (!querySessionId.isNullOrBlank()) {
                return querySessionId
            }
            val trimmed = scannedUrl.trim()
            if (trimmed.startsWith("{")) {
                val json = JSONObject(trimmed)
                val jsonSessionId = json.optString("sessionId", json.optString("session_id", ""))
                if (jsonSessionId.isNotBlank()) return jsonSessionId
            }
            if (trimmed.isNotBlank() && (trimmed.startsWith("CS") || trimmed.startsWith("SESSION_") || trimmed.contains("session_id="))) {
                return trimmed
            }
        } catch (e: Exception) {
            // ignore
        }
        return null
    }

    suspend fun processScannedQrToken(
        tokenUrl: String,
        studentProfile: StudentProfile
    ): AttendanceScanResult {
        // 1. Verify Token format, age & Security Hash
        val validation = QrCodeGenerator.validateToken(scannedUrl = tokenUrl)

        val parsedSessionId = when (validation) {
            is QrCodeGenerator.ScanValidationResult.Error -> {
                return AttendanceScanResult.Error(validation.message)
            }
            is QrCodeGenerator.ScanValidationResult.Success -> {
                validation.sessionId
            }
        }

        val rollNumber = studentProfile.rollNumber.ifBlank { "2506028" }
        val studentName = studentProfile.fullName.ifBlank { "Piyush Kumar" }
        val deviceId = studentProfile.deviceId.ifBlank { "ANDROID_DEVICE_ID" }

        // 2. Record present state in local Room DB first
        val now = System.currentTimeMillis()
        val record = AttendanceRecord(
            sessionId = parsedSessionId,
            studentRollNumber = rollNumber,
            studentName = studentName,
            studentPhone = studentProfile.phoneNumber,
            deviceId = deviceId,
            scannedTimestamp = now,
            status = "PRESENT",
            tokenSignature = tokenUrl
        )
        try {
            dao.insertAttendanceRecord(record)
        } catch (e: Exception) {
            // non-blocking local record insertion
        }

        // 3. Submit POST request to Google Apps Script Web App
        val (sheetSuccess, sheetMessage) = submitToGoogleSheet(
            rollNumber = rollNumber,
            studentName = studentName,
            deviceId = deviceId,
            sessionId = parsedSessionId
        )

        if (sheetSuccess) {
            return AttendanceScanResult.Success(
                studentName = studentName,
                rollNumber = rollNumber,
                timestampMs = now,
                sessionId = parsedSessionId,
                message = if (sheetMessage.startsWith("✅")) sheetMessage else "✅ $sheetMessage"
            )
        }

        // If Google Sheet submission failed due to network or script outage, local record is preserved
        if (sheetMessage.contains("Error", ignoreCase = true) ||
            sheetMessage.contains("failed", ignoreCase = true) ||
            sheetMessage.contains("Socket", ignoreCase = true) ||
            sheetMessage.contains("Connection", ignoreCase = true)
        ) {
            return AttendanceScanResult.Success(
                studentName = studentName,
                rollNumber = rollNumber,
                timestampMs = now,
                sessionId = parsedSessionId,
                message = "✅ Attendance Recorded (Saved Locally)"
            )
        }

        return AttendanceScanResult.Error(sheetMessage)
    }

    suspend fun getSessionRecords(sessionId: String): List<AttendanceRecord> {
        return dao.getRecordsForSession(sessionId)
    }

    suspend fun getAllLocalRecords(): List<AttendanceRecord> {
        return dao.getAllAttendanceRecords()
    }

    suspend fun clearOldAttendanceCache(maxAgeMillis: Long = 24 * 60 * 60 * 1000L): Int = withContext(Dispatchers.IO) {
        val cutoffTimestamp = System.currentTimeMillis() - maxAgeMillis
        val deletedRecords = dao.deleteOldAttendanceRecords(cutoffTimestamp)
        val deletedSessions = dao.deleteOldInactiveSessions(cutoffTimestamp)
        deletedRecords + deletedSessions
    }
}
