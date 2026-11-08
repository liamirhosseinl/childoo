package com.childoo.app

import android.Manifest
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhonelinkLock
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.childoo.designsystem.ChildooColors
import com.childoo.designsystem.ChunkyButton
import com.childoo.designsystem.GhostButton
import com.childoo.designsystem.IconBadge
import com.childoo.designsystem.Pill

/** Live permission snapshot, refreshed every time the parent comes back from a settings screen. */
@Composable
fun rememberPermissionState(): State<PermissionState> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(PermissionState.read(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        AutoReturn.cancel()
        state.value = PermissionState.read(context)
    }
    return state
}

/** The two switches Childoo cannot work without, in the order that lets the app return by itself. */
private enum class Required { Overlay, Usage }

/**
 * One guided flow instead of a checklist: a trust card, then a single button that opens exactly
 * the next switch and brings the parent back automatically. Two steps in total.
 */
@Composable
fun GuidedPermissions(perms: PermissionState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val next = when {
        !perms.overlay -> Required.Overlay
        !perms.usageAccess -> Required.Usage
        else -> null
    }
    val doneCount = listOf(perms.overlay, perms.usageAccess).count { it }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TrustCard()

        StepRow(
            number = 1,
            icon = Icons.Rounded.Layers,
            tint = ChildooColors.Lilac,
            title = stringResource(R.string.perm_overlay_title),
            body = stringResource(R.string.perm_overlay_body),
            done = perms.overlay,
            active = next == Required.Overlay,
        )
        StepRow(
            number = 2,
            icon = Icons.Rounded.BarChart,
            tint = ChildooColors.Sky,
            title = stringResource(R.string.perm_usage_title),
            body = stringResource(R.string.perm_usage_body),
            done = perms.usageAccess,
            active = next == Required.Usage,
        )

        if (next != null) {
            ChunkyButton(
                text = stringResource(
                    if (next == Required.Overlay) R.string.perm_cta_overlay else R.string.perm_cta_usage,
                    doneCount + 1,
                ),
                onClick = {
                    when (next) {
                        Required.Overlay -> {
                            AutoReturn.watch(context) { Settings.canDrawOverlays(it) }
                            Permissions.openOverlay(context)
                        }
                        Required.Usage -> {
                            AutoReturn.watch(context) { Permissions.hasUsageAccess(it) }
                            Permissions.openUsageAccess(context)
                        }
                    }
                },
            )
            Text(
                stringResource(R.string.perm_cta_hint),
                style = MaterialTheme.typography.bodySmall,
                color = ChildooColors.InkSoft,
                modifier = Modifier.fillMaxWidth(),
            )
            if (next == Required.Usage && Permissions.mayHitRestrictedSettings) {
                RestrictedSettingsHelp(onOpenAppInfo = { Permissions.openAppDetails(context) })
            }
        } else {
            AllSetRow()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !perms.notifications) {
                NotificationOptIn()
            }
        }
    }
}

@Composable
private fun TrustCard() {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(ChildooColors.Navy)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TrustLine(Icons.Rounded.VisibilityOff, stringResource(R.string.trust_screen))
        TrustLine(Icons.Rounded.PhonelinkLock, stringResource(R.string.trust_device))
        TrustLine(Icons.Rounded.Lock, stringResource(R.string.trust_pin))
    }
}

@Composable
private fun TrustLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = ChildooColors.Gold, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Color.White)
    }
}

@Composable
private fun StepRow(
    number: Int,
    icon: ImageVector,
    tint: Color,
    title: String,
    body: String,
    done: Boolean,
    active: Boolean,
) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(
                width = if (active) 2.dp else 1.5.dp,
                color = when {
                    done -> ChildooColors.Mint.copy(alpha = 0.4f)
                    active -> ChildooColors.Gold
                    else -> ChildooColors.Line
                },
                shape = shape,
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, tint)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.perm_step, number),
                style = MaterialTheme.typography.labelSmall,
                color = ChildooColors.InkSoft,
            )
            Text(title, style = MaterialTheme.typography.titleSmall, color = ChildooColors.Ink)
            Text(body, style = MaterialTheme.typography.bodySmall, color = ChildooColors.InkSoft)
        }
        Spacer(Modifier.width(10.dp))
        if (done) {
            Box(
                Modifier
                    .size(34.dp)
                    .background(ChildooColors.Mint, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.perm_granted), tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun AllSetRow() {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(ChildooColors.Mint.copy(alpha = 0.12f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Check, contentDescription = null, tint = ChildooColors.Mint)
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.perm_all_set), style = MaterialTheme.typography.titleSmall, color = ChildooColors.Mint)
    }
}

@Composable
private fun NotificationOptIn() {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    OptionalCard(
        icon = Icons.Rounded.Notifications,
        tint = ChildooColors.Coral,
        title = stringResource(R.string.perm_notifications_title),
        body = stringResource(R.string.perm_notifications_body),
        action = stringResource(R.string.perm_grant),
        onAction = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) },
    )
}

@Composable
private fun OptionalCard(
    icon: ImageVector,
    tint: Color,
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
    footer: @Composable () -> Unit = {},
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(1.5.dp, ChildooColors.Line, shape)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, tint)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleSmall, color = ChildooColors.Ink, modifier = Modifier.weight(1f, fill = false))
                    Spacer(Modifier.width(8.dp))
                    Pill(stringResource(R.string.badge_recommended), ChildooColors.InkSoft)
                }
                Spacer(Modifier.height(2.dp))
                Text(body, style = MaterialTheme.typography.bodySmall, color = ChildooColors.InkSoft)
            }
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(ChildooColors.Gold)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 18.dp, vertical = 9.dp),
            ) {
                Text(action, style = MaterialTheme.typography.labelLarge, color = ChildooColors.Navy)
            }
        }
        footer()
    }
}

/** Shown under step 2 on Android 13+: how to lift the "restricted setting" block on sideloaded builds. */
@Composable
private fun RestrictedSettingsHelp(onOpenAppInfo: () -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ChildooColors.Sky.copy(alpha = 0.08f))
            .clickable { open = !open }
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Info, contentDescription = null, tint = ChildooColors.SkyDeep, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.restricted_title),
                style = MaterialTheme.typography.labelLarge,
                color = ChildooColors.SkyDeep,
                modifier = Modifier.weight(1f),
            )
        }
        AnimatedVisibility(visible = open) {
            Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.restricted_body), style = MaterialTheme.typography.bodySmall, color = ChildooColors.InkSoft)
                listOf(R.string.restricted_step1, R.string.restricted_step2, R.string.restricted_step3)
                    .forEachIndexed { i, step -> NumberedStep(i + 1, stringResource(step)) }
                Spacer(Modifier.height(4.dp))
                GhostButton(
                    text = stringResource(R.string.restricted_open_app_info),
                    onClick = onOpenAppInfo,
                    color = ChildooColors.SkyDeep,
                )
            }
        }
    }
}

@Composable
private fun NumberedStep(number: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(22.dp)
                .background(ChildooColors.SkyDeep, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(number.toString(), style = MaterialTheme.typography.labelSmall, color = Color.White)
        }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = ChildooColors.Ink, modifier = Modifier.weight(1f))
    }
}

/** MIUI / HyperOS extras. These can't be read back by apps, so they are plain shortcuts. */
@Composable
fun XiaomiChecklist(perms: PermissionState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var autostartOpened by rememberSaveable { mutableStateOf(false) }
    var popupsOpened by rememberSaveable { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OptionalCard(
            icon = Icons.Rounded.RestartAlt,
            tint = ChildooColors.Coral,
            title = stringResource(R.string.xiaomi_autostart_title),
            body = stringResource(R.string.xiaomi_autostart_body),
            action = stringResource(R.string.open),
            onAction = {
                Permissions.openXiaomiAutostart(context)
                autostartOpened = true
            },
            footer = { if (autostartOpened) OpenedHint() },
        )
        OptionalCard(
            icon = Icons.Rounded.Widgets,
            tint = ChildooColors.Lilac,
            title = stringResource(R.string.xiaomi_popups_title),
            body = stringResource(R.string.xiaomi_popups_body),
            action = stringResource(R.string.open),
            onAction = {
                Permissions.openXiaomiOtherPermissions(context)
                popupsOpened = true
            },
            footer = { if (popupsOpened) OpenedHint() },
        )
        if (!perms.batteryUnrestricted) {
            OptionalCard(
                icon = Icons.Rounded.BatteryChargingFull,
                tint = ChildooColors.Mint,
                title = stringResource(R.string.perm_battery_title),
                body = stringResource(R.string.perm_battery_body),
                action = stringResource(R.string.open),
                onAction = { Permissions.openBattery(context) },
            )
        }
    }
}

@Composable
private fun OpenedHint() {
    Spacer(Modifier.height(10.dp))
    Text(
        stringResource(R.string.xiaomi_opened_hint),
        style = MaterialTheme.typography.bodySmall,
        color = ChildooColors.Mint,
    )
}
