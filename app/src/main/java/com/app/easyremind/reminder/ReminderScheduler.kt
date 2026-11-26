package com.app.easyremind.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import com.app.easyremind.data.ClassEntity
import com.app.easyremind.util.ScheduleMath
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderScheduler(private val appContext: Context) {

    private val alarmManager: AlarmManager? = appContext.getSystemService()

    fun rescheduleClass(clazz: ClassEntity, fromNow: LocalDateTime = LocalDateTime.now()) {
        if (!clazz.isEnabled) {
            cancelClass(clazz.id)
            return
        }
        val lead = clazz.reminderMinutes.coerceAtLeast(0)
        for (day in ScheduleMath.daySetOf(clazz.daysBitmask)) {
            var occurrence = ScheduleMath.nextOccurrence(fromNow, day, clazz.startMin, lead)
            fun reminderTime(date: java.time.LocalDate) =
                ScheduleMath.atTimeOn(date, clazz.startMin).minusMinutes(lead.toLong())

            var reminderLocal = reminderTime(occurrence)
            if (!reminderLocal.isAfter(fromNow)) {
                // The ICS-style day matched but seconds put the reminder just
                // behind us: wait for the following week instead of dropping it.
                occurrence = occurrence.plusWeeks(1)
                reminderLocal = reminderTime(occurrence)
            }
            setReminder(clazz, day, occurrence.toEpochDay(), reminderLocal)
        }
    }

    fun cancelClass(classId: Long) {
        for (day in DayOfWeek.entries) {
            val pi = reminderPendingIntent(classId, day, cancelOnly = true)
            alarmManager?.cancel(pi)
        }
    }

    private fun setReminder(
        clazz: ClassEntity,
        day: DayOfWeek,
        epochDay: Long,
        reminderLocal: LocalDateTime,
    ) {
        val am = alarmManager ?: return
        val pi = reminderPendingIntent(
            clazz.id,
            day,
            occurrenceEpochDay = epochDay,
        )
        val triggerAt = reminderLocal.atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        if (triggerAt < System.currentTimeMillis()) return
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            am.canScheduleExactAlarms()
        if (exact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun reminderPendingIntent(
        classId: Long,
        day: DayOfWeek,
        cancelOnly: Boolean = false,
        occurrenceEpochDay: Long = 0L,
    ): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val intent = Intent(appContext, ReminderReceiver::class.java).apply {
            action = ACTION_CLASS_REMINDER
            data = android.net.Uri.parse("easyremind://class/$classId/${day.value}")
            if (!cancelOnly) {
                putExtra(EXTRA_CLASS_ID, classId)
                putExtra(EXTRA_OCCURRENCE_DAY, occurrenceEpochDay)
            }
        }
        return PendingIntent.getBroadcast(
            appContext,
            requestCodeFor(classId, day),
            intent,
            flags,
        )
    }

    private fun requestCodeFor(classId: Long, day: DayOfWeek): Int {
        val base = (classId % 10_000_000L).toInt()
        return base * 10 + day.value
    }

    companion object {
        const val ACTION_CLASS_REMINDER = "com.app.easyremind.CLASS_REMINDER"
        const val EXTRA_CLASS_ID = "com.app.easyremind.CLASS_ID"
        const val EXTRA_START_MIN = "com.app.easyremind.START_MIN"
        const val EXTRA_END_MIN = "com.app.easyremind.END_MIN"
        const val EXTRA_SUBJECT = "com.app.easyremind.SUBJECT"
        const val EXTRA_ROOM = "com.app.easyremind.ROOM"
        const val EXTRA_OCCURRENCE_DAY = "com.app.easyremind.OCCURRENCE_DAY"
    }
}