package com.app.easyremind

import android.app.Application
import com.app.easyremind.data.AppDatabase
import com.app.easyremind.data.ScheduleRepository
import com.app.easyremind.reminder.Notifier
import com.app.easyremind.reminder.ReminderScheduler

class AppContainer(application: Application) {

    val database: AppDatabase by lazy { AppDatabase.get(application) }

    val notifier: Notifier by lazy { Notifier(application) }

    val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(application) }

    val repository: ScheduleRepository by lazy {
        ScheduleRepository(database, reminderScheduler)
    }
}