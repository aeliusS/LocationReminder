package com.udacity.project4.locationreminders.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.locationreminders.geofence.GeofencingConstants.ACTION_GEOFENCE_EVENT
import com.udacity.project4.locationreminders.workers.GeofenceNotificationWorker
import com.udacity.project4.locationreminders.workers.GeofenceNotificationWorker.Companion.KEY_REMINDER_ID

/**
 * Triggered by the Geofence.  Since we can have many Geofences at once, we pull the request
 * ID from the first Geofence, and locate it within the cached data in our Room DB
 *
 * Or users can add the reminders and then close the app, So our app has to run in the background
 * and handle the geofencing in the background.
 * To do that you can use https://developer.android.com/reference/android/support/v4/app/JobIntentService to do that.
 *
 */

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeoBroadcastReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_GEOFENCE_EVENT) {
            val geofencingEvent = GeofencingEvent.fromIntent(intent)
            if (geofencingEvent == null) {
                Log.d(TAG, "Received null geofence event")
                return
            }

            if (geofencingEvent.hasError()) {
                val errorMessage = errorMessage(context, geofencingEvent.errorCode)
                Log.e(TAG, errorMessage)
                return
            }

            // get the transition type
            val geofenceTransition = geofencingEvent.geofenceTransition
            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                Log.v(TAG, "Geofence entered")
                if (geofencingEvent.triggeringGeofences == null) {
                    Log.e(TAG, "No Geofence Trigger Found! Abort mission!")
                    return
                }
                for (geofence in geofencingEvent.triggeringGeofences!!) {
                    val reminderId = geofence.requestId
                    sendWorkerNotification(context, reminderId)
                }
            } else {
                Log.d(TAG, "Geofence transition event: $geofenceTransition")
            }
        }
    }

    private fun sendWorkerNotification(context: Context, reminderId: String) {
        val oneTimeNotification = OneTimeWorkRequestBuilder<GeofenceNotificationWorker>()
            .setInputData(workDataOf(KEY_REMINDER_ID to reminderId))
            .build()
        WorkManager.getInstance(context).enqueue(oneTimeNotification)
    }
}