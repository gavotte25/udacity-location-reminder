package com.udacity.project4.locationreminders.data.local;

import com.udacity.project4.locationreminders.data.dto.ReminderDTO

class FakeDao(private val reminderList: ArrayList<ReminderDTO> = arrayListOf(), private val errorFlag: Boolean = false): RemindersDao {

    override suspend fun getReminders(): List<ReminderDTO> {
        if (errorFlag) throw Exception("My Error")
        return reminderList
    }

    override suspend fun getReminderById(reminderId: String): ReminderDTO? {
        if (errorFlag) throw Exception("My Error")
        return reminderList.find { it.id == reminderId }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        val tempReminder = reminderList.find{it.id == reminder.id}
        if (tempReminder == null) {
            reminderList.add(reminder)
        } else {
            reminderList[reminderList.indexOf(tempReminder)] = reminder
        }
    }

    override suspend fun deleteAllReminders() {
        reminderList.clear()
    }
}
