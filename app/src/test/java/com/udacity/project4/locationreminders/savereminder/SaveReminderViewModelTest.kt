package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R

import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    private lateinit var viewModel: SaveReminderViewModel

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun initialSetting() {
        stopKoin()
        val dataSource = FakeDataSource(arrayListOf())
        viewModel = SaveReminderViewModel(ApplicationProvider.getApplicationContext(), dataSource)
    }

    @Test
    fun givenFullInfo_whenValidateEnteredData_thenReturnTrue() {
        val reminder = ReminderDataItem("title", "description", "location", 1.toDouble(), 1.toDouble())
        val result = viewModel.validateEnteredData(reminder)
        val showSnackBarInt: Int? = try{
            viewModel.showSnackBarInt.getOrAwaitValue()
        } catch (e: Exception) {
            null
        }
        assertThat(showSnackBarInt==null, `is`(true))
        assertThat(result, `is`(true))
    }

    @Test
    fun giventEmptyLatLongAndDescription_whenValidateEnteredData_returnTrue() {
        val reminder1 = ReminderDataItem("title", null, "location", null, null)
        val result1 = viewModel.validateEnteredData(reminder1)
        val reminder2 = ReminderDataItem("title", "", "location", null, null)
        val result2 = viewModel.validateEnteredData(reminder2)
        val showSnackBarInt: Int? = try{
            viewModel.showSnackBarInt.getOrAwaitValue()
        } catch (e: Exception) {
            null
        }
        assertThat(showSnackBarInt==null, `is`(true))
        assertThat(result1, `is`(true))
        assertThat(result2, `is`(true))
    }


    @Test
    fun givenNoLocation_whenValidateEnteredData_thenReturnFalseAndShowSnackBar() {
        val reminder1 = ReminderDataItem("title", "description", null, 1.toDouble(), 1.toDouble())
        val result1 = viewModel.validateEnteredData(reminder1)
        val reminder2 = ReminderDataItem("title", "description", "", 1.toDouble(), 1.toDouble())
        val result2 = viewModel.validateEnteredData(reminder2)
        val showSnackBarInt = viewModel.showSnackBarInt.getOrAwaitValue()
        assertThat(showSnackBarInt, `is`(R.string.err_select_location))
        assertThat(result1, `is`(false))
        assertThat(result2, `is`(false))
    }

    @Test
    fun givenNoTitle_whenValidateEnteredData_thenReturnFalse() {
        val reminder1 = ReminderDataItem(null, "description", "location", 1.toDouble(), 1.toDouble())
        val result1 = viewModel.validateEnteredData(reminder1)
        val reminder2 = ReminderDataItem("", "description", "location", 1.toDouble(), 1.toDouble())
        val result2 = viewModel.validateEnteredData(reminder2)
        val showSnackBarInt = viewModel.showSnackBarInt.getOrAwaitValue()
        assertThat(showSnackBarInt, `is`(R.string.err_enter_title))
        assertThat(result1, `is`(false))
        assertThat(result2, `is`(false))
    }

    @Test
    fun givenValidInput_whenSaveReminder_showToastAndNavigate() {
        val reminder = ReminderDataItem("title", "description", "location", 1.toDouble(), 1.toDouble())
        viewModel.saveReminder(reminder)
        val showToast = viewModel.showToast.getOrAwaitValue()
        val navigationCommand = viewModel.navigationCommand.getOrAwaitValue()
        assertThat(showToast!=null, `is`(true))
        assertThat(navigationCommand==NavigationCommand.Back, `is`(true))
    }

    @Test
    fun whenSaveReminder_thenCheckLoading() {
        val reminder = ReminderDataItem("title", "description", "location", 1.toDouble(), 1.toDouble())
        mainCoroutineRule.pauseDispatcher()
        viewModel.saveReminder(reminder)
        var showLoading = viewModel.showLoading.getOrAwaitValue()
        assertThat(showLoading, `is`(true))
        mainCoroutineRule.resumeDispatcher()
        showLoading = viewModel.showLoading.getOrAwaitValue()
        assertThat(showLoading, `is`(false))
    }

    @Test
    fun givenFilledData_whenOnClear_dataSetToNull() {
        viewModel.longitude.value = 100.toDouble()
        viewModel.latitude.value = 100.toDouble()
        viewModel.reminderTitle.value = "title"
        viewModel.reminderSelectedLocationStr.value = "location"
        viewModel.reminderDescription.value = "description"
        viewModel.onClear()
        val title = viewModel.reminderTitle.getOrAwaitValue()
        val description = viewModel.reminderDescription.getOrAwaitValue()
        val location = viewModel.reminderSelectedLocationStr.getOrAwaitValue()
        val lat = viewModel.latitude.getOrAwaitValue()
        val long = viewModel.longitude.getOrAwaitValue()
        assertThat(title==null, `is`(true))
        assertThat(description==null, `is`(true))
        assertThat(location==null, `is`(true))
        assertThat(lat==null, `is`(true))
        assertThat(long==null, `is`(true))
    }
}