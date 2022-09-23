package com.udacity.project4.locationreminders

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.LocationServices
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityReminderDescriptionBinding
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

/**
 * Activity that displays the reminder details after the user clicks on the notification
 */
class ReminderDescriptionActivity : AppCompatActivity(), KoinComponent {

    companion object {
        private const val EXTRA_ReminderDataItem = "EXTRA_ReminderDataItem"

        // receive the reminder object after the user clicks on the notification
        fun newIntent(context: Context, reminderDataItem: ReminderDataItem): Intent {
            val intent = Intent(context, ReminderDescriptionActivity::class.java)
            intent.putExtra(EXTRA_ReminderDataItem, reminderDataItem)
            return intent
        }
    }

    private lateinit var binding: ActivityReminderDescriptionBinding
    private val dataSource: ReminderDataSource by inject()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_reminder_description
        )
        val reminder = intent.extras?.get(EXTRA_ReminderDataItem) as ReminderDataItem
        binding.reminderDataItem = reminder
        binding.buttonOk.setOnClickListener {
            this.finish()
        }
        // the reminder is no longer needed in the db
        removeReminder(reminder.id)
    }

    private fun removeReminder(id: String) = runBlocking {
        // the reminder is no longer needed in the db
        removeReminderFromDb(id)
        // remove the geofence
        val geofencingClient = LocationServices.getGeofencingClient(this@ReminderDescriptionActivity)
        geofencingClient.removeGeofences(mutableListOf(id))
    }

    private suspend fun removeReminderFromDb(id: String) {
        dataSource.deleteReminder(id)
    }
}
