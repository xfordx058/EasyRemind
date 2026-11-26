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
    val suggestedBreak: TimerMode = TimerMode.Short,
) {
    val isBreak: Boolean get() = mode != TimerMode.Focus
    val sessionNumber: Int get() = (sessionsCompleted % sessionCount.coerceAtLeast(1)) + 1
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
        val status = store.status
        // An untouched timer has no stored duration; show the configured one
        // so the UI never starts at 00:00.
        val idleDurationMs = when (store.mode) {
            TimerMode.Focus -> s.focusDuration
            TimerMode.Short -> s.shortBreakDuration
            TimerMode.Long -> s.longBreakDuration
        }.toLong() * 60_000L
        val showIdleDuration = status == TimerStatus.Idle && store.totalMs <= 0L
        val longBreakDue = store.lastCompletedMode == TimerMode.Focus &&
            store.sessionsCompleted > 0 &&
            s.sessionCount > 0 &&
            store.sessionsCompleted % s.sessionCount == 0
        return FocusUiState(
            mode = store.mode,
            status = status,
            remainingMs = if (showIdleDuration) idleDurationMs else store.remainingMs(),
            totalMs = if (showIdleDuration) idleDurationMs else store.totalMs,
            sessionsCompleted = store.sessionsCompleted,
            completionSeq = store.completionSeq,
            lastCompletedMode = store.lastCompletedMode,
            focusDuration = s.focusDuration,
            shortBreakDuration = s.shortBreakDuration,
            longBreakDuration = s.longBreakDuration,
            sessionCount = s.sessionCount,
            suggestedBreak = if (longBreakDue) TimerMode.Long else TimerMode.Short,
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
        // Changing mode while paused used to leave a stale end time and an
        // empty remaining duration, which completed the new timer instantly.
        if (store.status != TimerStatus.Idle) return
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