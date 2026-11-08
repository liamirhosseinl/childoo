package com.childoo.lockkit

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

/**
 * Works out which app is in front by reading usage events incrementally.
 * Needs the "Usage access" special permission, which the parent grants once.
 */
internal class ForegroundAppDetector(context: Context) {

    private val usageStats = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val event = UsageEvents.Event()
    private var lastQueryAt = System.currentTimeMillis() - LOOKBACK_MS
    private var foreground: String? = null

    /** Returns the package currently in front, or null if unknown. Call about once a second. */
    fun poll(): String? {
        val now = System.currentTimeMillis()
        val events = usageStats.queryEvents(lastQueryAt, now) ?: return foreground
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            @Suppress("DEPRECATION")
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                // Same value as ACTIVITY_RESUMED on API 29+.
                foreground = event.packageName
            }
        }
        // Small overlap so an event stamped at the boundary is never missed; re-reading is harmless.
        lastQueryAt = now - OVERLAP_MS
        return foreground
    }

    private companion object {
        const val LOOKBACK_MS = 60_000L
        const val OVERLAP_MS = 2_000L
    }
}
