package com.childoo.app

import android.Manifest
import android.os.Build
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
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.RestartAlt
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
import com.childoo.designsystem.GhostButton
import com.childoo.designsystem.IconBadge
import com.childoo.designsystem.Pill
import com.childoo.designsystem.localDigits

/** Live permission snapshot, refreshed every time the parent comes back from a settings screen. */
@Composable
fun rememberPermissionState(): State<PermissionState> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(PermissionState.read(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { state.value = PermissionState.read(context) }
    return state
}

/** All system permissions as friendly cards, required ones first. */
@Composable
fun PermissionChecklist(perms: PermissionState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PermissionCard(
            icon = Icons.Rounded.BarChart,
            tint = ChildooColors.Sky,
            title = stringResource(R.string.perm_usage_title),
            body = stringResource(R.string.perm_usage_body),
            required = true,
            granted = perms.usageAccess,
            onGrant = { Permissions.openUsageAccess(context) },
        ) {
            if (!perms.usageAccess && Permissions.mayHitRestrictedSettings) {
                RestrictedSettingsHelp(onOpenAppInfo = { Permissions.openAppDetails(context) })
            }
        }
        PermissionCard(
            icon = Icons.Rounded.Layers,
            tint = ChildooColors.Lilac,
            title = stringResource(R.string.perm_overlay_title),
            body = stringResource(R.string.perm_overlay_body),
            required = true,
            granted = perms.overlay,
            onGrant = { Permissions.openOverlay(context) },
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionCard(
                icon = Icons.Rounded.Notifications,
                tint = ChildooColors.Coral,
                title = stringResource(R.string.perm_notifications_title),
                body = stringResource(R.string.perm_notifications_body),
                required = false,
                granted = perms.notifications,
                onGrant = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
            )
        }
        PermissionCard(
            icon = Icons.Rounded.BatteryChargingFull,
            tint = ChildooColors.Mint,
            title = stringResource(R.string.perm_battery_title),
            body = stringResource(R.string.perm_battery_body),
            required = false,
            granted = perms.batteryUnrestricted,
            onGrant = { Permissions.openBattery(context) },
        )
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    tint: Color,
    title: String,
    body: String,
    required: Boolean,
    granted: Boolean,
    onGrant: () -> Unit,
    extra: @Composable () -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .border(
                width = 1.5.dp,
                color = if (granted) ChildooColors.Mint.copy(alpha = 0.35f) else ChildooColors.Line,
                shape = RoundedCornerShape(22.dp),
            )
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, tint)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleSmall, color = ChildooColors.Ink, modifier = Modifier.weight(1f, fill = false))
                    Spacer(Modifier.width(8.dp))
                    if (!granted) {
                        Pill(
                            text = stringResource(if (required) R.string.badge_required else R.string.badge_recommended),
                            color = if (required) ChildooColors.CoralDeep else ChildooColors.InkSoft,
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(body, style = MaterialTheme.typography.bodySmall, color = ChildooColors.InkSoft)
            }
            Spacer(Modifier.width(12.dp))
            if (granted) {
                Box(
                    Modifier
                        .size(36.dp)
                        .background(ChildooColors.Mint, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.perm_granted), tint = Color.White, modifier = Modifier.size(22.dp))
                }
            } else {
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(ChildooColors.Gold)
                        .clickable(onClick = onGrant)
                        .padding(horizontal = 18.dp, vertical = 9.dp),
                ) {
                    Text(stringResource(R.string.perm_grant), style = MaterialTheme.typography.labelLarge, color = ChildooColors.Navy)
                }
            }
        }
        extra()
    }
}

/** Shown under Usage access on Android 13+: how to lift the "restricted setting" block. */
@Composable
private fun RestrictedSettingsHelp(onOpenAppInfo: () -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    Spacer(Modifier.height(12.dp))
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
            Text(localDigits(number.toString()), style = MaterialTheme.typography.labelSmall, color = Color.White)
        }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = ChildooColors.Ink, modifier = Modifier.weight(1f))
    }
}

/** MIUI / HyperOS extras. These can't be read back by apps, so the parent ticks them off. */
@Composable
fun XiaomiChecklist(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var autostartDone by rememberSaveable { mutableStateOf(false) }
    var popupsDone by rememberSaveable { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ManualStepCard(
            icon = Icons.Rounded.RestartAlt,
            tint = ChildooColors.Coral,
            title = stringResource(R.string.xiaomi_autostart_title),
            body = stringResource(R.string.xiaomi_autostart_body),
            done = autostartDone,
            onOpen = {
                Permissions.openXiaomiAutostart(context)
                autostartDone = true
            },
        )
        ManualStepCard(
            icon = Icons.Rounded.Widgets,
            tint = ChildooColors.Lilac,
            title = stringResource(R.string.xiaomi_popups_title),
            body = stringResource(R.string.xiaomi_popups_body),
            done = popupsDone,
            onOpen = {
                Permissions.openXiaomiOtherPermissions(context)
                popupsDone = true
            },
        )
    }
}

@Composable
private fun ManualStepCard(
    icon: ImageVector,
    tint: Color,
    title: String,
    body: String,
    done: Boolean,
    onOpen: () -> Unit,
) {
    PermissionCard(
        icon = icon,
        tint = tint,
        title = title,
        body = body,
        required = false,
        granted = false,
        onGrant = onOpen,
    ) {
        if (done) {
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.xiaomi_opened_hint),
                style = MaterialTheme.typography.bodySmall,
                color = ChildooColors.Mint,
            )
        }
    }
}
