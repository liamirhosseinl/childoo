package com.childoo.lockkit

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build

/**
 * Works out which apps are on screen by reading usage events incrementally.
 *
 * On Android 10+ an activity stays "visible" from RESUMED until STOPPED, so a video in
 * picture-in-picture or split screen still counts as being on screen (and still gets the challenge).
 * Needs the "Usage access" special permission.
 */
internal class ForegroundAppDetector(context: Context) {

    data class Snapshot(val front: String?, val visible: Set<String>)

    private val usageStats = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val event = UsageEvents.Event()
    private var lastQueryAt = System.currentTimeMillis() - LOOKBACK_MS
    private var front: String? = null

    /** Activity key ("package/class") → package, for activities currently resumed or paused-but-visible. */
    private val visibleActivities = LinkedHashMap<String, String>()

    fun poll(): Snapshot {
        val now = System.currentTimeMillis()
        val events = usageStats.queryEvents(lastQueryAt, now)
        if (events != null) {
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pkg = event.packageName ?: continue
                val key = "$pkg/${event.className}"
                when (event.eventType) {
                    RESUMED -> {
                        front = pkg
                        visibleActivities[key] = pkg
                    }
                    PAUSED -> if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) visibleActivities.remove(key)
                    STOPPED -> visibleActivities.remove(key)
                }
            }
        }
        // Small overlap so an event stamped at the boundary is never missed; re-reading is harmless.
        lastQueryAt = now - OVERLAP_MS
        val visible = visibleActivities.values.toMutableSet()
        front?.let { visible += it }
        return Snapshot(front, visible)
    }

    private companion object {
        const val LOOKBACK_MS = 60_000L
        const val OVERLAP_MS = 2_000L

        /** UsageEvents.Event.ACTIVITY_RESUMED (MOVE_TO_FOREGROUND before API 29). */
        const val RESUMED = 1

        /** UsageEvents.Event.ACTIVITY_PAUSED (MOVE_TO_BACKGROUND before API 29). */
        const val PAUSED = 2

        /** UsageEvents.Event.ACTIVITY_STOPPED, API 29+. */
        const val STOPPED = 23
    }
}
