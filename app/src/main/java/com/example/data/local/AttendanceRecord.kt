package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val studentRollNumber: String,
    val studentName: String,
    val studentPhone: String = "",
    val deviceId: String,
    val scannedTimestamp: Long = System.currentTimeMillis(),
    val status: String = "PRESENT", // PRESENT, EXPIRED_ATTEMPT, DUP_REJECTED
    val tokenSignature: String = ""
)
