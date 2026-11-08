package com.childoo.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayTimeAccumulatorTest {

    @Test
    fun countsOnlyTimeInTargetApps() {
        val acc = PlayTimeAccumulator()
        acc.tick(1_000, inTargetApp = true)
        acc.tick(1_000, inTargetApp = false)
        assertEquals(1_000, acc.playedMs)
    }

    @Test
    fun capsLargeGapsSoSleepIsNotPlay() {
        val acc = PlayTimeAccumulator()
        acc.tick(60 * 60_000L, inTargetApp = true)
        assertEquals(PlayTimeAccumulator.MAX_TICK_MS, acc.playedMs)
    }

    @Test
    fun challengeDueAfterInterval() {
        val acc = PlayTimeAccumulator(initialPlayedMs = 59_000)
        assertFalse(acc.isChallengeDue(intervalMinutes = 1))
        acc.tick(1_000, inTargetApp = true)
        assertTrue(acc.isChallengeDue(intervalMinutes = 1))
        acc.reset()
        assertFalse(acc.isChallengeDue(intervalMinutes = 1))
    }

    @Test
    fun ignoresNegativeElapsed() {
        val acc = PlayTimeAccumulator(initialPlayedMs = 10)
        acc.tick(-500, inTargetApp = true)
        assertEquals(10, acc.playedMs)
    }
}
