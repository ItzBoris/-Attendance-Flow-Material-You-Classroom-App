package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "student_profiles")
data class StudentProfile(
    @PrimaryKey
    val rollNumber: String,
    val fullName: String,
    val phoneNumber: String,
    val deviceId: String,
    val registeredAtTimestamp: Long = System.currentTimeMillis()
)
