package com.childoo.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.childoo.core.ChildProfile
import com.childoo.core.Interests
import com.childoo.core.LockSettings
import com.childoo.core.Rewards
import com.childoo.core.SoundPrefs
import com.childoo.core.SettingsRepository
import com.childoo.designsystem.ChildooColors
import com.childoo.designsystem.GhostButton
import com.childoo.designsystem.IconBadge
import com.childoo.designsystem.Nuzo
import com.childoo.designsystem.NuzoMood
import com.childoo.lockkit.LockKit
import kotlinx.coroutines.launch

/** The parent's dashboard after setup. */
@Composable
fun HomeScreen(repo: SettingsRepository, onKidCorner: () -> Unit) {
    var editingApps by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = editingApps) { editingApps = false }

    AnimatedContent(
        targetState = editingApps,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "home",
    ) { editing ->
        if (editing) {
            EditAppsScreen(repo, onDone = { editingApps = false })
        } else {
            Dashboard(repo, onEditApps = { editingApps = true }, onKidCorner = onKidCorner)
        }
    }
}

@Composable
private fun Dashboard(repo: SettingsRepository, onEditApps: () -> Unit, onKidCorner: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = LockSettings())
    val solved by repo.challengesSolved.collectAsStateWithLifecycle(initialValue = 0)
    val playedMs by repo.playedSinceChallengeMs.collectAsStateWithLifecycle(initialValue = 0L)
    val perms by rememberPermissionState()
    val child by repo.child.collectAsStateWithLifecycle(initialValue = ChildProfile())
    val rewards by repo.rewards.collectAsStateWithLifecycle(initialValue = Rewards())
    val sound by repo.soundPrefs.collectAsStateWithLifecycle(initialValue = SoundPrefs())
    val interests by repo.interests.collectAsStateWithLifecycle(initialValue = Interests())
    val apps by rememberInstalledApps()
    val selectedApps = remember(apps, settings.targetPackages) {
        apps.orEmpty().filter { it.packageName in settings.targetPackages }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(ChildooColors.Cream)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Nuzo(Modifier.width(40.dp), mood = NuzoMood.Happy)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    color = ChildooColors.Navy,
                )
                Text(
                    if (child.name.isBlank()) stringResource(R.string.home_child_age, child.age)
                    else stringResource(R.string.home_child, child.name, child.age),
                    style = MaterialTheme.typography.bodySmall,
                    color = ChildooColors.InkSoft,
                )
            }
        }

        val protectionOn = settings.protectionEnabled && perms.readyToProtect
        StatusHero(
            enabled = protectionOn,
            playedMinutes = (playedMs / 60_000L).toInt(),
            intervalMinutes = settings.intervalMinutes,
            canEnable = perms.readyToProtect && settings.targetPackages.isNotEmpty(),
            onToggle = { on ->
                scope.launch {
                    repo.setProtectionEnabled(on)
                    if (on) LockKit.start(context) else LockKit.stop(context)
                }
            },
        )

        if (!perms.readyToProtect) {
            AttentionCard()
            GuidedPermissions(perms)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                icon = Icons.Rounded.EmojiEvents,
                tint = ChildooColors.GoldDeep,
                value = solved.toString(),
                label = stringResource(R.string.stat_solved),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.Rounded.Apps,
                tint = ChildooColors.Sky,
                value = settings.targetPackages.size.toString(),
                label = stringResource(R.string.stat_apps),
                modifier = Modifier.weight(1f),
            )
        }

        BuddyCard(rewards = rewards, onOpen = onKidCorner)

        SectionCard(title = stringResource(R.string.interests_title)) {
            InterestsChart(interests)
        }

        // Protected apps
        SectionCard(
            title = stringResource(R.string.home_apps_title),
            action = stringResource(R.string.home_apps_edit),
            onAction = onEditApps,
        ) {
            if (selectedApps.isEmpty()) {
                Text(
                    stringResource(R.string.home_apps_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChildooColors.InkSoft,
                )
            } else {
                SelectedAppsStrip(selectedApps, max = 6)
            }
        }

        // Interval
        SectionCard(title = stringResource(R.string.home_interval_title)) {
            IntervalSegments(
                selected = settings.intervalMinutes,
                onSelect = { m -> scope.launch { repo.setIntervalMinutes(m) } },
            )
        }

        SectionCard(title = stringResource(R.string.home_sound_title)) {
            SoundRow(stringResource(R.string.sound_effects), sound.effects) { on -> scope.launch { repo.setSoundPrefs(sound.copy(effects = on)) } }
            SoundRow(stringResource(R.string.sound_music), sound.music) { on -> scope.launch { repo.setSoundPrefs(sound.copy(music = on)) } }
        }

        GhostButton(
            text = stringResource(R.string.test_challenge),
            icon = Icons.Rounded.PlayArrow,
            onClick = { LockKit.testChallenge(context) },
        )

        if (Permissions.isXiaomiFamily) {
            XiaomiSection(perms)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun StatusHero(
    enabled: Boolean,
    playedMinutes: Int,
    intervalMinutes: Int,
    canEnable: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val gradient = if (enabled) {
        Brush.linearGradient(listOf(ChildooColors.Navy, ChildooColors.NavySoft))
    } else {
        Brush.linearGradient(listOf(Color(0xFF4A4E73), Color(0xFF6B6F94)))
    }
    val played = playedMinutes.coerceAtMost(intervalMinutes)
    val remaining = (intervalMinutes - played).coerceAtLeast(0)

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(gradient)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProgressRing(
            progress = if (enabled) played / intervalMinutes.toFloat() else 0f,
            modifier = Modifier.size(108.dp),
        ) {
            if (enabled) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        remaining.toString(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                    )
                    Text(
                        stringResource(R.string.unit_min),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            } else {
                Nuzo(Modifier.width(54.dp), mood = NuzoMood.Thinking)
            }
        }
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(if (enabled) R.string.protection_on else R.string.protection_off),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (enabled) {
                    stringResource(R.string.protection_on_hint, remaining.toString())
                } else {
                    stringResource(R.string.protection_off_hint)
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(12.dp))
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                enabled = enabled || canEnable,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = ChildooColors.Gold,
                    checkedThumbColor = ChildooColors.Navy,
                    checkedBorderColor = ChildooColors.Gold,
                    uncheckedTrackColor = Color.White.copy(alpha = 0.2f),
                    uncheckedThumbColor = Color.White,
                    uncheckedBorderColor = Color.White.copy(alpha = 0.4f),
                ),
            )
        }
    }
}

@Composable
private fun ProgressRing(progress: Float, modifier: Modifier = Modifier, center: @Composable () -> Unit) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "ring")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.1f
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = Color.White.copy(alpha = 0.16f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke),
            )
            if (animated > 0f) {
                drawArc(
                    color = ChildooColors.Gold,
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        center()
    }
}

@Composable
private fun AttentionCard() {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ChildooColors.Coral.copy(alpha = 0.12f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = ChildooColors.CoralDeep)
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(R.string.home_attention),
            style = MaterialTheme.typography.bodyMedium,
            color = ChildooColors.Ink,
        )
    }
}

@Composable
private fun StatCard(icon: ImageVector, tint: Color, value: String, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .border(1.5.dp, ChildooColors.Line, RoundedCornerShape(22.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, tint, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(value, style = MaterialTheme.typography.headlineSmall, color = ChildooColors.Navy)
            Text(label, style = MaterialTheme.typography.bodySmall, color = ChildooColors.InkSoft, maxLines = 1)
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    action: String? = null,
    onAction: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.5.dp, ChildooColors.Line, RoundedCornerShape(24.dp))
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = ChildooColors.Navy, modifier = Modifier.weight(1f))
            if (action != null) {
                Row(
                    Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onAction)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, tint = ChildooColors.Mint, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(action, style = MaterialTheme.typography.labelLarge, color = ChildooColors.Mint)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun IntervalSegments(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(ChildooColors.Sand)
            .padding(4.dp),
    ) {
        SettingsRepository.INTERVAL_OPTIONS.forEach { minutes ->
            val isSelected = minutes == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) ChildooColors.Navy else Color.Transparent)
                    .clickable { onSelect(minutes) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.interval_short, minutes.toString()),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) Color.White else ChildooColors.InkSoft,
                )
            }
        }
    }
}

@Composable
private fun XiaomiSection(perms: PermissionState) {
    var open by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(ChildooColors.Gold.copy(alpha = 0.16f))
            .clickable { open = !open }
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.Timer, ChildooColors.GoldDeep, size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.ob_xiaomi_title), style = MaterialTheme.typography.titleSmall, color = ChildooColors.Ink)
                Text(stringResource(R.string.home_xiaomi_hint), style = MaterialTheme.typography.bodySmall, color = ChildooColors.InkSoft)
            }
        }
        if (open) {
            Spacer(Modifier.height(14.dp))
            XiaomiChecklist(perms)
        }
    }
}

@Composable
private fun EditAppsScreen(repo: SettingsRepository, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = LockSettings())
    Column(
        Modifier
            .fillMaxSize()
            .background(ChildooColors.Cream)
            .safeDrawingPadding(),
    ) {
        Row(
            Modifier.padding(start = 8.dp, end = 20.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDone) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back), tint = ChildooColors.Navy)
            }
            Text(
                stringResource(R.string.home_apps_title),
                style = MaterialTheme.typography.titleLarge,
                color = ChildooColors.Navy,
            )
        }
        AppPicker(
            selected = settings.targetPackages,
            onToggle = { pkg -> scope.launch { repo.toggleTarget(pkg) } },
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        )
    }
}

@Composable
private fun SoundRow(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = ChildooColors.Ink, modifier = Modifier.weight(1f))
        Switch(
            checked = on,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = ChildooColors.Mint,
                checkedThumbColor = Color.White,
                checkedBorderColor = ChildooColors.Mint,
            ),
        )
    }
}

/** The child's side of things at a glance: buddy, level, coins, streak, and the door to the Kid's Corner. */
@Composable
private fun BuddyCard(rewards: Rewards, onOpen: () -> Unit) {
    val today = remember { java.time.LocalDate.now().toEpochDay() }
    val wallet = rewards.wallet
    val level = wallet.level
    val from = com.childoo.core.Wallet.xpForLevel(level)
    val to = com.childoo.core.Wallet.xpForLevel(level + 1)
    val progress = ((wallet.xp - from).toFloat() / (to - from).coerceAtLeast(1)).coerceIn(0f, 1f)
    val streak = rewards.streak.current(today)
    val name = when (rewards.companion) {
        "lotli" -> "Lotli"
        "embrix" -> "Embrix"
        "nuzo" -> "Nuzo"
        else -> null
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF4B3699), Color(0xFF7B5CD6))))
            .clickable(onClick = onOpen)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Nuzo(Modifier.width(52.dp), mood = NuzoMood.Happy)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (name == null) stringResource(R.string.buddy_none) else stringResource(R.string.buddy_level, name, level),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(ChildooColors.Gold),
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Chip(stringResource(R.string.buddy_coins, wallet.coins))
            if (streak >= 1) Chip(stringResource(R.string.buddy_streak, streak))
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.kid_corner_open), style = MaterialTheme.typography.labelLarge, color = ChildooColors.Gold)
        }
    }
}

@Composable
private fun Chip(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White,
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.2f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/** Which island the child picks when given the choice: an early read on what they enjoy. */
@Composable
private fun InterestsChart(interests: Interests) {
    if (interests.total == 0) {
        Text(
            stringResource(R.string.interests_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = ChildooColors.InkSoft,
        )
        return
    }
    val rows = listOf(
        Triple("nuzo", R.string.island_numbers, ChildooColors.Sky),
        Triple("lotli", R.string.island_words, ChildooColors.Mint),
        Triple("embrix", R.string.island_courage, ChildooColors.Coral),
    ).sortedByDescending { interests.counts[it.first] ?: 0 }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { (world, name, color) ->
            val count = interests.counts[world] ?: 0
            val share = interests.share(world)
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(name), style = MaterialTheme.typography.titleSmall, color = ChildooColors.Ink, modifier = Modifier.weight(1f))
                    Text(
                        stringResource(R.string.interests_count, (share * 100).toInt(), count),
                        style = MaterialTheme.typography.bodySmall,
                        color = ChildooColors.InkSoft,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(ChildooColors.Sand),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(share.coerceIn(0.02f, 1f))
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(color),
                    )
                }
            }
        }
        Text(
            stringResource(R.string.interests_hint, interests.total),
            style = MaterialTheme.typography.bodySmall,
            color = ChildooColors.InkSoft,
        )
    }
}
