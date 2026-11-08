package com.childoo.core

import org.junit.Assert.assertEquals
import org.junit.Test

class RewardsTest {

    @Test
    fun walletRoundTrip() {
        val w = Wallet(coins = 75, xp = 230, owned = listOf("cap", "top-hat"), equipped = mapOf("hat" to "top-hat"))
        assertEquals(w, Wallet.decode(Wallet.encode(w)))
    }

    @Test
    fun corruptWalletIsCleaned() {
        val w = Wallet.decode("coins=-3;xp=abc;owned=cap,BAD ID,cap;equipped=hat:crown,face:cap,nose:cap")
        assertEquals(Wallet(coins = 0, xp = 0, owned = listOf("cap"), equipped = mapOf("face" to "cap")), w)
        assertEquals(Wallet(), Wallet.decode(""))
    }

    @Test
    fun levelsMatchTheEngine() {
        assertEquals(1, Wallet.levelFromXp(0))
        assertEquals(1, Wallet.levelFromXp(79))
        assertEquals(2, Wallet.levelFromXp(80))
        assertEquals(3, Wallet.levelFromXp(200))
        assertEquals(4, Wallet.levelFromXp(360))
    }

    @Test
    fun streakGrowsDailyAndLapses() {
        val day = 20_000L
        var s = Streak()
        assertEquals(1, s.ifPlayedOn(day))
        s = s.playedOn(day)
        assertEquals(1, s.ifPlayedOn(day))
        s = s.playedOn(day + 1)
        assertEquals(2, s.days)
        assertEquals(2, s.current(day + 2))
        assertEquals(0, s.current(day + 3))
        assertEquals(1, s.ifPlayedOn(day + 5))
    }
}
