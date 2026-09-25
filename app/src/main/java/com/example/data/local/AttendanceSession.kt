package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_sessions")
data class AttendanceSession(
    @PrimaryKey
    val sessionId: String,
    val subjectCode: String,
    val courseName: String,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val isActive: Boolean = true,
    val teacherUsername: String = "admin"
)
