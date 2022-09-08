package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.R

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest : KoinTest {

    // subject under test
    private lateinit var saveReminderViewModel: SaveReminderViewModel

    // use a fake repository to be injected into the ViewModel
    private lateinit var dataSource: FakeDataSource

    // this makes the test run sequentially, which is important for testing LiveData variables
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setupRepositoryAndViewModel() {
        // initialize the datasource
        dataSource = FakeDataSource()
        val module = module {
            single { SaveReminderViewModel(get(), dataSource) }
        }
        loadKoinModules(module)
        saveReminderViewModel = get()
    }

    @After
    fun closeOutKoin() {
        stopKoin()
    }

    @Test
    fun onClear_nullsAllValues() = runTest {
        // when calling onClear
        saveReminderViewModel.onClear()
        advanceUntilIdle()

        // Then the values in the view model are nulled out
        assertThat(saveReminderViewModel.reminderTitle.getOrAwaitValue(), (nullValue()))
        assertThat(saveReminderViewModel.reminderDescription.getOrAwaitValue(), (nullValue()))
        assertThat(
            saveReminderViewModel.reminderSelectedLocationStr.getOrAwaitValue(),
            (nullValue())
        )
        assertThat(saveReminderViewModel.selectedPOI.getOrAwaitValue(), (nullValue()))
        assertThat(saveReminderViewModel.latitude.getOrAwaitValue(), (nullValue()))
        assertThat(saveReminderViewModel.longitude.getOrAwaitValue(), (nullValue()))
        assertThat(saveReminderViewModel.locationPermissionGranted.getOrAwaitValue(), (nullValue()))
        assertThat(saveReminderViewModel.selectedMarker.getOrAwaitValue(), (nullValue()))
    }

    @Test
    fun saveInvalidReminder_showSnackbarError() = runTest {
        // given an invalid reminder
        val reminder = ReminderDataItem("", "", "", 0.0, 0.0)

        // when saving the reminder
        saveReminderViewModel.validateAndSaveReminder(reminder)
        advanceUntilIdle()

        // then the snackbar values are set to error
        assertEquals(
            saveReminderViewModel.showSnackBarInt.getOrAwaitValue(),
            R.string.err_enter_title
        )
    }

    @Test
    fun saveValidReminder_showLoading() = runTest {
        // given a valid reminder
        val reminder = ReminderDataItem("Test", "", "Google", 0.0, 0.0)

        // when saving the reminder
        saveReminderViewModel.validateAndSaveReminder(reminder)

        // then showLoading is set to true
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(true))
        // after it's done
        advanceUntilIdle()

        // then showLoading is set to false
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    // TODO: check saved reminder test

}