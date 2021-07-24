package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

class AndroidTestFakeDataSource(private val data: ArrayList<ReminderDTO>, private val errorFlag: Boolean = false) : ReminderDataSource {
    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return if(errorFlag) {
            Result.Error("Error")
        } else {
            Result.Success(data)
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        data.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        val reminder = data.find { it.id == id }
        return if (reminder==null) {
            Result.Error("Not exists")
        } else {
            Result.Success(reminder)
        }
    }

    override suspend fun deleteAllReminders() {
        data.clear()
    }
}