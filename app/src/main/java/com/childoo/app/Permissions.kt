package com.childoo.app

import android.Manifest
import android.app.AppOpsManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat

/** Snapshot of everything the lock needs; re-read on every ON_RESUME. */
data class PermissionState(
    val usageAccess: Boolean,
    val overlay: Boolean,
    val notifications: Boolean,
    val batteryUnrestricted: Boolean,
) {
    val readyToProtect: Boolean get() = usageAccess && overlay

    companion object {
        fun read(context: Context) = PermissionState(
            usageAccess = Permissions.hasUsageAccess(context),
            overlay = Settings.canDrawOverlays(context),
            notifications = Permissions.hasNotifications(context),
            batteryUnrestricted = Permissions.ignoresBatteryOptimizations(context),
        )
    }
}

object Permissions {

    val isXiaomiFamily: Boolean
        get() = Build.MANUFACTURER.lowercase() in setOf("xiaomi", "redmi", "poco")

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return if (mode == AppOpsManager.MODE_DEFAULT) {
            context.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            mode == AppOpsManager.MODE_ALLOWED
        }
    }

    fun hasNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun ignoresBatteryOptimizations(context: Context): Boolean {
        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return power.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun openUsageAccess(context: Context) = open(
        context,
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
    )

    fun openOverlay(context: Context) = open(
        context,
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri(context)),
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION),
    )

    fun openBattery(context: Context) = open(
        context,
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
        appDetails(context),
    )

    /** MIUI / HyperOS: lets the service come back after reboot or after the app is swiped away. */
    fun openXiaomiAutostart(context: Context) = open(
        context,
        Intent().setComponent(
            ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity",
            ),
        ),
        appDetails(context),
    )

    /** MIUI / HyperOS: "Display pop-up windows while running in the background" lives here. */
    fun openXiaomiOtherPermissions(context: Context) = open(
        context,
        Intent("miui.intent.action.APP_PERM_EDITOR")
            .setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
            .putExtra("extra_pkgname", context.packageName),
        Intent("miui.intent.action.APP_PERM_EDITOR").putExtra("extra_pkgname", context.packageName),
        appDetails(context),
    )

    private fun appDetails(context: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(context))

    private fun packageUri(context: Context): Uri = Uri.parse("package:${context.packageName}")

    /** Tries each intent in order; OEM screens move between versions, so always keep a fallback. */
    private fun open(context: Context, vararg intents: Intent) {
        for (intent in intents) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (_: ActivityNotFoundException) {
                // try next
            } catch (_: SecurityException) {
                // try next
            }
        }
    }
}
