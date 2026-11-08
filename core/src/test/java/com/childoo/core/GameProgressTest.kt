package com.childoo.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameProgressTest {

    @Test
    fun levelsRoundTrip() {
        val levels = mapOf("star-counter" to 3, "word-builder" to 1)
        assertEquals(levels, GameProgress.decodeLevels(GameProgress.encodeLevels(levels)))
    }

    @Test
    fun corruptLevelsAreIgnored() {
        assertEquals(mapOf("ok-game" to 2), GameProgress.decodeLevels("ok-game:2,bad id:3,,x:,:4,Up:1"))
    }

    @Test
    fun recentIsMostRecentFirstWithoutDuplicates() {
        val r = GameProgress.pushRecent(listOf("a", "b", "c"), "b")
        assertEquals(listOf("b", "a", "c"), r)
    }

    @Test
    fun recentIsCapped() {
        var r = emptyList<String>()
        repeat(10) { r = GameProgress.pushRecent(r, "g$it") }
        assertEquals(6, r.size)
        assertEquals("g9", r.first())
    }

    @Test
    fun pinHashMatchesOnlyTheSamePin() {
        val salt = ParentPin.newSalt()
        val hash = ParentPin.hash("2580", salt)
        assertTrue(ParentPin.matches("2580", salt, hash))
        assertFalse(ParentPin.matches("2581", salt, hash))
        assertFalse(ParentPin.matches("2580", ParentPin.newSalt(), hash))
    }

    @Test
    fun pinValidation() {
        assertTrue(ParentPin.isValid("0000"))
        assertFalse(ParentPin.isValid("123"))
        assertFalse(ParentPin.isValid("12a4"))
    }
}
