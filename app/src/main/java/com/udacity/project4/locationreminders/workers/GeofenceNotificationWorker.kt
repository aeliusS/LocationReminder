package com.udacity.project4.locationreminders.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result.Success
import com.udacity.project4.locationreminders.data.dto.Result.Error
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeofenceNotificationWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val dataSource: ReminderDataSource,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
) :
    CoroutineWorker(appContext, workerParameters) {

    // TODO: remove the two private variables and see if app still errors out

    override suspend fun doWork(): Result = withContext(coroutineDispatcher) {
        try {
            Log.d(TAG, "worker called for notification")
            val requestId = inputData.getString(KEY_REMINDER_ID)

            if (requestId != null) {
                when (val result = dataSource.getReminder(requestId)) {
                    is Success<*> -> {
                        val reminder = result.data as ReminderDTO
                        Log.d(TAG, "Found the reminder for id: ${reminder.id}")
                    }
                    is Error -> Log.e(TAG, "Did not find reminder for $requestId")
                }
            } else {
                Log.e(TAG, "Request Id is null")
            }
        } catch (ex: Exception) {
            Log.d(TAG, "Error sending geofence notification")
            return@withContext Result.failure()
        }

        Result.success()
    }

    companion object {
        private const val TAG = "NotificationWorker"
        const val KEY_REMINDER_ID = "REMINDER_ID"
    }
}