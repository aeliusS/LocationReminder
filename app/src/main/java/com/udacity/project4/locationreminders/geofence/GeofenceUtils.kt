package com.udacity.project4.locationreminders.geofence

import java.util.concurrent.TimeUnit

internal object GeofencingConstants {
    const val GEOFENCE_RADIUS_IN_METERS = 100f
    const val ACTION_GEOFENCE_EVENT = "SaveReminderFragment.action.ACTION_GEOFENCE_EVENT"
    val GEOFENCE_EXPIRATION_IN_MILLISECONDS: Long = TimeUnit.DAYS.toMillis(1)
}