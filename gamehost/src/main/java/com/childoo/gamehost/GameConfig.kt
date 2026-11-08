package com.childoo.gamehost

import org.json.JSONArray
import org.json.JSONObject

/** Everything the game engine needs to plan a session. Mirrors HostConfig in games/src/engine/types.ts. */
data class GameConfig(
    val age: Int,
    val childName: String = "",
    val companion: String? = null,
    val levels: Map<String, Int> = emptyMap(),
    val recent: List<String> = emptyList(),
    val voice: Boolean = true,
    val sound: Boolean = true,
    val forceGame: String? = null,
) {
    fun toJson(): String = JSONObject().apply {
        put("age", age)
        if (childName.isNotBlank()) put("childName", childName)
        companion?.let { put("companion", it) }
        put("levels", JSONObject(levels))
        put("recent", JSONArray(recent))
        put("voice", voice)
        put("sound", sound)
        put("seed", System.currentTimeMillis() and 0x7fffffffL)
        forceGame?.let { put("forceGame", it) }
    }.toString()
}

/** What happened in one game session. Mirrors SessionResult in the engine. */
data class GameResult(
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
) {
    companion object {
        /** Returns null for anything malformed; the caller still unlocks play. */
        fun parse(json: String): GameResult? = try {
            val o = JSONObject(json)
            GameResult(
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
            )
        } catch (_: Exception) {
            null
        }
    }
}
