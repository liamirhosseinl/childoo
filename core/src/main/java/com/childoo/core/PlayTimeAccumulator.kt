package com.childoo.core

/**
 * Pure logic (no Android) that decides when a challenge is due.
 * Kept separate so it can be unit-tested and reused on iOS later.
 */
class PlayTimeAccumulator(
    initialPlayedMs: Long = 0L,
    private val maxTickMs: Long = MAX_TICK_MS,
) {
    var playedMs: Long = initialPlayedMs
        private set

    /**
     * Adds [elapsedMs] when the child is in a target app with the screen on.
     * Large gaps (device sleep, process freeze) are capped so they never count as play.
     */
    fun tick(elapsedMs: Long, inTargetApp: Boolean) {
        if (!inTargetApp || elapsedMs <= 0) return
        playedMs += elapsedMs.coerceAtMost(maxTickMs)
    }

    fun isChallengeDue(intervalMinutes: Int): Boolean =
        playedMs >= intervalMinutes * 60_000L

    fun reset() {
        playedMs = 0L
    }

    companion object {
        const val MAX_TICK_MS = 5_000L
    }
}
