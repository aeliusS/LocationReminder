package com.udacity.project4.locationreminders.data

import androidx.lifecycle.MutableLiveData
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.dto.Result.*

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource : ReminderDataSource {

    var remindersServiceData: LinkedHashMap<String, ReminderDTO> = LinkedHashMap()

    // private val reminders = MutableLiveData<List<ReminderDTO>>()

    private var shouldReturnError = false

    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError) {
            return Error("Test exception")
        }
        return Success(remindersServiceData.values.toList())
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        remindersServiceData[reminder.id] = reminder
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError) {
            return Error("Test exception")
        }
        remindersServiceData[id]?.let {
            return Success(it)
        }
        return Error("Could not find task")
    }

    override suspend fun deleteAllReminders() {
        remindersServiceData.clear()
    }

    override suspend fun deleteReminder(id: String) {
        remindersServiceData.remove(id)
    }

    fun addReminders(vararg reminders: ReminderDTO) {
        for (reminder in reminders) {
            remindersServiceData[reminder.id] = reminder
        }
    }
}