package com.childoo.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

/** How Nuzo feels right now. Drives eyes, mouth and motion. */
enum class NuzoMood { Happy, Thinking, Oops, Cheer }

/**
 * Nuzo, the space-blob companion, drawn in code so it stays crisp at any size and costs no assets.
 * Placeholder until the rigged character ships; the public API (mood) will stay the same.
 */
@Composable
fun Nuzo(
    modifier: Modifier = Modifier,
    mood: NuzoMood = NuzoMood.Happy,
) {
    val motion = rememberInfiniteTransition(label = "nuzo")
    val breathe by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe",
    )
    val blink by motion.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 3800
                0f at 0
                0f at 3480
                1f at 3580
                0f at 3700
            },
        ),
        label = "blink",
    )
    val hop by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "hop",
    )
    val glow by motion.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "glow",
    )

    Canvas(modifier.aspectRatio(0.86f)) {
        val w = size.width
        val h = size.height
        val lift = if (mood == NuzoMood.Cheer) hop * h * 0.08f else breathe * h * 0.015f
        val squash = if (mood == NuzoMood.Cheer) (1f - hop) * 0.05f else breathe * 0.025f

        // Ground shadow shrinks as Nuzo rises.
        val shadowW = w * (0.62f - lift / h)
        drawOval(
            color = Color.Black.copy(alpha = 0.16f),
            topLeft = Offset((w - shadowW) / 2f, h * 0.93f),
            size = Size(shadowW, h * 0.05f),
        )

        val cx = w / 2f
        val bodyBottom = h * 0.92f
        withTransform({
            translate(top = -lift)
            scale(scaleX = 1f + squash, scaleY = 1f - squash, pivot = Offset(cx, bodyBottom))
        }) {
            drawNuzo(w, h, mood, blink, glow)
        }
    }
}

private fun DrawScope.drawNuzo(w: Float, h: Float, mood: NuzoMood, blink: Float, glow: Float) {
    val cx = w / 2f
    val bodyTop = h * 0.24f
    val bodyBottom = h * 0.92f
    val bodyW = w * 0.86f
    val left = cx - bodyW / 2f
    val right = cx + bodyW / 2f

    // Antenna with a glowing mint bulb.
    val stemTop = h * 0.08f
    drawLine(
        color = ChildooColors.GoldDeep,
        start = Offset(cx, bodyTop + h * 0.04f),
        end = Offset(cx, stemTop),
        strokeWidth = w * 0.035f,
        cap = StrokeCap.Round,
    )
    drawCircle(ChildooColors.MintLight.copy(alpha = glow * 0.45f), radius = w * 0.085f, center = Offset(cx, stemTop))
    drawCircle(ChildooColors.MintLight, radius = w * 0.05f, center = Offset(cx, stemTop))
    drawCircle(Color.White.copy(alpha = 0.7f), radius = w * 0.016f, center = Offset(cx - w * 0.015f, stemTop - w * 0.015f))

    // Little feet.
    val footW = w * 0.2f
    val footH = h * 0.07f
    drawOval(ChildooColors.GoldDeep, Offset(cx - w * 0.27f, bodyBottom - footH * 0.7f), Size(footW, footH))
    drawOval(ChildooColors.GoldDeep, Offset(cx + w * 0.07f, bodyBottom - footH * 0.7f), Size(footW, footH))

    // Body: a soft gumdrop shape.
    val body = Path().apply {
        moveTo(cx, bodyTop)
        cubicTo(right - bodyW * 0.08f, bodyTop, right, bodyTop + h * 0.2f, right, h * 0.62f)
        cubicTo(right, bodyBottom - h * 0.02f, cx + bodyW * 0.3f, bodyBottom, cx, bodyBottom)
        cubicTo(cx - bodyW * 0.3f, bodyBottom, left, bodyBottom - h * 0.02f, left, h * 0.62f)
        cubicTo(left, bodyTop + h * 0.2f, left + bodyW * 0.08f, bodyTop, cx, bodyTop)
        close()
    }
    drawPath(
        path = body,
        brush = Brush.radialGradient(
            colors = listOf(ChildooColors.GoldLight, ChildooColors.Gold, ChildooColors.GoldDeep),
            center = Offset(cx - bodyW * 0.18f, h * 0.42f),
            radius = bodyW * 0.85f,
        ),
    )
    // Gloss.
    drawOval(
        color = Color.White.copy(alpha = 0.35f),
        topLeft = Offset(cx - bodyW * 0.32f, bodyTop + h * 0.07f),
        size = Size(bodyW * 0.22f, h * 0.1f),
    )

    // Cheeks.
    val eyeY = h * 0.55f
    val eyeDx = bodyW * 0.19f
    drawOval(ChildooColors.Coral.copy(alpha = 0.35f), Offset(cx - eyeDx - w * 0.13f, eyeY + h * 0.06f), Size(w * 0.12f, h * 0.05f))
    drawOval(ChildooColors.Coral.copy(alpha = 0.35f), Offset(cx + eyeDx + w * 0.01f, eyeY + h * 0.06f), Size(w * 0.12f, h * 0.05f))

    // Eyes.
    val eyeR = w * 0.068f
    val ink = ChildooColors.Navy
    if (mood == NuzoMood.Cheer) {
        // Happy closed arcs: ^ ^
        val arc = Stroke(width = w * 0.032f, cap = StrokeCap.Round)
        listOf(cx - eyeDx, cx + eyeDx).forEach { ex ->
            drawArc(ink, 200f, 140f, false, Offset(ex - eyeR, eyeY - eyeR * 0.6f), Size(eyeR * 2, eyeR * 2), style = arc)
        }
    } else {
        val openness = (1f - blink).coerceIn(0.08f, 1f) * if (mood == NuzoMood.Oops) 0.8f else 1f
        val look = when (mood) {
            NuzoMood.Thinking -> Offset(w * 0.018f, -h * 0.012f)
            else -> Offset.Zero
        }
        listOf(cx - eyeDx, cx + eyeDx).forEach { ex ->
            drawOval(
                color = ink,
                topLeft = Offset(ex - eyeR, eyeY - eyeR * 1.15f * openness),
                size = Size(eyeR * 2f, eyeR * 2.3f * openness),
            )
            if (openness > 0.4f) {
                drawCircle(Color.White, radius = eyeR * 0.36f, center = Offset(ex - eyeR * 0.3f, eyeY - eyeR * 0.45f) + look)
            }
        }
    }

    // Mouth.
    val mouthY = h * 0.69f
    val mouthStroke = Stroke(width = w * 0.03f, cap = StrokeCap.Round)
    when (mood) {
        NuzoMood.Happy -> drawArc(
            ink, 20f, 140f, false,
            Offset(cx - w * 0.09f, mouthY - h * 0.06f), Size(w * 0.18f, h * 0.09f), style = mouthStroke,
        )
        NuzoMood.Cheer -> {
            val mouth = Path().apply {
                moveTo(cx - w * 0.11f, mouthY - h * 0.02f)
                quadraticTo(cx, mouthY + h * 0.11f, cx + w * 0.11f, mouthY - h * 0.02f)
                close()
            }
            drawPath(mouth, ink)
            drawOval(ChildooColors.Coral, Offset(cx - w * 0.045f, mouthY + h * 0.015f), Size(w * 0.09f, h * 0.035f))
        }
        NuzoMood.Oops -> drawCircle(ink, radius = w * 0.035f, center = Offset(cx, mouthY), style = mouthStroke)
        NuzoMood.Thinking -> drawLine(
            ink,
            Offset(cx - w * 0.06f, mouthY + h * 0.005f),
            Offset(cx + w * 0.05f, mouthY - h * 0.012f),
            strokeWidth = w * 0.03f,
            cap = StrokeCap.Round,
        )
    }
}
