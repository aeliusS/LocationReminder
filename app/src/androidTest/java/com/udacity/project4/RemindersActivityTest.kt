package com.udacity.project4

import android.app.Activity
import android.app.Application
import android.app.Instrumentation
import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onIdle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.udacity.project4.authentication.AuthenticationActivity
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    private lateinit var auth: FirebaseAuth
    private val dataBindingIdlingResource = DataBindingIdlingResource()
    private val idlingResource = EspressoIdlingResource.countingIdlingResource

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        // make sure you are logged in first
        auth = Firebase.auth
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single<ReminderDataSource> { RemindersLocalRepository(get()) }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
            if (auth.currentUser == null) {
                login()
                idlingResource.increment()
            }
        }
        // auth.signOut()
    }

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(idlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(idlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    @get:Rule
    var rule = ActivityScenarioRule(RemindersActivity::class.java)

    @get:Rule
    val intentsTestRule = IntentsTestRule(RemindersActivity::class.java)


    // TODO: add End to End testing to the app

    /* FLAKY TEST */
    @Test
    fun test_logout() = runBlocking {
//        Intents.init()
        // start up the reminder screen
        val activityScenario = rule.scenario
        dataBindingIdlingResource.monitorActivity(activityScenario)
        activityScenario.moveToState(Lifecycle.State.STARTED)

        intending(hasComponent(AuthenticationActivity::class.java.name)).respondWithFunction {
            login()
            idlingResource.increment()
            Instrumentation.ActivityResult(Activity.RESULT_OK, Intent())
        }
//        val result = Instrumentation.ActivityResult(Activity.RESULT_OK, Intent())
//        intending(hasComponent(AuthenticationActivity::class.java.name)).respondWith(result)

        // if logged in, test log out
        if (FirebaseAuth.getInstance().currentUser != null) {
            // onView(withText("LOGOUT")).check(matches(isDisplayed()))
            onView(withText("LOGOUT")).perform(click())

            // verify that the logout screen is displayed
            onView(withText("LOGIN")).check(matches(isDisplayed()))
        }

//        Intents.release()
        // close out the activity
        activityScenario.close()

    }

    private fun login() {
        FirebaseAuth.getInstance()
            .signInWithEmailAndPassword("testing@example.com", "asdf1234")
            .addOnCompleteListener() { task ->
                assertThat(task.isSuccessful, `is`(true))
                idlingResource.decrement()
            }
    }


    // TODO: add new reminder
    @Test
    fun addNewReminder() = runBlocking {
        // 1. make sure you are logged in first
        if (auth.currentUser == null) {
            login()
            idlingResource.increment()
        }
        // 2. start up the reminder screen
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // 3. add in a new reminder
        onView(withId(R.id.addReminderFAB)).perform(click())

        activityScenario.close()
    }

}
