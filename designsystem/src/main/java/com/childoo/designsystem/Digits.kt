package com.childoo.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration

private const val PERSIAN_DIGITS = "۰۱۲۳۴۵۶۷۸۹"

/** Rewrites ASCII digits as Persian digits; everything else is left untouched. */
fun String.toPersianDigits(): String = buildString(length) {
    for (c in this@toPersianDigits) append(if (c in '0'..'9') PERSIAN_DIGITS[c - '0'] else c)
}

/** True when the UI is currently shown in Persian. */
@Composable
@ReadOnlyComposable
fun isPersianUi(): Boolean = LocalConfiguration.current.locales[0].language == "fa"

/** Formats numbers in the digits a reader of the current UI language expects. */
@Composable
@ReadOnlyComposable
fun localDigits(text: String): String = if (isPersianUi()) text.toPersianDigits() else text
