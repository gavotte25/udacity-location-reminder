package com.udacity.project4.locationreminders.data.local

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.MainCoroutineRule

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    private lateinit var repo: RemindersLocalRepository
    private lateinit var dao: RemindersDao
    private lateinit var db: RemindersDatabase

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private fun setupRepo(dummyData: ArrayList<ReminderDTO> = arrayListOf()) = runBlocking {
        for(reminderDTO in dummyData) {
            dao.saveReminder(reminderDTO)
        }
    }

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, RemindersDatabase::class.java).build()
        dao = db.reminderDao()
        repo = RemindersLocalRepository(dao, Dispatchers.Main)
    }

    @After
    fun closeDb() = runBlocking {
        dao.deleteAllReminders()
        db.close()
        stopKoin()
    }

    @Test
    fun givenEmptyDao_whenGetReminders_thenReturnEmptyResult() = mainCoroutineRule.runBlockingTest {
        val result = repo.getReminders()
        val data = (result as Result.Success).data
        assertThat(data.isEmpty() , `is`(true))
    }

    @Test
    fun givenNonDao_whenGetReminders_thenReturnNonEmptyResult() = mainCoroutineRule.runBlockingTest {
        val reminderList = ArrayList<ReminderDTO>()
        for(i in 1..10) {
            reminderList.add(ReminderDTO("title$i", "description$i","location$i",i.toDouble(),i.toDouble(),i.toString()))
        }
        setupRepo(reminderList)
        val result = repo.getReminders()
        val data = (result as Result.Success).data
        assertThat(data == reminderList, `is`(true))

    }

    @Test
    fun givenNonExistingId_whenGetReminderById_thenReturnError() = mainCoroutineRule.runBlockingTest {
        val result = repo.getReminder("1")
        val message = (result as Result.Error).message
        assertThat(message , `is`("Reminder not found!"))
    }

    @Test
    fun givenExistingId_whenGetReminderById_thenReturnCorrectReminder() = mainCoroutineRule.runBlockingTest {
        val reminderList = ArrayList<ReminderDTO>()
        for(i in 1..10) {
            reminderList.add(ReminderDTO("title$i", "description$i","location$i",i.toDouble(),i.toDouble(),i.toString()))
        }
        setupRepo(reminderList)
        val result = repo.getReminder("1")
        val reminder = (result as Result.Success).data
        assertThat(reminder.title , `is`("title1"))
    }

    @Test
    fun givenEmptyDao_whenSaveReminder_thenNewReminderAdded() = mainCoroutineRule.runBlockingTest {
        val newReminder = ReminderDTO("new reminder", "description","location",1.toDouble(),1.toDouble())
        repo.saveReminder(newReminder)
        val data = (repo.getReminders() as Result.Success).data
        assertThat(data.first().title , `is`("new reminder"))
    }

    @Test
    fun givenNonEmptyDao_whenDeleteAllReminders_thenNoDataLeft() = mainCoroutineRule.runBlockingTest {
        val reminderList = ArrayList<ReminderDTO>()
        for(i in 1..10) {
            reminderList.add(ReminderDTO("title$i", "description$i","location$i",i.toDouble(),i.toDouble(),i.toString()))
        }
        setupRepo(reminderList)
        repo.deleteAllReminders()
        val data = (repo.getReminders() as Result.Success).data
        assertThat(data.isEmpty() , `is`(true))
    }
}