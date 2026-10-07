package com.app.easyremind.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.app.easyremind.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            -> Unit
            else -> return
        }
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val db = AppDatabase.get(appContext)
                val settings = db.settingsDao().get()
                val scheduler = ReminderScheduler(appContext)
                if (settings == null || settings.notificationsEnabled) {
                    for (clazz in db.classDao().getAll()) {
                        scheduler.rescheduleClass(clazz)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}