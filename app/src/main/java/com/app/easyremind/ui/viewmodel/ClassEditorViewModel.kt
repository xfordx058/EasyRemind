package com.app.easyremind.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.app.easyremind.data.ClassEntity
import com.app.easyremind.data.ScheduleRepository
import com.app.easyremind.util.ScheduleMath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek

data class ClassFormState(
    val id: Long = -1L,
    val isNew: Boolean = true,
    val subjectName: String = "",
    val instructor: String = "",
    val room: String = "",
    val selectedDays: Set<DayOfWeek> = emptySet(),
    val startMin: Int = -1,
    val endMin: Int = -1,
    val reminderMinutes: Int = 15,
    val isEnabled: Boolean = true,
    val loaded: Boolean = false,
    val loadFailed: Boolean = false,
) {
    val startSet: Boolean get() = startMin >= 0
    val endSet: Boolean get() = endMin >= 0
}

class ClassEditorViewModel(app: Application, classId: Long) : AndroidViewModel(app) {

    private val repository = app.container().repository

    private val _form = MutableStateFlow(ClassFormState(id = classId, isNew = classId < 0))
    val form = _form.asStateFlow()

    /** Snapshot of the last loaded/saved form, used to detect unsaved edits. */
    private var originalForm: ClassFormState? = null

    init {
        if (classId >= 0) {
            viewModelScope.launch {
                val c = repository.getClass(classId)
                if (c == null) {
                    _form.value = _form.value.copy(loaded = true, loadFailed = true)
                    return@launch
                }
                val loaded = ClassFormState(
                    id = c.id,
                    isNew = false,
                    subjectName = c.subjectName,
                    instructor = c.instructor,
                    room = c.room,
                    selectedDays = ScheduleMath.daySetOf(c.daysBitmask),
                    startMin = c.startMin,
                    endMin = c.endMin,
                    reminderMinutes = c.reminderMinutes,
                    isEnabled = c.isEnabled,
                    loaded = true,
                )
                originalForm = loaded
                _form.value = loaded
            }
        } else {
            viewModelScope.launch {
                val settings = repository.getSettings()
                val loaded = _form.value.copy(
                    reminderMinutes = settings.defaultReminderMinutes,
                    loaded = true,
                )
                originalForm = loaded
                _form.value = loaded
            }
        }
    }

    fun hasUnsavedChanges(): Boolean {
        val original = originalForm ?: return false
        val current = _form.value
        return current.subjectName != original.subjectName ||
            current.instructor != original.instructor ||
            current.room != original.room ||
            current.selectedDays != original.selectedDays ||
            current.startMin != original.startMin ||
            current.endMin != original.endMin ||
            current.reminderMinutes != original.reminderMinutes ||
            current.isEnabled != original.isEnabled
    }

    fun onSubjectChange(v: String) = update { it.copy(subjectName = v) }
    fun onInstructorChange(v: String) = update { it.copy(instructor = v) }
    fun onRoomChange(v: String) = update { it.copy(room = v) }

    fun toggleDay(day: DayOfWeek) = update {
        val days = if (day in it.selectedDays) it.selectedDays - day else it.selectedDays + day
        it.copy(selectedDays = days)
    }

    fun setStart(minutes: Int) = update { it.copy(startMin = minutes) }
    fun setEnd(minutes: Int) = update { it.copy(endMin = minutes) }
    fun setReminder(minutes: Int) = update { it.copy(reminderMinutes = minutes) }

    private inline fun update(transform: (ClassFormState) -> ClassFormState) {
        _form.value = transform(_form.value)
    }

    suspend fun save(): ScheduleRepository.SaveResult {
        val f = _form.value
        val entity = ClassEntity(
            id = if (f.isNew) 0L else f.id,
            subjectName = f.subjectName.trim(),
            instructor = f.instructor.trim(),
            room = f.room.trim(),
            daysBitmask = ScheduleMath.bitmaskOf(f.selectedDays),
            startMin = f.startMin,
            endMin = f.endMin,
            reminderMinutes = f.reminderMinutes,
            isEnabled = f.isEnabled,
        )
        return try {
            when (val result = repository.saveClass(entity, isNew = f.isNew)) {
                ScheduleRepository.SaveResult.Ok -> {
                    originalForm = f
                    result
                }
                else -> result
            }
        } catch (e: Exception) {
            ScheduleRepository.SaveResult.Failed(e.message ?: "Could not save the class. Please try again.")
        }
    }

    companion object {
        fun factory(classId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                ClassEditorViewModel(app, classId)
            }
        }
    }
}