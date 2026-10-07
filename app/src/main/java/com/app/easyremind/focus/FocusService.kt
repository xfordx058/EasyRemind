package com.app.easyremind.focus

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.app.easyremind.reminder.Notifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class FocusService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val handler = Handler(Looper.getMainLooper())

    private val tick = object : Runnable {
        override fun run() {
            update()
            handler.postDelayed(this, 1000L)
        }
    }

    private lateinit var store: TimerStateStore
    private lateinit var notifier: Notifier

    override fun onCreate() {
        super.onCreate()
        store = TimerStateStore(this)
        notifier = Notifier(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (store.status != TimerStatus.Running) {
            store.reset()
            stopSelf()
            return START_NOT_STICKY
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(Notifier.TICKER_ID, buildTickerNotification(), FOREGROUND_TYPE ?: 0)
        } else {
            startForeground(Notifier.TICKER_ID, buildTickerNotification())
        }
        handler.removeCallbacks(tick)
        handler.post(tick)
        return START_STICKY
    }

    private fun buildTickerNotification(): Notification {
        val breakMode = store.mode != TimerMode.Focus
        return notifier.focusTickerNotification(
            title = label(),
            remainingLabel = formatRemaining(store.remainingMs()),
            isBreak = breakMode,
        )
    }

    private fun update() {
        if (store.status != TimerStatus.Running) {
            handler.removeCallbacks(tick)
            stopSelf()
            return
        }
        val remaining = store.remainingMs()
        if (remaining <= 0L) {
            completeSession()
            return
        }
        notifier.sendFocusTicker(store.status, label(), formatRemaining(remaining))
    }

    private fun completeSession() {
        handler.removeCallbacks(tick)
        val wasFocus = store.complete()
        val app = store.sessionsCompleted
        scope.launch {
            notifier.showFocusComplete(
                isBreak = !wasFocus,
                breakLabel = if (wasFocus) "short break" else "next focus start",
            )
        }
        notifier.cancelFocusTicker()
        stopSelf()
    }

    private fun label(): String = when (store.mode) {
        TimerMode.Focus -> "Focus session"
        TimerMode.Short -> "Short break"
        TimerMode.Long -> "Long break"
    }

    private fun formatRemaining(ms: Long): String {
        val totalSeconds = ((ms + 999) / 1000).coerceAtLeast(0L)
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        return "%02d:%02d".format(m, s)
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        if (store.status != TimerStatus.Running && store.status != TimerStatus.Paused) {
            notifier.cancelFocusTicker()
        }
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        // specialUse is API 34+. Fall back for older devices.
        private val FOREGROUND_TYPE: Int? = getForegroundType()

        private fun getForegroundType(): Int? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                null
            }

        fun start(context: Context) {
            val intent = Intent(context, FocusService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FocusService::class.java))
        }
    }
}