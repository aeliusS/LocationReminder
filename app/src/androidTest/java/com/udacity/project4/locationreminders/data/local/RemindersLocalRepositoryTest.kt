package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var localDataSource: ReminderDataSource
    private lateinit var database: RemindersDatabase

    @Before
    fun setup() {
        // Using an in-memory database for testing, because it doesn't survive killing the process.
        // ONLY USE allowMainThreadQueries IN TESTING
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        localDataSource = RemindersLocalRepository(database.reminderDao(), Dispatchers.Main)
    }

    @After
    fun cleanUp() {
        database.close()
    }

    @Test
    fun saveReminder_retrieveReminder() = runTest {
        // given - a new reminder saved in the database
        val newReminder = ReminderDTO("title", "description", "location", 0.0, 0.0)
        localDataSource.saveReminder(newReminder)

        // when - reminder retrieved by id
        val result = localDataSource.getReminder(newReminder.id)

        // then - same reminder is returned
        result as Result.Success
        assertThat(result.data.title, `is`("title"))
        assertThat(result.data.description, `is`("description"))
        assertThat(result.data.location, `is`("location"))
        assertThat(result.data.latitude, `is`(0.0))
        assertThat(result.data.longitude, `is`(0.0))
    }

    @Test
    fun saveReminder_deleteReminder() = runTest {
        // given - a new reminder saved in the database
        val newReminder = ReminderDTO("title", "description", "location", 0.0, 0.0)
        localDataSource.saveReminder(newReminder)

        // when - the reminder is deleted
        localDataSource.deleteReminder(newReminder.id)

        // then - the reminder is removed from the db
        val result = localDataSource.getReminder(newReminder.id)
        result as Result.Error
        assertThat(result, `is`(Result.Error("Reminder not found!")))
    }

    @Test
    fun getReminder_testInvalidId() = runTest {
        // given - no reminder in db

        // when - query for a reminder with invalid id
        val result = localDataSource.getReminder("-1")

        // then - reminder not found is returned
        result as Result.Error
        assertThat(result, `is`(Result.Error("Reminder not found!")))
    }

}