package com.childoo.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.childoo.core.SettingsRepository
import com.childoo.gamehost.GameConfig
import com.childoo.gamehost.GameView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * The child's own space: see the buddy, spend coins, dress up. Opens without the parent PIN
 * because nothing here changes protection. Runs the same web engine as the challenges.
 */
@Composable
fun KidCorner(repo: SettingsRepository, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val config by produceState<GameConfig?>(initialValue = null, repo) {
        val child = repo.child.first()
        val rewards = repo.rewards.first()
        val sound = repo.soundPrefs.first()
        value = GameConfig(
            age = child.age,
            childName = child.name,
            companion = rewards.companion,
            wallet = rewards.wallet,
            streakDays = rewards.streak.current(LocalDate.now().toEpochDay()),
            voice = false,
            sound = sound.effects,
            music = sound.music,
            mode = GameConfig.Mode.Room,
        )
    }
    var view by remember { mutableStateOf<GameView?>(null) }

    // Back saves and closes, the same as the Done button.
    BackHandler { view?.requestFinish() ?: onClose() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF2A1F5C)),
    ) {
        val cfg = config ?: return@Box
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                GameView(
                    context = ctx,
                    config = cfg,
                    onFinished = { result ->
                        scope.launch {
                            val wallet = result?.wallet
                            if (result != null && result.isRoom && wallet != null) {
                                repo.recordRoom(wallet, result.companion)
                            }
                            onClose()
                        }
                    },
                    onCrashed = onClose,
                ).also {
                    view = it
                    it.start()
                }
            },
        )
    }
    DisposableEffect(Unit) {
        onDispose {
            view?.release()
            view = null
        }
    }
}
