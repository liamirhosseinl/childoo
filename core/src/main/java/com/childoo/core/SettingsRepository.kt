package com.childoo.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** One DataStore file per process; the delegate guarantees a single instance. */
private val Context.childooDataStore: DataStore<Preferences> by preferencesDataStore(name = "childoo_settings")

/**
 * The house rule a parent configures.
 * @param targetPackages apps that cost screen time (YouTube, games...).
 * @param intervalMinutes minutes of play in target apps before a challenge appears.
 * @param protectionEnabled whether the monitor service should be running.
 */
data class LockSettings(
    val targetPackages: Set<String> = emptySet(),
    val intervalMinutes: Int = SettingsRepository.DEFAULT_INTERVAL_MINUTES,
    val protectionEnabled: Boolean = false,
)

class SettingsRepository(context: Context) {

    private val store = context.applicationContext.childooDataStore

    private object Keys {
        val TARGETS = stringSetPreferencesKey("target_packages")
        val INTERVAL = intPreferencesKey("interval_minutes")
        val ENABLED = booleanPreferencesKey("protection_enabled")
        val PLAYED_MS = longPreferencesKey("played_since_challenge_ms")
        val CHALLENGES_SOLVED = intPreferencesKey("challenges_solved_total")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }

    val settings: Flow<LockSettings> = store.data.map { p ->
        LockSettings(
            targetPackages = p[Keys.TARGETS] ?: emptySet(),
            intervalMinutes = p[Keys.INTERVAL] ?: DEFAULT_INTERVAL_MINUTES,
            protectionEnabled = p[Keys.ENABLED] ?: false,
        )
    }

    /** Milliseconds played in target apps since the last solved challenge. */
    val playedSinceChallengeMs: Flow<Long> = store.data.map { it[Keys.PLAYED_MS] ?: 0L }

    val challengesSolved: Flow<Int> = store.data.map { it[Keys.CHALLENGES_SOLVED] ?: 0 }

    /** Whether the parent finished the first-run setup. */
    val onboardingDone: Flow<Boolean> = store.data.map { it[Keys.ONBOARDING_DONE] ?: false }

    suspend fun setOnboardingDone(done: Boolean) {
        store.edit { it[Keys.ONBOARDING_DONE] = done }
    }

    suspend fun setTargets(packages: Set<String>) {
        store.edit { it[Keys.TARGETS] = packages }
    }

    suspend fun toggleTarget(packageName: String) {
        store.edit { p ->
            val current = p[Keys.TARGETS] ?: emptySet()
            p[Keys.TARGETS] = if (packageName in current) current - packageName else current + packageName
        }
    }

    suspend fun setIntervalMinutes(minutes: Int) {
        require(minutes > 0) { "Interval must be positive" }
        store.edit { it[Keys.INTERVAL] = minutes }
    }

    suspend fun setProtectionEnabled(enabled: Boolean) {
        store.edit { it[Keys.ENABLED] = enabled }
    }

    suspend fun setPlayedSinceChallengeMs(ms: Long) {
        store.edit { it[Keys.PLAYED_MS] = ms.coerceAtLeast(0L) }
    }

    suspend fun recordChallengeSolved() {
        store.edit { p ->
            p[Keys.PLAYED_MS] = 0L
            p[Keys.CHALLENGES_SOLVED] = (p[Keys.CHALLENGES_SOLVED] ?: 0) + 1
        }
    }

    companion object {
        /** Product default: one 60-second challenge every 30 minutes of play. */
        const val DEFAULT_INTERVAL_MINUTES = 30
        val INTERVAL_OPTIONS = listOf(1, 5, 15, 30)
    }
}
