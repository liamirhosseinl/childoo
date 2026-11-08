package com.childoo.app

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.childoo.designsystem.ChildooColors
import com.childoo.designsystem.isPersianUi

/** In-app language switch. Uses the system per-app language API (Android 13+). */
object AppLanguage {
    val isSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun set(context: Context, languageTag: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)?.applicationLocales =
                LocaleList.forLanguageTags(languageTag)
        }
    }
}

/** Compact EN | فا segmented toggle. Hidden on Android versions without per-app languages. */
@Composable
fun LanguageToggle(modifier: Modifier = Modifier, onDark: Boolean = false) {
    if (!AppLanguage.isSupported) return
    val context = LocalContext.current
    val persian = isPersianUi()
    val track = if (onDark) Color.White.copy(alpha = 0.14f) else ChildooColors.Sand
    val idleText = if (onDark) Color.White.copy(alpha = 0.75f) else ChildooColors.InkSoft
    Row(
        modifier
            .clip(CircleShape)
            .background(track)
            .padding(3.dp),
    ) {
        listOf("en" to "EN", "fa" to "فا").forEach { (tag, label) ->
            val selected = (tag == "fa") == persian
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(if (selected) Color.White else Color.Transparent)
                    .clickable(enabled = !selected) { AppLanguage.set(context, tag) }
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) ChildooColors.Navy else idleText,
                )
            }
        }
    }
}
