package com.childoo.challenge

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.childoo.designsystem.ChildooColors
import kotlin.math.sin
import kotlin.random.Random

private class Piece(
    val x: Float,
    val delay: Float,
    val speed: Float,
    val sway: Float,
    val spin: Float,
    val w: Float,
    val color: Color,
)

/** One burst of falling confetti; plays once when it enters the composition. */
@Composable
internal fun Confetti(modifier: Modifier = Modifier, count: Int = 90) {
    val palette = listOf(
        ChildooColors.Gold, ChildooColors.Coral, ChildooColors.MintLight,
        ChildooColors.Sky, ChildooColors.Lilac, Color.White,
    )
    val pieces = remember {
        val rnd = Random(System.nanoTime())
        List(count) {
            Piece(
                x = rnd.nextFloat(),
                delay = rnd.nextFloat() * 0.35f,
                speed = 0.7f + rnd.nextFloat() * 0.6f,
                sway = 10f + rnd.nextFloat() * 30f,
                spin = 180f + rnd.nextFloat() * 540f,
                w = 6f + rnd.nextFloat() * 8f,
                color = palette[rnd.nextInt(palette.size)],
            )
        }
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(2600, easing = LinearEasing))
    }

    Canvas(modifier) {
        val p = progress.value
        pieces.forEach { piece ->
            val t = ((p - piece.delay) / (1f - piece.delay)).coerceIn(0f, 1f)
            if (t <= 0f) return@forEach
            val y = -40f + t * piece.speed * (size.height + 80f)
            val x = piece.x * size.width + sin(t * 9f + piece.x * 6f) * piece.sway * density
            val alpha = if (t > 0.8f) (1f - t) / 0.2f else 1f
            val pw = piece.w * density / 2f
            rotate(degrees = t * piece.spin, pivot = Offset(x, y)) {
                drawRect(
                    color = piece.color.copy(alpha = alpha),
                    topLeft = Offset(x - pw / 2f, y - pw),
                    size = Size(pw, pw * 2f),
                )
            }
        }
    }
}
