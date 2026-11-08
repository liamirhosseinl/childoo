package com.childoo.lockkit

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** The only entry point the app module needs. */
object LockKit {

    fun start(context: Context) {
        send(context, UsageMonitorService.ACTION_START)
        ProtectionWatchdog.schedule(context)
    }

    fun stop(context: Context) {
        ProtectionWatchdog.cancel(context)
        context.stopService(Intent(context, UsageMonitorService::class.java))
    }

    /** Shows a challenge right away, on top of whatever is on screen. For setup and demos. */
    fun testChallenge(context: Context) = send(context, UsageMonitorService.ACTION_TEST_CHALLENGE)

    private fun send(context: Context, action: String) {
        val intent = Intent(context, UsageMonitorService::class.java).setAction(action)
        ContextCompat.startForegroundService(context, intent)
    }
}
