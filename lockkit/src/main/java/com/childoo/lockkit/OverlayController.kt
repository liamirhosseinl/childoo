package com.childoo.lockkit

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.KeyEvent
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

/**
 * Shows the challenge as a full-screen window above the locked app.
 * Must be called on the main thread.
 */
internal class OverlayController(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var root: FrameLayout? = null
    private var owner: OverlayLifecycleOwner? = null

    val isShowing: Boolean get() = root != null

    fun show(onSolved: () -> Unit): Boolean {
        if (isShowing) return true
        if (!Settings.canDrawOverlays(context)) return false

        val lifecycleOwner = OverlayLifecycleOwner().also { it.start() }
        val frame = BackBlockingFrame(context)
        frame.setViewTreeLifecycleOwner(lifecycleOwner)
        frame.setViewTreeSavedStateRegistryOwner(lifecycleOwner)

        val compose = ComposeView(context).apply {
            setContent { ChallengeScreen(onSolved = onSolved) }
        }
        frame.addView(compose)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        )

        return try {
            windowManager.addView(frame, params)
            root = frame
            owner = lifecycleOwner
            true
        } catch (e: RuntimeException) {
            // e.g. BadTokenException when the overlay permission was revoked a moment ago.
            lifecycleOwner.stop()
            false
        }
    }

    fun hide() {
        val frame = root ?: return
        try {
            windowManager.removeViewImmediate(frame)
        } catch (_: IllegalArgumentException) {
            // Already detached.
        }
        owner?.stop()
        root = null
        owner = null
    }

    /** Swallows Back so the child can't dismiss the challenge; Home still works and hides it. */
    private class BackBlockingFrame(context: Context) : FrameLayout(context) {
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
        registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}
