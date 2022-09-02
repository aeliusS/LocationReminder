package com.udacity.project4

import android.app.Application
import android.os.StrictMode
import android.util.Log
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapsSdkInitializedCallback
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.locationreminders.workers.GeofenceNotificationWorker
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.androidx.workmanager.dsl.worker
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.dsl.single

class MyApp : Application(), OnMapsSdkInitializedCallback, KoinComponent {

    override fun onCreate() {
        // strict mode
        /*
        val policy = StrictMode.VmPolicy.Builder()
            .detectAll()
            .penaltyLog()
            .build()
        StrictMode.setVmPolicy(policy)
         */

        super.onCreate()
        MapsInitializer.initialize(this@MyApp, MapsInitializer.Renderer.LATEST, this)

        /**
         * use Koin Library as a service locator
         */
        val myModule = module {
            //Declare a ViewModel - be later inject into Fragment with dedicated injector using by viewModel()
            viewModelOf(::RemindersListViewModel)

            //Declare singleton definitions to be later injected using by inject()
            singleOf(::SaveReminderViewModel)
            single<ReminderDataSource> { RemindersLocalRepository(get()) }
            single { LocalDB.createRemindersDao(this@MyApp) }

            // worker definition
            single(named("IODispatcher")) {
                Dispatchers.IO
            }
            // TODO: comment out the line below for testing
            // worker { GeofenceNotificationWorker(get(), get(), get(), get(named("IODispatcher"))) }
            // workerOf(::GeofenceNotificationWorker)
        }

        startKoin {
            androidContext(this@MyApp)
            androidLogger()
            // workManagerFactory() // TODO: comment this out for testing
            modules(listOf(myModule))
        }
    }

    override fun onMapsSdkInitialized(renderer: MapsInitializer.Renderer) {
        when (renderer) {
            MapsInitializer.Renderer.LATEST -> Log.d(
                "MyApp",
                "The latest version of the renderer is used."
            )
            MapsInitializer.Renderer.LEGACY -> Log.d(
                "MyApp",
                "The legacy version of the renderer is used."
            )
        }
    }
}