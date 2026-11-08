package com.childoo.challenge

import kotlin.random.Random

/** One visual-math question with four shuffled options. Pure Kotlin, unit-testable. */
data class MathQuestion(
    val left: Int,
    val right: Int,
    val isAddition: Boolean,
    val options: List<Int>,
) {
    val answer: Int get() = if (isAddition) left + right else left - right
    val prompt: String get() = "$left ${if (isAddition) "+" else "−"} $right"

    companion object {
        /**
         * Generates an age-appropriate question. Subtraction never goes below zero,
         * and the four options are distinct and non-negative.
         */
        fun generate(maxOperand: Int = 10, random: Random = Random.Default): MathQuestion {
            val isAddition = random.nextBoolean()
            var a = random.nextInt(1, maxOperand + 1)
            var b = random.nextInt(1, maxOperand + 1)
            if (!isAddition && b > a) {
                val t = a; a = b; b = t
            }
            val answer = if (isAddition) a + b else a - b
            val options = mutableSetOf(answer)
            var guard = 0
            while (options.size < 4 && guard < 100) {
                val candidate = answer + random.nextInt(-3, 4)
                if (candidate >= 0) options.add(candidate)
                guard++
            }
            var filler = answer + 4
            while (options.size < 4) options.add(filler++)
            return MathQuestion(a, b, isAddition, options.shuffled(random))
        }
    }
}
