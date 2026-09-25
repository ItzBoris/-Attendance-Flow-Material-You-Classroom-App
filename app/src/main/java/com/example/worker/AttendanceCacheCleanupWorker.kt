package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.repository.AttendanceRepository
import java.util.concurrent.TimeUnit

class AttendanceCacheCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getInstance(applicationContext)
            val repository = AttendanceRepository(db.attendanceDao(), applicationContext)
            // Clear attendance records and inactive sessions older than 24 hours
            val deletedCount = repository.clearOldAttendanceCache(maxAgeMillis = 24 * 60 * 60 * 1000L)
            Log.d(TAG, "Successfully cleared $deletedCount old attendance cache entries")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error performing attendance cache cleanup", e)
            Result.retry()
        }
    }

    companion object {
        const val TAG = "AttendanceCacheCleanupWorker"
        const val WORK_NAME = "attendance_cache_cleanup_work"

        fun schedulePeriodicCleanup(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val cleanupWorkRequest = PeriodicWorkRequestBuilder<AttendanceCacheCleanupWorker>(
                12, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                cleanupWorkRequest
            )
        }
    }
}
