package com.childoo.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
)

/** Apps kids most often lose hours in; shown first so the parent finds them fast. */
val SuggestedTargets = setOf(
    "com.google.android.youtube",
    "com.google.android.apps.youtube.kids",
    "com.zhiliaoapp.musically",
    "com.ss.android.ugc.trill",
    "com.instagram.android",
    "com.roblox.client",
    "com.supercell.clashroyale",
    "com.supercell.brawlstars",
    "com.mojang.minecraftpe",
    "com.tencent.ig",
    "com.dts.freefireth",
    "com.netflix.mediaclient",
    "org.telegram.messenger",
    "com.whatsapp",
)

/** Slow (labels + icons); call off the main thread. */
fun loadLaunchableApps(context: Context): List<InstalledApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val infos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
    } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, 0)
    }
    val iconPx = (48 * context.resources.displayMetrics.density).toInt()
    return infos
        .distinctBy { it.activityInfo.packageName }
        .filter { it.activityInfo.packageName != context.packageName }
        .map { info ->
            InstalledApp(
                packageName = info.activityInfo.packageName,
                label = info.loadLabel(pm).toString(),
                icon = runCatching { info.loadIcon(pm).toBitmap(iconPx, iconPx).asImageBitmap() }.getOrNull(),
            )
        }
        .sortedBy { it.label.lowercase() }
}
