package com.childoo.gamehost

import com.childoo.core.Wallet
import org.json.JSONArray
import org.json.JSONObject

/** Everything the game engine needs to plan a session. Mirrors HostConfig in games/src/engine/types.ts. */
data class GameConfig(
    val age: Int,
    val childName: String = "",
    val companion: String? = null,
    val levels: Map<String, Int> = emptyMap(),
    val recent: List<String> = emptyList(),
    val wallet: Wallet = Wallet(),
    /** Streak length if today's challenge is finished. */
    val streakDays: Int = 1,
    val voice: Boolean = true,
    val sound: Boolean = true,
    val music: Boolean = true,
    val mode: Mode = Mode.Challenge,
    val forceGame: String? = null,
) {
    enum class Mode(val wire: String) { Challenge("challenge"), Room("room") }

    fun toJson(): String = JSONObject().apply {
        put("mode", mode.wire)
        put("age", age)
        if (childName.isNotBlank()) put("childName", childName)
        companion?.let { put("companion", it) }
        put("levels", JSONObject(levels))
        put("recent", JSONArray(recent))
        put("wallet", walletJson(wallet))
        put("streakDays", streakDays)
        put("voice", voice)
        put("sound", sound)
        put("music", music)
        put("seed", System.currentTimeMillis() and 0x7fffffffL)
        forceGame?.let { put("forceGame", it) }
    }.toString()

    private fun walletJson(w: Wallet) = JSONObject().apply {
        put("coins", w.coins)
        put("xp", w.xp)
        put("owned", JSONArray(w.owned))
        put("equipped", JSONObject(w.equipped))
    }
}

/** What happened in one session (a game, or a visit to the Kid's Corner). Mirrors SessionResult. */
data class GameResult(
    val mode: String,
    val gameId: String,
    val skill: String,
    val world: String,
    val level: Int,
    val newLevel: Int,
    val rounds: Int,
    val firstTry: Int,
    val wrong: Int,
    val guessing: Boolean,
    val durationMs: Long,
    val coinsEarned: Int,
    val companion: String?,
    val wallet: Wallet?,
) {
    val isRoom: Boolean get() = mode == "room"

    companion object {
        /** Returns null for anything malformed; the caller still unlocks play. */
        fun parse(json: String): GameResult? = try {
            val o = JSONObject(json)
            GameResult(
                mode = o.optString("mode", "challenge"),
                gameId = o.getString("gameId"),
                skill = o.optString("skill"),
                world = o.optString("world"),
                level = o.optInt("level", 1),
                newLevel = o.optInt("newLevel", o.optInt("level", 1)),
                rounds = o.optInt("rounds"),
                firstTry = o.optInt("firstTry"),
                wrong = o.optInt("wrong"),
                guessing = o.optBoolean("guessing"),
                durationMs = o.optLong("durationMs"),
                coinsEarned = o.optInt("coinsEarned"),
                companion = o.optString("companion").takeIf { it.isNotBlank() && it != "null" },
                wallet = o.optJSONObject("wallet")?.let(::parseWallet),
            )
        } catch (_: Exception) {
            null
        }

        private fun parseWallet(o: JSONObject): Wallet {
            val owned = o.optJSONArray("owned")?.let { arr -> List(arr.length()) { arr.optString(it) } }.orEmpty()
            val equipped = o.optJSONObject("equipped")
                ?.let { eq -> eq.keys().asSequence().associateWith { key -> eq.optString(key) } }
                .orEmpty()
            // Round-trip through the codec so anything odd is cleaned the same way as stored data.
            return Wallet.decode(Wallet.encode(Wallet(o.optInt("coins"), o.optInt("xp"), owned, equipped)))
        }
    }
}
