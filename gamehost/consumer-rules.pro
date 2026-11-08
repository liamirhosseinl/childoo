# The game engine calls these methods by name from JavaScript.
-keepclassmembers class com.childoo.gamehost.GameBridge {
    @android.webkit.JavascriptInterface <methods>;
}
