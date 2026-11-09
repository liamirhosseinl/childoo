package com.childoo.lockkit

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.childoo.core.LockSettings
import com.childoo.core.PlayTimeAccumulator
import com.childoo.core.SettingsRepository
import com.childoo.gamehost.GameConfig
import com.childoo.gamehost.GameResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Foreground service that measures time spent in the parent's target apps and puts the
 * challenge on top once the interval is used up. Everything runs on the main thread:
 * one cheap poll per second, and WindowManager needs the main thread anyway.
 */
class UsageMonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var repo: SettingsRepository
    private lateinit var detector: ForegroundAppDetector
    private lateinit var overlay: OverlayController
    private lateinit var power: PowerManager

    private var settings = LockSettings()
    private var accumulator = PlayTimeAccumulator()
    private var loop: Job? = null

    /** True once the interval is used up; stays true until the child solves the challenge. */
    private var challengeDue = false
    private var testMode = false

    /** True while the latest settings are being read for a challenge that is about to open. */
    private var opening = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        repo = SettingsRepository(this)
        detector = ForegroundAppDetector(this)
        overlay = OverlayController(this)
        power = getSystemService(Context.POWER_SERVICE) as PowerManager
        startInForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Must be called promptly after startForegroundService(), including on restarts.
        startInForeground()
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelfCleanly()
                return START_NOT_STICKY
            }
            ACTION_TEST_CHALLENGE -> {
                testMode = true
                showChallenge()
            }
        }
        if (loop == null) loop = scope.launch { runLoop() }
        return START_STICKY
    }

    private suspend fun runLoop() {
        accumulator = PlayTimeAccumulator(initialPlayedMs = repo.playedSinceChallengeMs.first())
        scope.launch { repo.settings.collect { settings = it } }
        settings = repo.settings.first()

        var lastTick = SystemClock.elapsedRealtime()
        var lastSave = lastTick
        while (scope.isActive) {
            delay(TICK_MS)
            val now = SystemClock.elapsedRealtime()
            val elapsed = now - lastTick
            lastTick = now

            if (!settings.protectionEnabled && !testMode) {
                stopSelfCleanly()
                return
            }

            val snapshot = if (power.isInteractive) detector.poll() else null
            val front = snapshot?.front
            // A target counts while it is anywhere on screen, including picture-in-picture and split screen.
            val inTarget = snapshot != null && snapshot.visible.any { it in settings.targetPackages }

            if (!challengeDue) {
                accumulator.tick(elapsed, inTarget)
                if (accumulator.isChallengeDue(settings.intervalMinutes)) challengeDue = true
            }

            when {
                (challengeDue || testMode) && inTarget -> showChallenge()
                // Child left the target app (home, another app): get out of the way.
                // The challenge comes back the moment they reopen the target app.
                overlay.isShowing && !testMode && snapshot != null && !inTarget && front != packageName -> overlay.hide()
            }

            if (now - lastSave >= SAVE_EVERY_MS) {
                lastSave = now
                repo.setPlayedSinceChallengeMs(accumulator.playedMs)
            }
        }
    }

    /**
     * Reads the child's latest profile, buddy, coins and sound settings every time, so a preview
     * started before the monitor loop is running still knows the chosen buddy and settings.
     */
    private fun showChallenge() {
        if (overlay.isShowing || opening) return
        opening = true
        scope.launch {
            try {
                val child = repo.child.first()
                val progress = repo.gameProgress.first()
                val rewards = repo.rewards.first()
                val sound = repo.soundPrefs.first()
                val today = LocalDate.now().toEpochDay()
                val config = GameConfig(
                    age = child.age,
                    childName = child.name,
                    companion = rewards.companion,
                    levels = progress.levels,
                    recent = progress.recent,
                    wallet = rewards.wallet,
                    streakDays = rewards.streak.ifPlayedOn(today),
                    // Read-aloud is off until recorded voices exist for each language.
                    voice = false,
                    sound = sound.effects,
                    music = sound.music,
                )
                overlay.show(config, onDone = ::onChallengeSolved)
            } finally {
                opening = false
            }
        }
    }

    private fun onChallengeSolved(result: GameResult?) {
        overlay.hide()
        val wasTest = testMode
        testMode = false
        if (wasTest) {
            // A preview earns nothing, but a buddy picked during it is kept.
            val buddy = result?.companion
            if (buddy != null) scope.launch { repo.setCompanion(buddy) }
            return
        }
        challengeDue = false
        accumulator.reset()
        scope.launch {
            if (result != null) {
                repo.recordGame(result.gameId, result.newLevel, result.wallet, result.companion, result.chosenWorld)
            } else {
                repo.recordChallengeSolved()
            }
        }
    }

    private fun stopSelfCleanly() {
        overlay.hide()
        loop?.cancel()
        loop = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        isRunning = false
        overlay.hide()
        val played = accumulator.playedMs
        // Best effort final save; the scope is cancelled right after.
        CoroutineScope(Dispatchers.IO).launch { repo.setPlayedSinceChallengeMs(played) }
        scope.cancel()
        super.onDestroy()
    }

    private fun startInForeground() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), type)
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.lockkit_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { setShowBadge(false) }
            manager.createNotificationChannel(channel)
        }
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        val content = launch?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.lockkit_ic_shield)
            .setContentTitle(getString(R.string.lockkit_notification_title))
            .setContentText(getString(R.string.lockkit_notification_text))
            .setContentIntent(content)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        /** True while this process has the service alive; read by the watchdog. */
        @Volatile
        var isRunning: Boolean = false
            private set

        const val ACTION_START = "com.childoo.lockkit.START"
        const val ACTION_STOP = "com.childoo.lockkit.STOP"
        const val ACTION_TEST_CHALLENGE = "com.childoo.lockkit.TEST_CHALLENGE"

        private const val CHANNEL_ID = "childoo_protection"
        private const val NOTIFICATION_ID = 4201
        private const val TICK_MS = 1_000L
        private const val SAVE_EVERY_MS = 15_000L
    }
}
