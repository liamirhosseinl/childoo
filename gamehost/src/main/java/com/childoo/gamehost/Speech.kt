package com.childoo.gamehost

import android.content.Context
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Reads prompts aloud so children who can't read yet can still play. English for now. */
internal class Speech(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private var pending: String? = null

    override fun onInit(status: Int) {
        val engine = tts ?: return
        if (status != TextToSpeech.SUCCESS) return
        val lang = engine.setLanguage(Locale.US)
        ready = lang != TextToSpeech.LANG_MISSING_DATA && lang != TextToSpeech.LANG_NOT_SUPPORTED
        engine.setSpeechRate(0.92f)
        engine.setPitch(1.12f)
        pending?.let { speak(it) }
        pending = null
    }

    fun speak(text: String) {
        val engine = tts ?: return
        if (!ready) {
            pending = text
            return
        }
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "childoo-${SystemClock.uptimeMillis()}")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
