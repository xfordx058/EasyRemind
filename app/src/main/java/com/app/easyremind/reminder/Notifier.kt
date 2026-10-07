package com.app.easyremind.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.app.easyremind.R
import com.app.easyremind.data.ClassEntity
import com.app.easyremind.util.ScheduleMath

class Notifier(private val appContext: Context) {

    private val notifManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notifManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_CLASS_REMINDERS,
                    "Class Reminders",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Reminders before each class"
                    enableVibration(true)
                }
            )
            notifManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_FOCUS,
                    "Focus Timer",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Focus session notifications"
                    enableVibration(true)
                }
            )
        }
    }

    fun updateChannels(soundEnabled: Boolean, vibrationEnabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            listOf(CHANNEL_CLASS_REMINDERS, CHANNEL_FOCUS).forEach { id ->
                val channel = notifManager.getNotificationChannel(id) ?: return@forEach
                if (soundEnabled) {
                    val attrs = android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                    channel.setSound(
                        android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION),
                        attrs,
                    )
                } else {
                    channel.setSound(null, null)
                }
                channel.enableVibration(vibrationEnabled)
                notifManager.createNotificationChannel(channel)
            }
        }
    }

    fun showClassReminder(clazz: ClassEntity, minutesUntil: Long) {
        if (!NotificationManagerCompat.from(appContext).areNotificationsEnabled()) return
        val startsText = when {
            minutesUntil <= 0 -> "Starting now"
            minutesUntil == 1L -> "Starts in 1 minute"
            else -> "Starts in $minutesUntil minutes"
        }
        val subj = clazz.subjectName
        val infoLine = listOfNotNull(
            clazz.room.ifBlank { null },
            ScheduleMath.formatTimeRange(clazz.startMin, clazz.endMin),
        ).joinToString("  ")
        val notification = NotificationCompat.Builder(appContext, CHANNEL_CLASS_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(subj)
            .setContentText(startsText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$startsText\n\n$infoLine")
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        notify(appContext, notificationIdForClass(clazz.id), notification)
    }

    fun showFocusComplete(isBreak: Boolean, breakLabel: String) {
        if (!NotificationManagerCompat.from(appContext).areNotificationsEnabled()) return
        val notification = NotificationCompat.Builder(appContext, CHANNEL_FOCUS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (isBreak) "Break Complete" else "Focus Complete")
            .setContentText(
                if (isBreak) "Back to it — start your next focus session."
                else "Time for a $breakLabel break."
            )
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        notify(appContext, 2001, notification)
    }

    fun focusTickerNotification(
        title: String,
        remainingLabel: String,
        isBreak: Boolean,
    ): Notification {
        val kind = if (isBreak) "BREAK" else "FOCUS"
        return NotificationCompat.Builder(appContext, CHANNEL_FOCUS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$kind — $remainingLabel")
            .setContentText(if (isBreak) "Short break in progress." else "Keep going. Stay focused.")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    fun sendFocusTicker(status: com.app.easyremind.focus.TimerStatus, title: String, remainingLabel: String) {
        if (!NotificationManagerCompat.from(appContext).areNotificationsEnabled()) return
        val paused = status == com.app.easyremind.focus.TimerStatus.Paused
        val kind = (if (title.contains("break", ignoreCase = true)) "BREAK" else "FOCUS")
        val notification = NotificationCompat.Builder(appContext, CHANNEL_FOCUS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (paused) "Paused — $remainingLabel" else "$kind — $remainingLabel")
            .setContentText(if (paused) "Tap the app to resume." else if (title.contains("break", ignoreCase = true)) "Short break in progress." else "Keep going. Stay focused.")
            .setOngoing(!paused)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        try {
            NotificationManagerCompat.from(appContext).notify(TICKER_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    fun cancelFocusTicker() {
        NotificationManagerCompat.from(appContext).cancel(TICKER_ID)
    }

    private fun notify(context: Context, id: Int, notification: Notification) {
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // notifications disabled
        }
    }

    private fun notificationIdForClass(classId: Long): Int =
        (900 + (classId % 50_000)).toInt()

    companion object {
        const val CHANNEL_CLASS_REMINDERS = "class_reminders"
        const val CHANNEL_FOCUS = "focus_sessions"
        const val TICKER_ID = 1001
    }
}