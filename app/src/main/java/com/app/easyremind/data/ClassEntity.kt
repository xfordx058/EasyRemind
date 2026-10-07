package com.app.easyremind.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "classes")
data class ClassEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val subjectName: String,
    val instructor: String = "",
    val room: String = "",
    val daysBitmask: Int = 0,
    val startMin: Int = 0,
    val endMin: Int = 0,
    val reminderMinutes: Int = 15,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)