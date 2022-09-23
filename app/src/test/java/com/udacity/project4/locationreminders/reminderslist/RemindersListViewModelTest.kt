package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
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
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import kotlin.test.assertNotNull
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest : KoinTest {

    // subject under test
    private lateinit var remindersListViewModel: RemindersListViewModel

    // use a fake repository to be injected into the ViewModel
    private lateinit var dataSource: FakeDataSource

    // this makes the test run sequentially, which is important for testing LiveData variables
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setupRepositoryAndViewModel() {
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

    @After
    fun closeOutKoin() {
        // unloadKoinModules(module)
        stopKoin()
    }

//    @get:Rule
//    val koinTestRule = KoinTestRule.create {
//        modules(module {
//            single { RemindersListViewModel(get(), dataSource) }
//        })
//    }


    @Test
    fun loadReminders_showLoading() = runTest {
        // verify that Koin loaded the view model
        assertNotNull(remindersListViewModel)

        // when the reminders are loaded into the view model
        remindersListViewModel.loadReminders()

        // then showLoading will be set to true
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(true))
        // now let the coroutine finish before continuing
        advanceUntilIdle()

        // then the showLoading is set to false
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun loadReminders_reminderListLoaded() = runTest {
        // given the remindersListViewModel
        assertNotNull(remindersListViewModel)

        // when the reminders are loaded into the view model
        remindersListViewModel.loadReminders()
        // now let the coroutine finish before continuing
        advanceUntilIdle()

        // then the reminderList will not be empty
        val remindersList = remindersListViewModel.remindersList.getOrAwaitValue()
        val showNoData = remindersListViewModel.showNoData.getOrAwaitValue()
        assertThat(remindersList.isEmpty(), `is`(false))
        assertThat(showNoData, `is`(false))
    }

    @Test
    fun loadReminders_showError() = runTest {
        // given the remindersListViewModel and returnError set to true
        assertNotNull(remindersListViewModel)
        dataSource.setReturnError(true)

        // when the reminders are loaded into the view model
        remindersListViewModel.loadReminders()
        advanceUntilIdle()

        // it should set the error values
        val showSnackBar = remindersListViewModel.showSnackBar.getOrAwaitValue()
        val showNoData = remindersListViewModel.showNoData.getOrAwaitValue()
        assertThat(showSnackBar, `is`("Test exception"))
        assertThat(showNoData, `is`(true))
    }

}