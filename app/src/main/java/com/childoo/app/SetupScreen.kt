package com.childoo.app

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.childoo.core.LockSettings
import com.childoo.core.SettingsRepository
import com.childoo.lockkit.LockKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Parent setup for the lock prototype: grant permissions, pick apps, pick interval, switch on.
 * This screen is the parent area; later it moves behind a parent gate.
 */
@Composable
fun SetupScreen() {
    val context = LocalContext.current
    val repo = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()

    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = LockSettings())
    val solved by repo.challengesSolved.collectAsStateWithLifecycle(initialValue = 0)

    var perms by remember { mutableStateOf(PermissionState.read(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { perms = PermissionState.read(context) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { perms = PermissionState.read(context) }

    val apps by produceState<List<InstalledApp>?>(initialValue = null) {
        value = withContext(Dispatchers.Default) { loadLaunchableApps(context) }
    }
    val sortedApps = remember(apps, settings.targetPackages) {
        apps?.sortedWith(
            compareByDescending<InstalledApp> { it.packageName in settings.targetPackages }
                .thenByDescending { it.packageName in SuggestedTargets },
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Header(solved) }

        // 1. Main switch
        item {
            ProtectionCard(
                enabled = settings.protectionEnabled,
                canEnable = perms.readyToProtect && settings.targetPackages.isNotEmpty(),
                needsApps = settings.targetPackages.isEmpty(),
                needsPermissions = !perms.readyToProtect,
                onToggle = { on ->
                    scope.launch {
                        repo.setProtectionEnabled(on)
                        if (on) LockKit.start(context) else LockKit.stop(context)
                    }
                },
            )
        }

        // 2. Permissions
        item { SectionTitle(stringResource(R.string.section_permissions)) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(vertical = 4.dp)) {
                    PermissionRow(
                        title = stringResource(R.string.perm_usage_title),
                        body = stringResource(R.string.perm_usage_body),
                        granted = perms.usageAccess,
                        onGrant = { Permissions.openUsageAccess(context) },
                    )
                    PermissionRow(
                        title = stringResource(R.string.perm_overlay_title),
                        body = stringResource(R.string.perm_overlay_body),
                        granted = perms.overlay,
                        onGrant = { Permissions.openOverlay(context) },
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        PermissionRow(
                            title = stringResource(R.string.perm_notifications_title),
                            body = stringResource(R.string.perm_notifications_body),
                            granted = perms.notifications,
                            onGrant = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        )
                    }
                    PermissionRow(
                        title = stringResource(R.string.perm_battery_title),
                        body = stringResource(R.string.perm_battery_body),
                        granted = perms.batteryUnrestricted,
                        onGrant = { Permissions.openBattery(context) },
                    )
                }
            }
        }

        // 2b. Xiaomi / Redmi / POCO extras: these can't be detected, so they're plain links.
        if (Permissions.isXiaomiFamily) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Gold.copy(alpha = 0.18f))) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.xiaomi_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(stringResource(R.string.xiaomi_body), style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { Permissions.openXiaomiAutostart(context) }) {
                                Text(stringResource(R.string.xiaomi_autostart))
                            }
                            OutlinedButton(onClick = { Permissions.openXiaomiOtherPermissions(context) }) {
                                Text(stringResource(R.string.xiaomi_popups))
                            }
                        }
                    }
                }
            }
        }

        // 3. Interval
        item { SectionTitle(stringResource(R.string.section_interval)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsRepository.INTERVAL_OPTIONS.forEach { minutes ->
                    FilterChip(
                        selected = settings.intervalMinutes == minutes,
                        onClick = { scope.launch { repo.setIntervalMinutes(minutes) } },
                        label = { Text(stringResource(R.string.interval_minutes, minutes)) },
                    )
                }
            }
        }

        // 4. Test
        item {
            FilledTonalButton(
                onClick = { LockKit.testChallenge(context) },
                enabled = perms.overlay,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.test_challenge)) }
        }

        // 5. Target apps
        item {
            SectionTitle(stringResource(R.string.section_apps, settings.targetPackages.size))
        }
        val list = sortedApps
        if (list == null) {
            item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else {
            items(list, key = { it.packageName }) { app ->
                AppRow(
                    app = app,
                    checked = app.packageName in settings.targetPackages,
                    suggested = app.packageName in SuggestedTargets,
                    onToggle = { scope.launch { repo.toggleTarget(app.packageName) } },
                )
            }
        }
    }
}

@Composable
private fun Header(solved: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = androidx.compose.ui.res.painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .background(Navy, CircleShape),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                stringResource(R.string.app_name),
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Navy,
            )
            Text(
                stringResource(R.string.challenges_solved, solved),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProtectionCard(
    enabled: Boolean,
    canEnable: Boolean,
    needsApps: Boolean,
    needsPermissions: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (enabled) Mint else Navy),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (enabled) R.string.protection_on else R.string.protection_off),
                    color = androidx.compose.ui.graphics.Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                val hint = when {
                    enabled -> R.string.protection_on_hint
                    needsPermissions -> R.string.protection_needs_permissions
                    needsApps -> R.string.protection_needs_apps
                    else -> R.string.protection_off_hint
                }
                Text(
                    stringResource(hint),
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                // Turning off is always allowed; turning on needs permissions and at least one app.
                enabled = enabled || canEnable,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Navy,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun PermissionRow(title: String, body: String, granted: Boolean, onGrant: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        if (granted) {
            Surface(shape = RoundedCornerShape(50), color = Mint.copy(alpha = 0.15f)) {
                Text(
                    stringResource(R.string.perm_granted),
                    color = Mint,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        } else {
            Button(
                onClick = onGrant,
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Navy),
            ) { Text(stringResource(R.string.perm_grant)) }
        }
    }
}

@Composable
private fun AppRow(app: InstalledApp, checked: Boolean, suggested: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val icon = app.icon
        if (icon != null) {
            Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(40.dp))
        } else {
            Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            if (suggested) {
                Text(
                    stringResource(R.string.app_suggested),
                    style = MaterialTheme.typography.labelSmall,
                    color = Coral,
                )
            }
        }
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
    }
}
