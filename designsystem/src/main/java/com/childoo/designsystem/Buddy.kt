package com.childoo.designsystem

import android.graphics.ImageDecoder
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import android.annotation.TargetApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import java.util.concurrent.Executors

/** The three buddies. Only the ones whose animation sets are finished can be played with. */
object Buddies {
    const val DEFAULT = "lotli"
    val ALL = listOf("lotli", "nuzo", "embrix")
    val ACTIVE = setOf("lotli")

    /** The buddy to show: the child's own if it is ready, otherwise the default. */
    fun active(id: String?): String = if (id != null && id in ACTIVE) id else DEFAULT

    fun name(id: String): String = when (id) {
        "nuzo" -> "Nuzo"
        "embrix" -> "Embrix"
        else -> "Lotli"
    }

    fun portrait(id: String): Int = when (id) {
        "nuzo" -> R.drawable.buddy_nuzo
        "embrix" -> R.drawable.buddy_embrix
        else -> R.drawable.buddy_lotli
    }
}

/** Clips shared with the games bundle (assets/games/characters/<buddy>/<clip>.webp). */
enum class BuddyClip(val file: String, val loops: Boolean) {
    Idle("idle", true),
    Hello("hello", false),
    Happy("happy", false),
    Celebrate("celebrate", false),
}

/** Clip canvas, as exported: 400 × 520 with the standing pose in the lower part. */
private const val CLIP_ASPECT = 400f / 520f

private val decodeThread = Executors.newSingleThreadExecutor()

/**
 * The buddy, alive. On Android 9+ it plays the same animated clips the games use; a one-shot clip
 * (hello, happy, …) settles into the idle loop when it ends. Elsewhere, or for a buddy that is not
 * animated yet, it shows the portrait with a gentle breathing motion.
 */
@Composable
fun Buddy(
    modifier: Modifier = Modifier,
    buddy: String = Buddies.DEFAULT,
    clip: BuddyClip = BuddyClip.Idle,
    portraitOnly: Boolean = false,
) {
    val animated = !portraitOnly && buddy in Buddies.ACTIVE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    if (animated) {
        AnimatedBuddy(modifier, buddy, clip)
    } else {
        BuddyPortrait(modifier, buddy)
    }
}

/** The still portrait, breathing softly from its feet. */
@Composable
fun BuddyPortrait(modifier: Modifier = Modifier, buddy: String = Buddies.DEFAULT, breathe: Boolean = true) {
    val t = rememberInfiniteTransition(label = "breathe")
    val b by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b",
    )
    Image(
        painter = painterResource(Buddies.portrait(buddy)),
        contentDescription = Buddies.name(buddy),
        contentScale = ContentScale.Fit,
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer {
                if (breathe) {
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    scaleY = 1f + b * 0.025f
                    scaleX = 1f - b * 0.01f
                }
            },
    )
}

@TargetApi(Build.VERSION_CODES.P)
@Composable
private fun AnimatedBuddy(modifier: Modifier, buddy: String, clip: BuddyClip) {
    val context = LocalContext.current
    val view = remember(buddy) {
        ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = Buddies.name(buddy)
            setImageResource(Buddies.portrait(buddy))
        }
    }
    AndroidView(modifier = modifier.aspectRatio(CLIP_ASPECT), factory = { view })
    DisposableEffect(view, buddy, clip) {
        val iv = view
        var alive = true
        fun show(c: BuddyClip) {
            decodeThread.execute {
                val drawable = runCatching { decode(context.assets, buddy, c) }.getOrNull() ?: return@execute
                iv.post {
                    if (!alive) return@post
                    (iv.drawable as? AnimatedImageDrawable)?.stop()
                    iv.setImageDrawable(drawable)
                    if (drawable is AnimatedImageDrawable) {
                        drawable.repeatCount = if (c.loops) AnimatedImageDrawable.REPEAT_INFINITE else 0
                        if (!c.loops) {
                            drawable.registerAnimationCallback(object : Animatable2.AnimationCallback() {
                                override fun onAnimationEnd(d: Drawable?) {
                                    if (alive) show(BuddyClip.Idle)
                                }
                            })
                        }
                        drawable.start()
                    }
                }
            }
        }
        show(clip)
        onDispose {
            alive = false
            (iv.drawable as? AnimatedImageDrawable)?.stop()
        }
    }
}

@TargetApi(Build.VERSION_CODES.P)
private fun decode(assets: android.content.res.AssetManager, buddy: String, clip: BuddyClip): Drawable =
    ImageDecoder.decodeDrawable(ImageDecoder.createSource(assets, "games/characters/$buddy/${clip.file}.webp"))
