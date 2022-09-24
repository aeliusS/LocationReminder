package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.FakeRemindersLocalRepository
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.mockito.Mockito.*

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest: AutoCloseKoinTest() {
    private lateinit var repository: FakeRemindersLocalRepository
    private lateinit var appContext: Application

    @Before
    fun initRepository() {
        stopKoin()//stop the original app koin
        repository = FakeRemindersLocalRepository()
        appContext = getApplicationContext()

        val myModule = module {
            viewModel {
                RemindersListViewModel(appContext,repository)
            }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
    }

    @After
    fun cleanupDb() = runTest {
        repository.deleteAllReminders()
    }

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }

    @Test
    fun clickAddReminderButton_navigateToAddReminderFragment() {
        // given - reminder list screen
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        // when - click on the "+" button
        onView(withId(R.id.addReminderFAB)).perform(click())

        // then - verify that we navigate to the add screen
        verify(navController).navigate(
            ReminderListFragmentDirections.toSaveReminder()
        )
    }

    @Test
    fun saveNewReminder_verifyNewReminderOnUI() = runTest {
        // given - add new reminder to the db
        repository.saveReminder(ReminderDTO("title1", "description1", "location1", 0.0, 0.0))

        // when - reminder list fragment launched
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        // then - reminder is displayed on the screen
        onView(withId(R.id.title)).check(matches(isDisplayed()))
        onView(withId(R.id.title)).check(matches(withText("title1")))
        onView(withId(R.id.description)).check(matches(isDisplayed()))
        onView(withId(R.id.description)).check(matches(withText("description1")))
        onView(withId(R.id.locationString)).check(matches(isDisplayed()))
        onView(withId(R.id.locationString)).check(matches(withText("location1")))
    }

    @Test
    fun noReminders_displaysNoData() = runTest {
        // given a repository with no reminders

        // when - reminder list fragment is launched
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        // then - no data is displayed
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }

    @Test
    fun loadReminders_showError() = runBlocking {
        // given - a repository with reminder(s) in it and setReturnError set to true
        repository.saveReminder(ReminderDTO("title1", "description1", "location1", 0.0, 0.0))
        repository.setReturnError(true)

        // when - reminder list fragment is launched
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        // then - a snackbar will display error message
        onView(withText("Test exception"))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        repository.setReturnError(false)
    }

}