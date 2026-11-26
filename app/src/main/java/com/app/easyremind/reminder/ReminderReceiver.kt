package com.app.easyremind.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.app.easyremind.data.AppDatabase
import com.app.easyremind.util.ScheduleMath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ACTION_CLASS_REMINDER) return
        val classId = intent.getLongExtra(ReminderScheduler.EXTRA_CLASS_ID, -1L)
        if (classId < 0) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.get(context)
                val clazz = db.classDao().getById(classId)
                val now = LocalDateTime.now()
                val settings = db.settingsDao().get()
                if (clazz == null || !clazz.isEnabled) return@launch
                if (settings != null && !settings.notificationsEnabled) return@launch

                val occurrence = LocalDate.ofEpochDay(
                    intent.getLongExtra(ReminderScheduler.EXTRA_OCCURRENCE_DAY, now.toLocalDate().toEpochDay())
                )
                val end = ScheduleMath.atTimeOn(occurrence, clazz.endMin)
                if (now.isAfter(end)) return@launch

                val start = ScheduleMath.atTimeOn(occurrence, clazz.startMin)
                val minutesUntil = ChronoUnit.MINUTES.between(now, start)

                Notifier(context).showClassReminder(clazz, minutesUntil)
                ReminderScheduler(context.applicationContext).rescheduleClass(clazz)
            } finally {
                pendingResult.finish()
            }
        }
    }
}