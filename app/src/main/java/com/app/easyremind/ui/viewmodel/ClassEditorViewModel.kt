package com.app.easyremind.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.app.easyremind.data.ClassEntity
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
    val loaded: Boolean = false,
) {
    val startSet: Boolean get() = startMin >= 0
    val endSet: Boolean get() = endMin >= 0
}

class ClassEditorViewModel(app: Application, classId: Long) : AndroidViewModel(app) {

    private val repository = app.container().repository

    private val _form = MutableStateFlow(ClassFormState(id = classId, isNew = classId < 0))
    val form = _form.asStateFlow()

    init {
        if (classId >= 0) {
            viewModelScope.launch {
                val c = repository.getClass(classId) ?: return@launch
                _form.value = ClassFormState(
                    id = c.id,
                    isNew = false,
                    subjectName = c.subjectName,
                    instructor = c.instructor,
                    room = c.room,
                    selectedDays = com.app.easyremind.util.ScheduleMath.daySetOf(c.daysBitmask),
                    startMin = c.startMin,
                    endMin = c.endMin,
                    reminderMinutes = c.reminderMinutes,
                    loaded = true,
                )
            }
        } else {
            viewModelScope.launch {
                val settings = repository.getSettings()
                _form.value = _form.value.copy(
                    reminderMinutes = settings.defaultReminderMinutes,
                    loaded = true,
                )
            }
        }
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

    suspend fun save(): com.app.easyremind.data.ScheduleRepository.SaveResult {
        val f = _form.value
        val entity = ClassEntity(
            id = f.id,
            subjectName = f.subjectName.trim(),
            instructor = f.instructor.trim(),
            room = f.room.trim(),
            daysBitmask = com.app.easyremind.util.ScheduleMath.bitmaskOf(f.selectedDays),
            startMin = f.startMin,
            endMin = f.endMin,
            reminderMinutes = f.reminderMinutes,
            isEnabled = true,
        )
        return repository.saveClass(entity, isNew = f.isNew)
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