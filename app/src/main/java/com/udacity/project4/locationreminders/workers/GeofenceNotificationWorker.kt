package com.udacity.project4.locationreminders.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class GeofenceNotificationWorker(
    appContext: Context,
    workerParameters: WorkerParameters): CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        // TODO("Not yet implemented")
        Log.d(TAG, "worker called")

        return Result.failure()
    }

    companion object {
        private const val TAG = "NotificationWorker"
        const val KEY_REMINDER_ID = "GEOFENCE_INTENT"
    }
}