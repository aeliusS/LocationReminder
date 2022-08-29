package com.udacity.project4.locationreminders.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result.Success
import com.udacity.project4.locationreminders.data.dto.Result.Error
import com.udacity.project4.locationreminders.data.dto.asReminderDataItem
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.sendNotification
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeofenceNotificationWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val dataSource: ReminderDataSource,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.IO
) :
    CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result = withContext(defaultDispatcher) {
        try {
            Log.d(TAG, "worker called for notification")
            val requestId = inputData.getString(KEY_REMINDER_ID)

            if (requestId != null) {
                when (val result = dataSource.getReminder(requestId)) {
                    is Success<ReminderDTO> -> {
                        val reminder = result.data
                        Log.d(TAG, "Found the reminder for id: ${reminder.id}")
                        sendNotification(applicationContext, reminder.asReminderDataItem())
                    }
                    is Error -> Log.e(TAG, "Did not find reminder for $requestId")
                }
            } else {
                Log.e(TAG, "Request Id is null")
            }
        } catch (ex: Exception) {
            Log.d(TAG, "Error sending geofence notification: ${ex.message}")
            return@withContext Result.failure()
        }

        Result.success()
    }

    companion object {
        private const val TAG = "NotificationWorker"
        const val KEY_REMINDER_ID = "REMINDER_ID"
    }
}