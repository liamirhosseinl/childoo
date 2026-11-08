package com.childoo.app

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/**
 * After the parent flips a switch in system Settings, brings Childoo back to the front by itself,
 * so granting permissions feels like one continuous flow instead of a hunt for the back button.
 *
 * Overlay permission is requested first on purpose: once it is granted, Android allows Childoo
 * to come back to the front on its own for the next step.
 */
object AutoReturn {
    private val handler = Handler(Looper.getMainLooper())
    private var pending: Runnable? = null

    fun watch(context: Context, isGranted: (Context) -> Boolean) {
        cancel()
        val app = context.applicationContext
        val startedAt = SystemClock.uptimeMillis()
        val check = object : Runnable {
            override fun run() {
                when {
                    isGranted(app) -> {
                        pending = null
                        bringToFront(app)
                    }
                    SystemClock.uptimeMillis() - startedAt > TIMEOUT_MS -> pending = null
                    else -> handler.postDelayed(this, POLL_MS)
                }
            }
        }
        pending = check
        handler.postDelayed(check, POLL_MS)
    }

    fun cancel() {
        pending?.let { handler.removeCallbacks(it) }
        pending = null
    }

    private fun bringToFront(context: Context) {
        val intent = Intent(context, MainActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
        )
        try {
            context.startActivity(intent)
        } catch (_: RuntimeException) {
            // Android refused a background start; the parent comes back by hand and the screen refreshes.
        }
    }

    private const val POLL_MS = 400L
    private const val TIMEOUT_MS = 120_000L
}
