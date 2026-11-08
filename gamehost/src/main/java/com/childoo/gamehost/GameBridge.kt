package com.childoo.gamehost

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface

/**
 * Exposed to the game page as `window.ChildooHost`. Methods run on a WebView binder thread,
 * so everything is handed to the main thread before touching views or state.
 */
class GameBridge(
    private val configJson: String,
    private val listener: Listener,
) {
    interface Listener {
        fun onComplete(resultJson: String)
        fun onSpeak(text: String)
        fun onStopSpeech()
        fun onHaptic(kind: String)
    }

    private val main = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun getConfig(): String = configJson

    @JavascriptInterface
    fun complete(resultJson: String) {
        main.post { listener.onComplete(resultJson) }
    }

    @JavascriptInterface
    fun speak(text: String) {
        val clean = text.take(MAX_SPEECH)
        main.post { listener.onSpeak(clean) }
    }

    @JavascriptInterface
    fun stopSpeech() {
        main.post { listener.onStopSpeech() }
    }

    @JavascriptInterface
    fun haptic(kind: String) {
        main.post { listener.onHaptic(kind) }
    }

    private companion object {
        const val MAX_SPEECH = 300
    }
}
