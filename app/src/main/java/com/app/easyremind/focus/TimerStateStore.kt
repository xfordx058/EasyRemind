package com.app.easyremind.focus

import android.content.Context

enum class TimerMode(val key: String) {
    Focus("focus"),
    Short("short"),
    Long("long"),
}

enum class TimerStatus {
    Idle,
    Running,
    Paused,
}

/**
 * Timestamp-based timer state so the remaining time stays accurate when the
 * app is closed, minimized, or the process is restarted.
 */
class TimerStateStore(context: Context) {

    private val prefs = context.getSharedPreferences("focus_timer", Context.MODE_PRIVATE)

    var status: TimerStatus
        get() = when (prefs.getString(KEY_STATUS, TimerStatus.Idle.name)) {
            TimerStatus.Running.name -> TimerStatus.Running
            TimerStatus.Paused.name -> TimerStatus.Paused
            else -> TimerStatus.Idle
        }
        set(value) = prefs.edit().putString(KEY_STATUS, value.name).apply()

    var mode: TimerMode
        get() = when (prefs.getString(KEY_MODE, TimerMode.Focus.name)) {
            TimerMode.Short.name -> TimerMode.Short
            TimerMode.Long.name -> TimerMode.Long
            else -> TimerMode.Focus
        }
        set(value) = prefs.edit().putString(KEY_MODE, value.name).apply()

    var endAtMs: Long
        get() = prefs.getLong(KEY_END_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_END_AT, value).apply()

    var remainingWhenPausedMs: Long
        get() = prefs.getLong(KEY_REMAINING_PAUSED, 0L)
        set(value) = prefs.edit().putLong(KEY_REMAINING_PAUSED, value).apply()

    var sessionsCompleted: Int
        get() = prefs.getInt(KEY_SESSIONS, 0)
        set(value) = prefs.edit().putInt(KEY_SESSIONS, value).apply()

    var totalMs: Long
        get() = prefs.getLong(KEY_TOTAL, 0L)
        set(value) = prefs.edit().putLong(KEY_TOTAL, value).apply()

    var completionSeq: Int
        get() = prefs.getInt(KEY_COMPLETION_SEQ, 0)
        private set(value) = prefs.edit().putInt(KEY_COMPLETION_SEQ, value).apply()

    var lastCompletedMode: TimerMode?
        get() = prefs.getString(KEY_LAST_COMPLETED, null)?.let { value ->
            TimerMode.entries.firstOrNull { it.key == value }
        }
        private set(value) = prefs.edit().putString(KEY_LAST_COMPLETED, value?.key).apply()

    fun remainingMs(nowMs: Long = System.currentTimeMillis()): Long = when (status) {
        TimerStatus.Running -> (endAtMs - nowMs).coerceAtLeast(0L)
        TimerStatus.Paused -> remainingWhenPausedMs
        TimerStatus.Idle -> totalMs
    }

    fun start(mode: TimerMode, durationMs: Long) {
        this.mode = mode
        totalMs = durationMs
        remainingWhenPausedMs = durationMs
        endAtMs = System.currentTimeMillis() + durationMs
        status = TimerStatus.Running
    }

    fun pause(nowMs: Long = System.currentTimeMillis()) {
        if (status != TimerStatus.Running) return
        remainingWhenPausedMs = (endAtMs - nowMs).coerceAtLeast(0L)
        totalMs = totalMs
        status = TimerStatus.Paused
    }

    fun resume() {
        if (status != TimerStatus.Paused) return
        endAtMs = System.currentTimeMillis() + remainingWhenPausedMs.coerceAtLeast(0L)
        status = TimerStatus.Running
    }

    fun reset() {
        totalMs = 0L
        remainingWhenPausedMs = 0L
        endAtMs = 0L
        status = TimerStatus.Idle
    }

    fun clearCompletionMarker() {
        prefs.edit()
            .putInt(KEY_COMPLETION_SEQ, 0)
            .remove(KEY_LAST_COMPLETED)
            .apply()
    }

    /** Completes the current run; returns true if it was a focus session. */
    fun complete(nowMs: Long = System.currentTimeMillis()): Boolean {
        if (status != TimerStatus.Running) return false
        val wasFocus = mode == TimerMode.Focus
        lastCompletedMode = mode
        completionSeq = completionSeq + 1
        if (wasFocus) {
            sessionsCompleted = sessionsCompleted + 1
        }
        totalMs = 0L
        endAtMs = 0L
        remainingWhenPausedMs = 0L
        status = TimerStatus.Idle
        return wasFocus
    }

    companion object {
        private const val KEY_STATUS = "status"
        private const val KEY_MODE = "mode"
        private const val KEY_END_AT = "end_at"
        private const val KEY_REMAINING_PAUSED = "remaining_paused"
        private const val KEY_SESSIONS = "sessions"
        private const val KEY_TOTAL = "total"
        private const val KEY_COMPLETION_SEQ = "completion_seq"
        private const val KEY_LAST_COMPLETED = "last_completed"
    }
}