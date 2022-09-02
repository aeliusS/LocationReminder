package com.udacity.project4.locationreminders.savereminder

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.runner.RunWith
import org.koin.test.KoinTest

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest: KoinTest {

    // subject under test
    private lateinit var saveReminderViewModel: SaveReminderViewModel

    // use a fake repository to be injected into the ViewModel
    private lateinit var dataSource: FakeDataSource



}