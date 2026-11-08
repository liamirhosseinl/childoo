package com.childoo.core

data class ChildProfile(
    val name: String = "",
    val age: Int = DEFAULT_AGE,
) {
    companion object {
        const val MIN_AGE = 4
        const val MAX_AGE = 10
        const val DEFAULT_AGE = 6
        val AGE_OPTIONS = (MIN_AGE..MAX_AGE).toList()
    }
}
