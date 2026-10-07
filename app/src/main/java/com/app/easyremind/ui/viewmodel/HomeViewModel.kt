package com.app.easyremind.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.easyremind.data.ClassEntity
import com.app.easyremind.data.SettingsEntity
import com.app.easyremind.util.ScheduleMath
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDateTime

data class HomeUiState(
    val classes: List<ClassEntity> = emptyList(),
    val settings: SettingsEntity = SettingsEntity(),
    val next: ScheduleMath.NextClass = ScheduleMath.NextClass.NoClasses,
    val today: LocalDateTime = LocalDateTime.now(),
) {
    val nowMin: Int get() = today.hour * 60 + today.minute
    val todaysClasses: List<ClassEntity>
        get() = classes
            .filter { it.isEnabled && ScheduleMath.hasDay(it.daysBitmask, today.dayOfWeek) }
            .sortedBy { it.startMin }
    val firstName: String get() = "Student"
    val greeting: String
        get() = when (today.hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    val dateLabel: String
        get() = today.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d", java.util.Locale.US))
}

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = app.container().repository

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.classesFlow.collect { classes ->
                _uiState.value = _uiState.value.copy(classes = classes)
            }
        }
        viewModelScope.launch {
            repository.settingsFlow.collect { settings ->
                _uiState.value = _uiState.value.copy(
                    settings = settings ?: SettingsEntity(),
                )
            }
        }
        viewModelScope.launch {
            ticker().collect { now ->
                _uiState.value = _uiState.value.copy(
                    today = now,
                    next = ScheduleMath.nextClass(_uiState.value.classes, now),
                )
            }
        }
    }

    private fun ticker() = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(15_000L)
        }
    }

    fun dayOfWeek() = _uiState.value.today.dayOfWeek
}

fun LocalDateTime.shortDay(): String =
    dayOfWeek.name.lowercase().replaceFirstChar(Char::uppercase)