package com.childoo.core

/**
 * Per-game levels and the most recent games, as the game engine expects them.
 * Stored as compact strings ("star-counter:3,word-builder:2") so the codec is plain Kotlin and unit-tested.
 */
data class GameProgress(
    val levels: Map<String, Int> = emptyMap(),
    val recent: List<String> = emptyList(),
) {
    companion object {
        private const val RECENT_LIMIT = 6
        private val ID = Regex("^[a-z0-9-]{1,40}$")

        fun encodeLevels(levels: Map<String, Int>): String =
            levels.entries
                .filter { ID.matches(it.key) }
                .sortedBy { it.key }
                .joinToString(",") { "${it.key}:${it.value.coerceIn(1, 9)}" }

        fun decodeLevels(raw: String): Map<String, Int> =
            raw.split(',').mapNotNull { part ->
                val id = part.substringBefore(':', "")
                val level = part.substringAfter(':', "").toIntOrNull()
                if (ID.matches(id) && level != null) id to level.coerceIn(1, 9) else null
            }.toMap()

        fun encodeRecent(recent: List<String>): String = recent.filter { ID.matches(it) }.joinToString(",")

        fun decodeRecent(raw: String): List<String> = raw.split(',').filter { ID.matches(it) }

        /** Most recent first, no duplicates, capped. */
        fun pushRecent(recent: List<String>, gameId: String): List<String> =
            (listOf(gameId) + recent.filter { it != gameId }).take(RECENT_LIMIT)
    }
}
