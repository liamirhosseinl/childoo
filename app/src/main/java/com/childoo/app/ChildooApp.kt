package com.childoo.app

import android.os.SystemClock
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.childoo.core.SettingsRepository

private enum class Root { Loading, Onboarding, Gate, Home }

/**
 * Root: first run goes through onboarding; afterwards the parent PIN guards the dashboard.
 * The gate closes again when the app has been in the background for more than a minute,
 * so a quick trip to system Settings doesn't ask for the PIN again.
 */
@Composable
fun ChildooApp() {
    val context = LocalContext.current
    val repo = remember { SettingsRepository(context) }
    val onboardingDone by produceState<Boolean?>(initialValue = null, repo) {
        repo.onboardingDone.collect { value = it }
    }
    val hasPin by produceState<Boolean?>(initialValue = null, repo) {
        repo.hasPin.collect { value = it }
    }
    var unlocked by rememberSaveable { mutableStateOf(false) }
    var stoppedAt by remember { mutableLongStateOf(0L) }

    // The parent who just finished setup is already "in"; don't ask for the PIN they just made.
    LaunchedEffect(onboardingDone) {
        if (onboardingDone == false) unlocked = true
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { stoppedAt = SystemClock.elapsedRealtime() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        if (stoppedAt != 0L && SystemClock.elapsedRealtime() - stoppedAt > RELOCK_AFTER_MS) unlocked = false
    }

    val root = when {
        onboardingDone == null || hasPin == null -> Root.Loading
        onboardingDone == false -> Root.Onboarding
        hasPin == true && !unlocked -> Root.Gate
        else -> Root.Home
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Crossfade(targetState = root, label = "root") { screen ->
            when (screen) {
                Root.Loading -> Unit
                Root.Onboarding -> OnboardingFlow(repo)
                Root.Gate -> ParentGate(repo, onUnlocked = { unlocked = true })
                Root.Home -> HomeScreen(repo)
            }
        }
    }
}

private const val RELOCK_AFTER_MS = 60_000L
