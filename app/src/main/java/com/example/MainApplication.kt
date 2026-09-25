package com.example

import android.app.Application
import com.example.worker.AttendanceCacheCleanupWorker

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Schedule periodic WorkManager task to clear old attendance cache
        AttendanceCacheCleanupWorker.schedulePeriodicCleanup(this)
    }
}
