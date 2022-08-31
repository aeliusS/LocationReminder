package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest : KoinTest {

    //TODO: provide testing to the RemindersListViewModel and its live data objects

    // subject under test
    private lateinit var remindersListViewModel: RemindersListViewModel

    // use a fake repository to be injected into the ViewModel
    private lateinit var dataSource: FakeDataSource

    @Before
    fun setupViewModel() {
        // initialize the datasource with 3 reminders
        dataSource = FakeDataSource()
        val reminder1 = ReminderDTO("Title1", "Description1", "Sydney", 1.0, 2.0)
        val reminder2 = ReminderDTO("Title2", "Description2", "Sydney", 1.0, 2.0)
        val reminder3 = ReminderDTO("Title3", "Description3", "Sydney", 1.0, 2.0)
        dataSource.addReminders(reminder1, reminder2, reminder3)

        val module = module {
            single { RemindersListViewModel(get(), dataSource) }
        }
        loadKoinModules(module)
        remindersListViewModel = get()
    }

    // this makes the test run sequentially, which is important for testing LiveData variables
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // TODO: rename function
    @Test
    fun testingComponent() {
//        startKoin {
//            modules(
//                module {
//                    single { RemindersListViewModel(get(), dataSource) }
//                }
//            )
//        }
        assertNotNull(remindersListViewModel)
        // TODO: call load function before check
        val value = remindersListViewModel.remindersList.getOrAwaitValue()
        assertThat(value, (not(nullValue())))
    }

}