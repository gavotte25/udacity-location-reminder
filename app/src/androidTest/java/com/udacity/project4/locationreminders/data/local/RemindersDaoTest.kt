package com.udacity.project4.locationreminders.data.local

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.MainCoroutineRule

import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {
    private lateinit var dao: RemindersDao
    private lateinit var db: RemindersDatabase

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, RemindersDatabase::class.java).build()
        dao = db.reminderDao()
    }

    @After
    fun closeDb() {
        db.close()
        stopKoin()
    }

    @Test
    fun givenEmptyDb_whenGetReminders_thenReturnEmptyResult() = mainCoroutineRule.runBlockingTest{
        val result = dao.getReminders()
        assertThat(result.isEmpty(), `is`(true))
    }

    @Test
    fun givenEmptyDb_whenSaveReminderAndGetReminders_thenReturnNonEmptyResult() = mainCoroutineRule.runBlockingTest {
        for(i in 1..10) {
            dao.saveReminder(ReminderDTO("title$i", "description$i","location$i",i.toDouble(),i.toDouble()))
        }
        val result = dao.getReminders()
        assertThat(result.size, `is`(10))
        assertThat(result[1].title, `is`("title2")) // to make sure saved data is valid
    }

    @Test
    fun givenNonExistingId_whenGetReminderById_thenReturnNull() = mainCoroutineRule.runBlockingTest {
        for(i in 1..10) {
            dao.saveReminder(ReminderDTO("title$i", "description$i","location$i",i.toDouble(),i.toDouble(),i.toString()))
        }
        val result = dao.getReminderById("11")
        assertThat(result==null, `is`(true))
    }

    @Test
    fun givenExistingId_whenGetReminderById_thenReturnCorrectReminder() = mainCoroutineRule.runBlockingTest{
        for(i in 1..10) {
            dao.saveReminder(ReminderDTO("title$i", "description$i","location$i",i.toDouble(),i.toDouble(), i.toString()))
        }
        val result = dao.getReminderById("5")
        assertThat(requireNotNull(result).title, `is`("title5"))
    }

    // test case for new reminder has been already covered
    @Test
    fun givenExistingReminder_whenSaveReminder_thenReminderUpdated() = mainCoroutineRule.runBlockingTest{
        for(i in 1..10) {
            dao.saveReminder(ReminderDTO("title$i", "description$i","location$i",i.toDouble(),i.toDouble(),i.toString()))
        }
        val newReminder = ReminderDTO("title1", "description updated","location1",1.toDouble(),1.toDouble(),"1")
        dao.saveReminder(newReminder)
        val updatedReminder = dao.getReminderById("1")
        val allReminders = dao.getReminders()
        assertThat(requireNotNull(updatedReminder).title, `is`("title1"))
        assertThat(updatedReminder.description, `is`("description updated"))
        assertThat(allReminders.size, `is`(10)) // make sure no new record added
    }

    @Test
    fun givenNonEmptyDb_whenDeleteAllReminders_thenAllDataDeleted() = mainCoroutineRule.runBlockingTest {
        for(i in 1..10) {
            dao.saveReminder(ReminderDTO("title$i", "description$i","location$i",i.toDouble(),i.toDouble()))
        }
        var result = dao.getReminders()
        assertThat(result.size, `is`(10))
        dao.deleteAllReminders()
        result = dao.getReminders()
        assertThat(result.isEmpty(), `is`(true))
    }

}