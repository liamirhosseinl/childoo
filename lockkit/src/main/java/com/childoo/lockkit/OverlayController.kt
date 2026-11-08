package com.childoo.lockkit

import android.content.Context
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.childoo.challenge.ChallengeScreen
import com.childoo.gamehost.GameConfig
import com.childoo.gamehost.GameResult
import com.childoo.gamehost.GameView

/**
 * Shows the challenge as a full-screen window above the locked app.
 *
 * The game engine (WebView) is the normal path; if the device has no working WebView the
 * native math challenge is used instead, so the child is never stuck. While the window is up,
 * Childoo holds audio focus so the video behind it pauses. Must be called on the main thread.
 */
internal class OverlayController(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var root: FrameLayout? = null
    private var game: GameView? = null
    private var owner: OverlayLifecycleOwner? = null
    private var focusRequest: AudioFocusRequest? = null
    private var onDone: ((GameResult?) -> Unit)? = null

    val isShowing: Boolean get() = root != null

    fun show(config: GameConfig, onDone: (GameResult?) -> Unit): Boolean {
        if (isShowing) return true
        if (!Settings.canDrawOverlays(context)) return false
        this.onDone = onDone

        val frame = BackBlockingFrame(context)
        val gameView = try {
            GameView(
                context = context,
                config = config,
                onFinished = { result -> finish(result) },
                onCrashed = { swapToNative(frame) },
            )
        } catch (e: RuntimeException) {
            // No WebView provider installed or it is mid-update: fall back to the native challenge.
            null
        }
        if (gameView != null) {
            frame.addView(gameView)
            game = gameView
        } else {
            attachNative(frame)
        }

        return try {
            windowManager.addView(frame, layoutParams())
            root = frame
            gameView?.start()
            pauseOtherAudio()
            true
        } catch (e: RuntimeException) {
            // e.g. BadTokenException when the overlay permission was revoked a moment ago.
            cleanUp()
            false
        }
    }

    fun hide() {
        val frame = root ?: run {
            cleanUp()
            return
        }
        try {
            windowManager.removeViewImmediate(frame)
        } catch (_: IllegalArgumentException) {
            // Already detached.
        }
        cleanUp()
    }

    private fun finish(result: GameResult?) {
        val callback = onDone
        onDone = null
        callback?.invoke(result)
    }

    private fun swapToNative(frame: FrameLayout) {
        game?.let {
            frame.removeView(it)
            it.release()
        }
        game = null
        attachNative(frame)
    }

    private fun attachNative(frame: FrameLayout) {
        val lifecycleOwner = OverlayLifecycleOwner().also { it.start() }
        frame.setViewTreeLifecycleOwner(lifecycleOwner)
        frame.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        owner = lifecycleOwner
        frame.addView(
            ComposeView(context).apply {
                setContent { ChallengeScreen(onSolved = { finish(null) }) }
            },
        )
    }

    private fun cleanUp() {
        game?.release()
        game = null
        owner?.stop()
        owner = null
        root = null
        onDone = null
        releaseAudio()
    }

    /** Transient focus makes video and music apps pause, and resume by themselves when we let go. */
    private fun pauseOtherAudio() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setWillPauseWhenDucked(false)
            .setOnAudioFocusChangeListener { }
            .build()
        audio.requestAudioFocus(request)
        focusRequest = request
    }

    private fun releaseAudio() {
        focusRequest?.let { audio.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    private fun layoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.OPAQUE,
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    /** Swallows Back so the child can't dismiss the challenge; Home still works and hides it. */
    private class BackBlockingFrame(context: Context) : FrameLayout(context) {
        init {
            isFocusable = true
            isFocusableInTouchMode = true
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        override fun dispatchKeyEvent(event: KeyEvent): Boolean =
            if (event.keyCode == KeyEvent.KEYCODE_BACK) true else super.dispatchKeyEvent(event)
    }
}

/** Minimal lifecycle so Compose can run inside a window that has no Activity. */
private class OverlayLifecycleOwner : SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    fun start() {
        savedStateController.performRestore(null)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun stop() {
        if (registry.currentState == Lifecycle.State.DESTROYED) return
        registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}
