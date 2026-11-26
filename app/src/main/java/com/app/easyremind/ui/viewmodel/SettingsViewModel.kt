package com.app.easyremind.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.app.easyremind.data.SettingsEntity
import com.app.easyremind.data.ScheduleRepository

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = app.container().repository

    private val _settings = MutableStateFlow<SettingsEntity?>(null)
    val settings = _settings.asStateFlow()

    init {
        viewModelScope.launch {
            _settings.value = repository.getSettings()
            repository.settingsFlow.collect { _settings.value = it }
        }
    }

    fun update(transform: (SettingsEntity) -> SettingsEntity) {
        viewModelScope.launch {
            // Read the persisted value instead of a possibly-stale flow value,
            // otherwise a quick second toggle can undo the first one.
            val next = transform(repository.getSettings())
            repository.updateSettings(next)
            getApplication<com.app.easyremind.EasyRemindApp>().container.notifier
                .updateChannels(next.soundEnabled, next.vibrationEnabled)
        }
    }

    suspend fun exportJson(): String = repository.exportJson()

    suspend fun importJson(text: String): ScheduleRepository.ImportResult =
        repository.importJson(text)

    suspend fun rescheduleAll() = repository.rescheduleAll()
}