package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.AttendanceRecord
import com.example.data.local.AttendanceSession
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    fun exportAndShareCsv(
        context: Context,
        session: AttendanceSession,
        records: List<AttendanceRecord>
    ): Intent? {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val dateFileStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

            val csvHeader = "Timestamp,Roll Number,Student Name,Device ID,Session ID\n"
            val sb = StringBuilder()
            sb.append(csvHeader)

            records.forEach { rec ->
                val timeFormatted = dateFormat.format(Date(rec.scannedTimestamp))
                val cleanRoll = rec.studentRollNumber.replace("\"", "\"\"")
                val cleanName = rec.studentName.replace("\"", "\"\"")
                val cleanDevice = rec.deviceId.replace("\"", "\"\"")
                val cleanSessionId = rec.sessionId.ifBlank { session.sessionId }.replace("\"", "\"\"")

                sb.append("\"$timeFormatted\",\"$cleanRoll\",\"$cleanName\",\"$cleanDevice\",\"$cleanSessionId\"\n")
            }

            val fileName = "attendance_export.csv"
            val cacheDir = File(context.cacheDir, "exports")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            val csvFile = File(cacheDir, fileName)
            csvFile.writeText(sb.toString())

            val authority = "${context.packageName}.fileprovider"
            val fileUri: Uri = FileProvider.getUriForFile(context, authority, csvFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Attendance CSV Report: ${session.subjectCode}")
                putExtra(Intent.EXTRA_TEXT, "Attached is the attendance report for ${session.courseName} (${session.subjectCode}).")
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            return Intent.createChooser(shareIntent, "Share Attendance CSV")
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
