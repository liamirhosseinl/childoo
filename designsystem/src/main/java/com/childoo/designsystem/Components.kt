package com.childoo.designsystem

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Chunky "3D" button with a darker lip underneath that presses down on touch.
 * The same language is used for the child's answer tiles, so parent and child UI feel like one product.
 */
@Composable
fun ChunkyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    face: Color = ChildooColors.Gold,
    lip: Color = ChildooColors.GoldDeep,
    contentColor: Color = ChildooColors.Navy,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 58.dp,
) {
    ChunkySurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(height),
        face = face,
        lip = lip,
        enabled = enabled,
        shape = RoundedCornerShape(height / 2),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize), color = contentColor)
        }
    }
}

/** Generic pressable surface with a lip. Content is centred on the face. */
@Composable
fun ChunkySurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    face: Color,
    lip: Color,
    enabled: Boolean = true,
    shape: RoundedCornerShape = RoundedCornerShape(22.dp),
    lipHeight: Dp = 5.dp,
    dimWhenDisabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val drop by animateDpAsState(if (pressed && enabled) lipHeight else 0.dp, label = "press")

    Box(
        modifier
            .alpha(if (enabled || !dimWhenDisabled) 1f else 0.4f)
            .clip(shape)
            .background(lip, shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        Box(
            Modifier
                .matchParentSizeWithLip(lipHeight, drop)
                .background(face, shape),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}

private fun Modifier.matchParentSizeWithLip(lip: Dp, drop: Dp): Modifier =
    this
        .fillMaxSize()
        .padding(top = drop, bottom = lip - drop)

/** Quiet secondary action: outlined pill. */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = ChildooColors.Navy,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(CircleShape)
            .border(1.5.dp, color.copy(alpha = 0.18f), CircleShape)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

/** Round icon badge used in lists and cards. */
@Composable
fun IconBadge(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    Box(
        modifier
            .size(size)
            .background(tint.copy(alpha = 0.14f), RoundedCornerShape(size * 0.36f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.52f))
    }
}

/** Small rounded label, e.g. "Recommended". */
@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier, filled: Boolean = false) {
    Box(
        modifier
            .clip(CircleShape)
            .background(if (filled) color else color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = if (filled) Color.White else color,
        )
    }
}
