package com.voria.kenaret.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.voria.kenaret.KenaretApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Re-plans reminders after a reboot, an app update, or a time/time-zone change. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> Unit
            else -> return
        }
        val app = context.applicationContext as? KenaretApp ?: return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                app.container.scheduler.reschedule()
            } catch (_: Exception) {
                // Never log details: they could include sensitive data.
            } finally {
                pending.finish()
            }
        }
    }
}
