package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    private lateinit var viewModel: RemindersListViewModel

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun initialSetting() {
        stopKoin()
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun givenNotNullData_whenRunLoadReminders_thenLoadRemindersList() {
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext())
        val data = arrayListOf<ReminderDTO>()
        for(i in 1..10) {
            data.add(ReminderDTO("title$i", "description$i","location$i",i.toDouble(),i.toDouble()))
        }
        val dataSource = FakeDataSource(data)
        viewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), dataSource)
        viewModel.loadReminders()
        val reminderList = viewModel.remindersList.getOrAwaitValue()
        val showNoData = viewModel.showNoData.getOrAwaitValue()
        assertThat(reminderList.size, `is`(10))
        assertThat(showNoData, `is`(false))
    }

    @Test
    fun givenNullData_whenRunLoadReminders_shouldShowNoData() {
        val dataSource = FakeDataSource(arrayListOf<ReminderDTO>())
        viewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), dataSource)
        viewModel.loadReminders()
        val reminderList = viewModel.remindersList.getOrAwaitValue()
        val showNoData = viewModel.showNoData.getOrAwaitValue()
        assertThat(reminderList.size, `is`(0))
        assertThat(showNoData, `is`(true))
    }

    @Test
    fun givenErrorData_whenRunLoadReminders_shouldReturnError() {
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext())
        val dataSource = FakeDataSource(arrayListOf<ReminderDTO>(),true)
        viewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), dataSource)
        viewModel.loadReminders()
        val errorMessage = viewModel.showSnackBar.getOrAwaitValue()
        val showNoData = viewModel.showNoData.getOrAwaitValue()
        assertThat(errorMessage, `is`("Error"))
        assertThat(showNoData, `is`(true))
    }

    @Test
    fun whenRunLoadReminders_thenCheckLoading() {
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext())
        val dataSource = FakeDataSource(arrayListOf<ReminderDTO>(),true)
        viewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), dataSource)
        mainCoroutineRule.pauseDispatcher()
        viewModel.loadReminders()
        var showLoading = viewModel.showLoading.getOrAwaitValue()
        assertThat(showLoading, `is`(true))
        mainCoroutineRule.resumeDispatcher()
        showLoading = viewModel.showLoading.getOrAwaitValue()
        assertThat(showLoading, `is`(false))
    }
}