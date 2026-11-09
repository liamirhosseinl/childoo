package com.childoo.core

/**
 * The child's coins, buddy and accessories. Mirrors Wallet in games/src/engine/economy.ts;
 * the game engine owns the rules, Android only stores what it hands back.
 */
data class Wallet(
    val coins: Int = 0,
    val xp: Int = 0,
    val owned: List<String> = emptyList(),
    val equipped: Map<String, String> = emptyMap(),
) {
    val level: Int get() = levelFromXp(xp)

    companion object {
        private val ID = Regex("^[a-z0-9-]{1,32}$")
        private val SLOTS = setOf("hat", "face", "back")

        /** Same curve as the engine: 80, 200, 360, 560 ... XP for levels 2, 3, 4, 5 ... */
        fun xpForLevel(level: Int): Int = if (level <= 1) 0 else 20 * (level - 1) * (level + 2)

        fun levelFromXp(xp: Int): Int {
            var level = 1
            while (xpForLevel(level + 1) <= xp) level++
            return level
        }

        /** "coins=12;xp=40;owned=cap,crown;equipped=hat:cap" */
        fun encode(w: Wallet): String = listOf(
            "coins=${w.coins.coerceAtLeast(0)}",
            "xp=${w.xp.coerceAtLeast(0)}",
            "owned=${w.owned.filter { ID.matches(it) }.distinct().joinToString(",")}",
            "equipped=${w.equipped.filter { it.key in SLOTS && ID.matches(it.value) }.entries.joinToString(",") { "${it.key}:${it.value}" }}",
        ).joinToString(";")

        fun decode(raw: String): Wallet {
            if (raw.isBlank()) return Wallet()
            val parts = raw.split(';').associate { it.substringBefore('=') to it.substringAfter('=', "") }
            val coins = parts["coins"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0
            val xp = parts["xp"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0
            val owned = parts["owned"].orEmpty().split(',').filter { ID.matches(it) }.distinct()
            val equipped = parts["equipped"].orEmpty().split(',').mapNotNull { pair ->
                val slot = pair.substringBefore(':', "")
                val id = pair.substringAfter(':', "")
                if (slot in SLOTS && ID.matches(id) && id in owned) slot to id else null
            }.toMap()
            return Wallet(coins, maxOf(xp, coins), owned, equipped)
        }
    }
}

/** Daily streak: one challenge a day keeps the flame alive. */
data class Streak(val days: Int = 0, val lastDay: Long = Long.MIN_VALUE) {

    /** The streak to show right now (0 if it has lapsed). */
    fun current(today: Long): Int = if (lastDay == today || lastDay == today - 1) days else 0

    /** What the streak becomes if the child finishes a challenge on [today]. */
    fun ifPlayedOn(today: Long): Int = when (lastDay) {
        today -> days.coerceAtLeast(1)
        today - 1 -> days + 1
        else -> 1
    }

    fun playedOn(today: Long): Streak = Streak(ifPlayedOn(today), today)
}

/** Everything the child has earned, as the app and the games need it. */
data class Rewards(
    val wallet: Wallet = Wallet(),
    val companion: String? = null,
    val streak: Streak = Streak(),
)

data class SoundPrefs(
    val voice: Boolean = true,
    val effects: Boolean = true,
    val music: Boolean = true,
)

/** Island picks per world ("nuzo", "lotli", "embrix"). */
data class Interests(val counts: Map<String, Int> = emptyMap()) {
    val total: Int get() = counts.values.sum()

    fun share(world: String): Float = if (total == 0) 0f else (counts[world] ?: 0).toFloat() / total

    fun plusOne(world: String): Interests = Interests(counts + (world to (counts[world] ?: 0) + 1))

    companion object {
        private val ID = Regex("^[a-z]{1,16}$")

        fun encode(i: Interests): String =
            i.counts.filter { ID.matches(it.key) && it.value > 0 }.entries.sortedBy { it.key }.joinToString(",") { "${it.key}:${it.value}" }

        fun decode(raw: String): Interests = Interests(
            raw.split(',').mapNotNull { part ->
                val k = part.substringBefore(':', "")
                val v = part.substringAfter(':', "").toIntOrNull()
                if (ID.matches(k) && v != null && v > 0) k to v else null
            }.toMap(),
        )
    }
}
