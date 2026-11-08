package com.childoo.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Brand palette. Names describe the colour, not the use, so they stay valid as the UI grows. */
object ChildooColors {
    val Navy = Color(0xFF1E2266)
    val NavyDeep = Color(0xFF12154A)
    val NavySoft = Color(0xFF2C3190)
    val Gold = Color(0xFFFFC145)
    val GoldLight = Color(0xFFFFDB85)
    val GoldDeep = Color(0xFFE8A20F)
    val Cream = Color(0xFFFBF8F3)
    val Sand = Color(0xFFF2ECE2)
    val Mint = Color(0xFF17917D)
    val MintLight = Color(0xFF2FC4A6)
    val Coral = Color(0xFFFF7A59)
    val CoralDeep = Color(0xFFD9553A)
    val Sky = Color(0xFF4DA8FF)
    val SkyDeep = Color(0xFF2A7FD6)
    val Lilac = Color(0xFF9B7BFF)
    val LilacDeep = Color(0xFF7458E0)
    val Ink = Color(0xFF1B1D3A)
    val InkSoft = Color(0xFF5D6185)
    val Line = Color(0xFFE6E0D6)

    // Character palettes, matched to the character sheets.
    val NuzoLight = Color(0xFFD4EEFF)
    val NuzoBlue = Color(0xFF8CC8F5)
    val NuzoDeep = Color(0xFF5C9FE0)
    val NuzoHorn = Color(0xFFB28CF5)
}

/** Rounded display face for headings, numbers and anything a child reads. */
val Fredoka = FontFamily(
    Font(R.font.fredoka_medium, FontWeight.Medium),
    Font(R.font.fredoka_semibold, FontWeight.SemiBold),
    Font(R.font.fredoka_bold, FontWeight.Bold),
    Font(R.font.fredoka_bold, FontWeight.ExtraBold),
)

/** Friendly, very legible body face for parent-facing text. */
val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.Medium),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
)

private fun TextStyle.brand(weight: FontWeight, size: Int? = null, line: Int? = null, family: FontFamily = Nunito) = copy(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size?.sp ?: fontSize,
    lineHeight = line?.sp ?: lineHeight,
    letterSpacing = 0.sp,
)

private val Base = Typography()

val ChildooTypography = Typography(
    displayLarge = Base.displayLarge.brand(FontWeight.Bold, family = Fredoka),
    displayMedium = Base.displayMedium.brand(FontWeight.Bold, family = Fredoka),
    displaySmall = Base.displaySmall.brand(FontWeight.Bold, 34, 42, Fredoka),
    headlineLarge = Base.headlineLarge.brand(FontWeight.SemiBold, 30, 38, Fredoka),
    headlineMedium = Base.headlineMedium.brand(FontWeight.SemiBold, 26, 34, Fredoka),
    headlineSmall = Base.headlineSmall.brand(FontWeight.SemiBold, 22, 30, Fredoka),
    titleLarge = Base.titleLarge.brand(FontWeight.SemiBold, 20, 28, Fredoka),
    titleMedium = Base.titleMedium.brand(FontWeight.Bold, 17, 24),
    titleSmall = Base.titleSmall.brand(FontWeight.Bold, 15, 22),
    bodyLarge = Base.bodyLarge.brand(FontWeight.Normal, 16, 26),
    bodyMedium = Base.bodyMedium.brand(FontWeight.Normal, 14, 22),
    bodySmall = Base.bodySmall.brand(FontWeight.Normal, 13, 20),
    labelLarge = Base.labelLarge.brand(FontWeight.SemiBold, 16, 20, Fredoka),
    labelMedium = Base.labelMedium.brand(FontWeight.SemiBold, 13, 18),
    labelSmall = Base.labelSmall.brand(FontWeight.SemiBold, 11, 16),
)

private val ChildooScheme = lightColorScheme(
    primary = ChildooColors.Navy,
    onPrimary = Color.White,
    primaryContainer = ChildooColors.NavySoft,
    onPrimaryContainer = Color.White,
    secondary = ChildooColors.Mint,
    onSecondary = Color.White,
    tertiary = ChildooColors.Gold,
    onTertiary = ChildooColors.Navy,
    background = ChildooColors.Cream,
    onBackground = ChildooColors.Ink,
    surface = Color.White,
    onSurface = ChildooColors.Ink,
    surfaceVariant = ChildooColors.Sand,
    onSurfaceVariant = ChildooColors.InkSoft,
    outline = ChildooColors.Line,
    outlineVariant = ChildooColors.Line,
    error = ChildooColors.CoralDeep,
)

private val ChildooShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun ChildooTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ChildooScheme,
        typography = ChildooTypography,
        shapes = ChildooShapes,
        content = content,
    )
}
