package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import kotlin.test.assertNotNull
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.stopKoin
import org.koin.core.context.unloadKoinModules
import org.koin.core.module.Module

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest : KoinTest {

    //TODO: provide testing to the RemindersListViewModel and its live data objects

    // subject under test
    private lateinit var remindersListViewModel: RemindersListViewModel

    // use a fake repository to be injected into the ViewModel
    private lateinit var dataSource: FakeDataSource

    // koin module
    private lateinit var module: Module

    @Before
    fun setupViewModel() {
        // initialize the datasource with 3 reminders
        dataSource = FakeDataSource()
        val reminder1 = ReminderDTO("Title1", "Description1", "Sydney", 1.0, 2.0)
        val reminder2 = ReminderDTO("Title2", "Description2", "Sydney", 1.0, 2.0)
        val reminder3 = ReminderDTO("Title3", "Description3", "Sydney", 1.0, 2.0)
        dataSource.addReminders(reminder1, reminder2, reminder3)

        module = module {
            single { RemindersListViewModel(get(), dataSource) }
        }

        stopKoin()
        startKoin {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(module)
        }
        // loadKoinModules(module)
    }

    @After
    fun closeOutKoin() {
        // unloadKoinModules(module)
        stopKoin()
    }

    // this makes the test run sequentially, which is important for testing LiveData variables
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Test
    fun loadReminders_showLoading() = runTest {
        // verify that Koin loaded the view model
        remindersListViewModel = get()
        assertNotNull(remindersListViewModel)

        // load the reminders in the view model
        remindersListViewModel.loadReminders()

        // now let the coroutine finish before continuing
        advanceUntilIdle()

        // then the showLoading is set to false
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(false) )
    }

    @Test
    fun loadReminders_reminderListLoaded() = runTest {
        // load the reminders in the view model
        remindersListViewModel = get()
        remindersListViewModel.loadReminders()

        // now let the coroutine finish before continuing
        advanceUntilIdle()

        // the reminderList will not be null
        val value = remindersListViewModel.remindersList.getOrAwaitValue()
        assertThat(value, (not(nullValue())))
    }

}