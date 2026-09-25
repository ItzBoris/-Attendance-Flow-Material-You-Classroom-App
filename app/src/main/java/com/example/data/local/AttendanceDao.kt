package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    // Student Profile
    @Query("SELECT * FROM student_profiles LIMIT 1")
    fun getActiveStudentProfileFlow(): Flow<StudentProfile?>

    @Query("SELECT * FROM student_profiles LIMIT 1")
    suspend fun getActiveStudentProfile(): StudentProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveStudentProfile(profile: StudentProfile)

    @Query("DELETE FROM student_profiles")
    suspend fun clearStudentProfile()

    // Sessions
    @Query("SELECT * FROM attendance_sessions WHERE isActive = 1 ORDER BY startTime DESC LIMIT 1")
    fun getActiveSessionFlow(): Flow<AttendanceSession?>

    @Query("SELECT * FROM attendance_sessions WHERE isActive = 1 ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveSession(): AttendanceSession?

    @Query("SELECT * FROM attendance_sessions WHERE sessionId = :sessionId")
    suspend fun getSessionById(sessionId: String): AttendanceSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: AttendanceSession)

    @Query("UPDATE attendance_sessions SET isActive = 0, endTime = :endTime WHERE sessionId = :sessionId")
    suspend fun endSession(sessionId: String, endTime: Long = System.currentTimeMillis())

    // Attendance Records
    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId ORDER BY scannedTimestamp DESC")
    fun getRecordsForSessionFlow(sessionId: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId ORDER BY scannedTimestamp DESC")
    suspend fun getRecordsForSession(sessionId: String): List<AttendanceRecord>

    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId AND studentRollNumber = :rollNumber LIMIT 1")
    suspend fun getRecordForStudentInSession(sessionId: String, rollNumber: String): AttendanceRecord?

    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId AND deviceId = :deviceId LIMIT 1")
    suspend fun getRecordForDeviceInSession(sessionId: String, deviceId: String): AttendanceRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceRecord(record: AttendanceRecord)

    @Query("SELECT COUNT(*) FROM attendance_records WHERE sessionId = :sessionId AND status = 'PRESENT'")
    fun getPresentCountFlow(sessionId: String): Flow<Int>

    @Query("SELECT * FROM attendance_records ORDER BY scannedTimestamp DESC")
    suspend fun getAllAttendanceRecords(): List<AttendanceRecord>

    // Cleanup Queries
    @Query("DELETE FROM attendance_records WHERE scannedTimestamp < :cutoffTimestamp")
    suspend fun deleteOldAttendanceRecords(cutoffTimestamp: Long): Int

    @Query("DELETE FROM attendance_sessions WHERE isActive = 0 AND endTime > 0 AND endTime < :cutoffTimestamp")
    suspend fun deleteOldInactiveSessions(cutoffTimestamp: Long): Int
}
