package com.childoo.challenge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MathQuestionTest {

    @Test
    fun alwaysFourDistinctNonNegativeOptionsContainingAnswer() {
        val random = Random(42)
        repeat(2_000) {
            val q = MathQuestion.generate(random = random)
            assertEquals(4, q.options.size)
            assertEquals(4, q.options.toSet().size)
            assertTrue(q.options.all { it >= 0 })
            assertTrue(q.answer in q.options)
            assertTrue(q.answer >= 0)
        }
    }
}
