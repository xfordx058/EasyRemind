package com.app.easyremind.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val notificationsEnabled: Boolean = true,
    val defaultReminderMinutes: Int = 15,
    val scheduleView: String = "list",
    val focusDuration: Int = 25,
    val shortBreakDuration: Int = 5,
    val longBreakDuration: Int = 15,
    val sessionCount: Int = 4,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val theme: String = "system",
    val showInstructor: Boolean = true,
    val showRoom: Boolean = true,
    val weekStartsOn: String = "MONDAY",
)