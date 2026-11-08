package com.childoo.lockkit

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.childoo.core.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Brings protection back after a reboot or an app update, if the parent had it on. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (SettingsRepository(app).settings.first().protectionEnabled) {
                    LockKit.start(app)
                }
            } catch (_: Exception) {
                // Never crash at boot; the parent can re-enable from the app.
            } finally {
                pending.finish()
            }
        }
    }
}
