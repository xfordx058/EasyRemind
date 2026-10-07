package com.app.easyremind.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.easyremind.data.ClassEntity
import com.app.easyremind.data.SettingsEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

data class ScheduleUiState(
    val classes: List<ClassEntity> = emptyList(),
    val settings: SettingsEntity = SettingsEntity(),
    val selectedDay: DayOfWeek? = LocalDate.now().dayOfWeek,
) {
    val gridMode: Boolean get() = settings.scheduleView == "grid"
}

class ScheduleViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = app.container().repository

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.classesFlow.collect { list ->
                _uiState.value = _uiState.value.copy(classes = list)
            }
        }
        viewModelScope.launch {
            repository.settingsFlow.collect { s ->
                _uiState.value = _uiState.value.copy(settings = s ?: SettingsEntity())
            }
        }
    }

    fun selectDay(day: DayOfWeek?) {
        _uiState.value = _uiState.value.copy(selectedDay = day)
    }

    fun setViewMode(grid: Boolean) {
        val s = _uiState.value.settings
        viewModelScope.launch {
            repository.updateSettings(s.copy(scheduleView = if (grid) "grid" else "list"))
        }
    }
}