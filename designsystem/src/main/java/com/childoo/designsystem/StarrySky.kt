package com.childoo.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

private class Star(val x: Float, val y: Float, val r: Float, val phase: Float, val sparkle: Boolean)

/** Deep-space backdrop with gently twinkling stars. Used behind the child-facing screens. */
@Composable
fun StarrySky(
    modifier: Modifier = Modifier,
    starCount: Int = 46,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val stars = remember(starCount) {
        val rnd = Random(7)
        List(starCount) {
            Star(
                x = rnd.nextFloat(),
                y = rnd.nextFloat(),
                r = 0.8f + rnd.nextFloat() * 2.2f,
                phase = rnd.nextFloat(),
                sparkle = rnd.nextFloat() < 0.12f,
            )
        }
    }
    val time by rememberInfiniteTransition(label = "sky").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
        label = "twinkle",
    )

    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(ChildooColors.NavyDeep, ChildooColors.Navy, Color(0xFF3A2F8F)),
                ),
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // Soft nebula glows.
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(ChildooColors.Lilac.copy(alpha = 0.28f), Color.Transparent),
                    center = Offset(size.width * 0.9f, size.height * 0.18f),
                    radius = size.width * 0.7f,
                ),
                radius = size.width * 0.7f,
                center = Offset(size.width * 0.9f, size.height * 0.18f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(ChildooColors.MintLight.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width * 0.05f, size.height * 0.8f),
                    radius = size.width * 0.8f,
                ),
                radius = size.width * 0.8f,
                center = Offset(size.width * 0.05f, size.height * 0.8f),
            )
            stars.forEach { s ->
                val a = 0.25f + 0.75f * abs(sin(((time + s.phase) * 2f * PI).toFloat()))
                val c = Offset(s.x * size.width, s.y * size.height)
                val r = s.r * density / 1.6f
                drawCircle(Color.White.copy(alpha = a), radius = r, center = c)
                if (s.sparkle) {
                    val len = r * 4f * a
                    drawLine(Color.White.copy(alpha = a * 0.8f), c - Offset(len, 0f), c + Offset(len, 0f), strokeWidth = r * 0.6f)
                    drawLine(Color.White.copy(alpha = a * 0.8f), c - Offset(0f, len), c + Offset(0f, len), strokeWidth = r * 0.6f)
                }
            }
        }
        content()
    }
}
