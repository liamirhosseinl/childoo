package com.childoo.gamehost

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.webkit.WebViewAssetLoader

/**
 * Hosts the bundled web game engine (assets/games) offline.
 *
 * The page is served from the app's own assets through WebViewAssetLoader, so it runs on an https
 * origin with no network access and can never navigate anywhere else.
 */
@SuppressLint("ViewConstructor")
class GameView(
    context: Context,
    config: GameConfig,
    private val onFinished: (GameResult?) -> Unit,
    private val onCrashed: () -> Unit,
) : FrameLayout(context) {

    private val speech = Speech(context)
    private val haptics = Haptics(context)
    private val webView: WebView
    private var finished = false

    init {
        setBackgroundColor(Color.parseColor("#12154A"))
        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()

        webView = createWebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url ?: return null
                return loader.shouldInterceptRequest(url)
            }

            // The game never leaves its own page.
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = true

            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                post { if (!finished) onCrashed() }
                return true
            }
        }
        webView.addJavascriptInterface(
            GameBridge(
                configJson = config.toJson(),
                listener = object : GameBridge.Listener {
                    override fun onComplete(resultJson: String) {
                        if (finished) return
                        finished = true
                        speech.stop()
                        onFinished(GameResult.parse(resultJson))
                    }

                    override fun onSpeak(text: String) = speech.speak(text)
                    override fun onStopSpeech() = speech.stop()
                    override fun onHaptic(kind: String) = haptics.play(kind)
                },
            ),
            BRIDGE_NAME,
        )
        addView(webView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun start() {
        webView.loadUrl(START_URL)
    }

    /** Asks the page to finish politely (the Kid's Corner saves and closes), e.g. on Back. */
    fun requestFinish() {
        webView.evaluateJavascript("document.querySelector('.done-btn')?.click()", null)
    }

    /** Must be called when the view is removed, or the WebView and speech engine leak. */
    fun release() {
        speech.shutdown()
        webView.stopLoading()
        webView.removeJavascriptInterface(BRIDGE_NAME)
        removeView(webView)
        webView.destroy()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(context: Context): WebView = WebView(context).apply {
        setBackgroundColor(Color.parseColor("#12154A"))
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        overScrollMode = OVER_SCROLL_NEVER
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            allowContentAccess = false
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            textZoom = 100
        }
        val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        WebView.setWebContentsDebuggingEnabled(debuggable)
    }

    companion object {
        private const val BRIDGE_NAME = "ChildooHost"
        private const val START_URL = "https://appassets.androidplatform.net/assets/games/index.html"
    }
}
