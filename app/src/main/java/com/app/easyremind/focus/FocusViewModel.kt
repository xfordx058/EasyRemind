package com.app.easyremind.focus

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.easyremind.EasyRemindApp
import com.app.easyremind.data.SettingsEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

data class FocusUiState(
    val mode: TimerMode = TimerMode.Focus,
    val status: TimerStatus = TimerStatus.Idle,
    val remainingMs: Long = 0L,
    val totalMs: Long = 0L,
    val sessionsCompleted: Int = 0,
    val completionSeq: Int = 0,
    val lastCompletedMode: TimerMode? = null,
    val focusDuration: Int = 25,
    val shortBreakDuration: Int = 5,
    val longBreakDuration: Int = 15,
    val sessionCount: Int = 4,
) {
    val isBreak: Boolean get() = mode != TimerMode.Focus
    val sessionNumber: Int get() = (sessionsCompleted % sessionCount).coerceAtLeast(0) + 1
    val progress: Float
        get() = if (totalMs <= 0L) 0f else (remainingMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
}

class FocusViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as EasyRemindApp).container
    private val store = TimerStateStore(app)

    private val settingsCache = MutableStateFlow<SettingsEntity?>(null)

    private val _uiState = MutableStateFlow(buildState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            container.repository.settingsFlow.collect { settings ->
                settingsCache.value = settings
                _uiState.value = buildState()
            }
        }
        viewModelScope.launch {
            tickerFlow().collect {
                _uiState.value = buildState()
            }
        }
    }

    private fun tickerFlow() = flow {
        while (true) {
            emit(Unit)
            delay(250L)
        }
    }

    private fun buildState(): FocusUiState {
        val s = settingsCache.value ?: SettingsEntity()
        return FocusUiState(
            mode = store.mode,
            status = store.status,
            remainingMs = store.remainingMs(),
            totalMs = store.totalMs,
            sessionsCompleted = store.sessionsCompleted,
            completionSeq = store.completionSeq,
            lastCompletedMode = store.lastCompletedMode,
            focusDuration = s.focusDuration,
            shortBreakDuration = s.shortBreakDuration,
            longBreakDuration = s.longBreakDuration,
            sessionCount = s.sessionCount,
        )
    }

    fun startCurrentMode() {
        val current = _uiState.value
        if (current.status == TimerStatus.Running || current.status == TimerStatus.Paused) return
        val durationMs = when (current.mode) {
            TimerMode.Focus -> current.focusDuration.toLong()
            TimerMode.Short -> current.shortBreakDuration.toLong()
            TimerMode.Long -> current.longBreakDuration.toLong()
        } * 60_000L
        store.start(current.mode, durationMs)
        container.notifier.cancelFocusTicker()
        FocusService.start(getApplication())
        _uiState.value = buildState()
    }

    fun pause() {
        store.pause()
        _uiState.value = buildState()
    }

    fun resume() {
        if (store.status != TimerStatus.Paused) return
        store.resume()
        FocusService.start(getApplication())
        _uiState.value = buildState()
    }

    fun reset() {
        store.reset()
        FocusService.stop(getApplication())
        _uiState.value = buildState()
    }

    fun setMode(mode: TimerMode) {
        if (store.status == TimerStatus.Running) return
        store.mode = mode
        store.totalMs = 0L
        store.remainingWhenPausedMs = 0L
        _uiState.value = buildState()
    }

    fun dismissCompletion() {
        store.clearCompletionMarker()
        _uiState.value = buildState()
    }

    fun completionPending(): Boolean = _uiState.value.completionSeq > 0
}