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
            repository.settingsFlow.collect { _settings.value = it }
        }
    }

    fun update(transform: (SettingsEntity) -> SettingsEntity) {
        val current = _settings.value ?: return
        val next = transform(current)
        viewModelScope.launch {
            repository.updateSettings(next)
            getApplication<com.app.easyremind.EasyRemindApp>().container.notifier.apply {
                updateChannels(next.soundEnabled, next.vibrationEnabled)
            }
            _settings.value = next
        }
    }

    suspend fun exportJson(): String = repository.exportJson()

    suspend fun importJson(text: String): ScheduleRepository.ImportResult =
        repository.importJson(text)

    suspend fun rescheduleAll() = repository.rescheduleAll()
}