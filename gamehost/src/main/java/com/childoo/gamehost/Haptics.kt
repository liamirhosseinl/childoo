package com.childoo.gamehost

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Short, gentle taps. Never long buzzes: they feel like punishment to a child. */
internal class Haptics(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun play(kind: String) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val effect = when (kind) {
            "success" -> VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
            "error" -> VibrationEffect.createWaveform(longArrayOf(0, 25, 50, 25), -1)
            else -> VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        try {
            v.vibrate(effect)
        } catch (_: SecurityException) {
            // Vibration is a nicety; never crash for it.
        }
    }
}
