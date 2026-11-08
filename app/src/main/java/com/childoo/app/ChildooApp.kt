package com.childoo.app

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.childoo.core.SettingsRepository

/** Root: first run goes through onboarding, afterwards the parent lands on Home. */
@Composable
fun ChildooApp() {
    val context = LocalContext.current
    val repo = remember { SettingsRepository(context) }
    val onboardingDone by produceState<Boolean?>(initialValue = null, repo) {
        repo.onboardingDone.collect { value = it }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Crossfade(targetState = onboardingDone, label = "root") { done ->
            when (done) {
                null -> Unit // DataStore is loading; a blank cream frame for a few milliseconds.
                false -> OnboardingFlow(repo)
                true -> HomeScreen(repo)
            }
        }
    }
}
