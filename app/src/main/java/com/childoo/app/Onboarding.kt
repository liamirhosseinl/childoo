package com.childoo.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.childoo.core.ChildProfile
import com.childoo.core.LockSettings
import com.childoo.core.SettingsRepository
import com.childoo.designsystem.ChildooColors
import com.childoo.designsystem.ChunkyButton
import com.childoo.designsystem.GhostButton
import com.childoo.designsystem.IconBadge
import com.childoo.designsystem.Buddy
import com.childoo.designsystem.BuddyClip
import com.childoo.designsystem.Pill
import com.childoo.designsystem.StarrySky
import com.childoo.lockkit.LockKit
import kotlinx.coroutines.launch

private enum class Step { Welcome, Child, Apps, Interval, Permissions, Xiaomi, Pin, Ready }

/** First-run setup for the parent, one decision per screen. */
@Composable
fun OnboardingFlow(repo: SettingsRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = LockSettings())
    val perms by rememberPermissionState()
    var childName by rememberSaveable { mutableStateOf("") }
    var childAge by rememberSaveable { mutableIntStateOf(ChildProfile.DEFAULT_AGE) }

    val steps = remember {
        buildList {
            add(Step.Welcome)
            add(Step.Child)
            add(Step.Apps)
            add(Step.Interval)
            add(Step.Permissions)
            if (Permissions.isXiaomiFamily) add(Step.Xiaomi)
            add(Step.Pin)
            add(Step.Ready)
        }
    }
    var index by rememberSaveable { mutableIntStateOf(0) }
    val step = steps[index]
    BackHandler(enabled = index > 0) { index-- }

    fun next() {
        if (index < steps.lastIndex) index++
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(ChildooColors.Cream)
            .safeDrawingPadding(),
    ) {
        if (step != Step.Welcome) {
            StepTopBar(
                current = index,
                total = steps.lastIndex,
                onBack = { index-- },
            )
        }

        AnimatedContent(
            targetState = index,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally { w -> if (forward) w / 3 else -w / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { w -> if (forward) -w / 3 else w / 3 } + fadeOut())
            },
            modifier = Modifier.weight(1f),
            label = "step",
        ) { i ->
            when (steps[i]) {
                Step.Welcome -> WelcomeStep()
                Step.Child -> ChildStep(
                    name = childName,
                    age = childAge,
                    onName = { childName = it },
                    onAge = { childAge = it },
                )
                Step.Apps -> AppPicker(
                    selected = settings.targetPackages,
                    onToggle = { pkg -> scope.launch { repo.toggleTarget(pkg) } },
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    header = {
                        StepHeader(
                            title = stringResource(R.string.ob_apps_title),
                            body = stringResource(R.string.ob_apps_body),
                        )
                    },
                )
                Step.Interval -> IntervalStep(
                    selected = settings.intervalMinutes,
                    onSelect = { m -> scope.launch { repo.setIntervalMinutes(m) } },
                )
                Step.Permissions -> ScrollStep {
                    StepHeader(
                        title = stringResource(R.string.ob_perms_title),
                        body = stringResource(R.string.ob_perms_body),
                    )
                    GuidedPermissions(perms)
                }
                Step.Xiaomi -> ScrollStep {
                    StepHeader(
                        title = stringResource(R.string.ob_xiaomi_title),
                        body = stringResource(R.string.ob_xiaomi_body),
                    )
                    XiaomiChecklist(perms)
                }
                Step.Pin -> ScrollStep {
                    PinSetup(
                        onPinChosen = { pin ->
                            scope.launch {
                                repo.setPin(pin)
                                next()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    )
                }
                Step.Ready -> ReadyStep(
                    appCount = settings.targetPackages.size,
                    interval = settings.intervalMinutes,
                    canTest = perms.overlay,
                    onTest = { LockKit.testChallenge(context) },
                )
            }
        }

        // Bottom action
        Box(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp)) {
            when (step) {
                Step.Welcome -> ChunkyButton(
                    text = stringResource(R.string.ob_welcome_cta),
                    onClick = ::next,
                )
                Step.Child -> ChunkyButton(
                    text = stringResource(R.string.ob_continue),
                    onClick = {
                        scope.launch {
                            repo.setChild(childName, childAge)
                            next()
                        }
                    },
                )
                Step.Pin -> Unit
                Step.Apps -> ChunkyButton(
                    text = if (settings.targetPackages.isEmpty()) {
                        stringResource(R.string.ob_apps_cta_empty)
                    } else {
                        pluralStringResource(
                            R.plurals.ob_apps_cta,
                            settings.targetPackages.size,
                            settings.targetPackages.size,
                        )
                    },
                    enabled = settings.targetPackages.isNotEmpty(),
                    onClick = ::next,
                )
                Step.Permissions -> ChunkyButton(
                    text = stringResource(if (perms.readyToProtect) R.string.ob_continue else R.string.ob_perms_cta_blocked),
                    enabled = perms.readyToProtect,
                    onClick = ::next,
                )
                Step.Ready -> ChunkyButton(
                    text = stringResource(R.string.ob_ready_cta),
                    icon = Icons.Rounded.Shield,
                    face = ChildooColors.Mint,
                    lip = Color(0xFF0E6E5E),
                    contentColor = Color.White,
                    enabled = perms.readyToProtect && settings.targetPackages.isNotEmpty(),
                    onClick = {
                        scope.launch {
                            repo.setProtectionEnabled(true)
                            LockKit.start(context)
                            repo.setOnboardingDone(true)
                        }
                    },
                )
                else -> ChunkyButton(text = stringResource(R.string.ob_continue), onClick = ::next)
            }
        }
    }
}

@Composable
private fun StepTopBar(current: Int, total: Int, onBack: () -> Unit) {
    val progress by animateFloatAsState(current / total.toFloat(), label = "progress")
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 20.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back), tint = ChildooColors.Navy)
        }
        Spacer(Modifier.width(4.dp))
        Box(
            Modifier
                .weight(1f)
                .height(8.dp)
                .clip(CircleShape)
                .background(ChildooColors.Sand),
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

@Composable
private fun StepHeader(title: String, body: String) {
    Column(Modifier.padding(top = 12.dp, bottom = 12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = ChildooColors.Navy)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = ChildooColors.InkSoft)
    }
}

@Composable
private fun ScrollStep(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp),
    ) { content() }
}

@Composable
private fun WelcomeStep() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(36.dp)),
        ) {
            StarrySky {
                Buddy(
                    Modifier
                        .align(Alignment.Center)
                        .width(190.dp),
                    clip = BuddyClip.Hello,
                )
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(20.dp),
                )
            }
        }
        Column(Modifier.padding(horizontal = 8.dp, vertical = 24.dp)) {
            Text(
                stringResource(R.string.ob_welcome_title),
                style = MaterialTheme.typography.headlineLarge,
                color = ChildooColors.Navy,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.ob_welcome_body),
                style = MaterialTheme.typography.bodyLarge,
                color = ChildooColors.InkSoft,
            )
        }
    }
}

private data class IntervalChoice(val minutes: Int, val label: Int, val note: Int, val recommended: Boolean = false)

@Composable
private fun IntervalStep(selected: Int, onSelect: (Int) -> Unit) {
    val choices = listOf(
        IntervalChoice(30, R.string.interval_30_label, R.string.interval_30_note, recommended = true),
        IntervalChoice(15, R.string.interval_15_label, R.string.interval_15_note),
        IntervalChoice(5, R.string.interval_5_label, R.string.interval_5_note),
        IntervalChoice(1, R.string.interval_1_label, R.string.interval_1_note),
    ).filter { it.minutes in SettingsRepository.INTERVAL_OPTIONS }

    ScrollStep {
        StepHeader(
            title = stringResource(R.string.ob_interval_title),
            body = stringResource(R.string.ob_interval_body),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            choices.forEach { c ->
                IntervalCard(c, selected = c.minutes == selected, onClick = { onSelect(c.minutes) })
            }
        }
    }
}

@Composable
private fun IntervalCard(choice: IntervalChoice, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) ChildooColors.Navy else Color.White)
            .border(1.5.dp, if (selected) ChildooColors.Navy else ChildooColors.Line, shape)
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
            Text(
                choice.minutes.toString(),
                style = MaterialTheme.typography.headlineLarge,
                color = if (selected) ChildooColors.Gold else ChildooColors.Navy,
            )
            Text(
                stringResource(R.string.unit_min),
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) Color.White.copy(alpha = 0.7f) else ChildooColors.InkSoft,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(choice.label),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) Color.White else ChildooColors.Ink,
                )
                if (choice.recommended) {
                    Spacer(Modifier.width(8.dp))
                    Pill(stringResource(R.string.badge_recommended), ChildooColors.Gold, filled = selected)
                }
            }
            Text(
                stringResource(choice.note),
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) Color.White.copy(alpha = 0.75f) else ChildooColors.InkSoft,
            )
        }
    }
}

@Composable
private fun ReadyStep(appCount: Int, interval: Int, canTest: Boolean, onTest: () -> Unit) {
    ScrollStep {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Buddy(Modifier.width(180.dp), clip = BuddyClip.Celebrate)
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.ob_ready_title),
                style = MaterialTheme.typography.headlineLarge,
                color = ChildooColors.Navy,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.ob_ready_body),
                style = MaterialTheme.typography.bodyLarge,
                color = ChildooColors.InkSoft,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryTile(
                    icon = Icons.Rounded.Apps,
                    tint = ChildooColors.Sky,
                    value = appCount.toString(),
                    label = stringResource(R.string.summary_apps),
                    modifier = Modifier.weight(1f),
                )
                SummaryTile(
                    icon = Icons.Rounded.Timer,
                    tint = ChildooColors.Coral,
                    value = interval.toString(),
                    label = stringResource(R.string.summary_interval),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(20.dp))
            if (canTest) {
                GhostButton(
                    text = stringResource(R.string.test_challenge),
                    icon = Icons.Rounded.PlayArrow,
                    onClick = onTest,
                )
            }
        }
    }
}

@Composable
private fun SummaryTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .border(1.5.dp, ChildooColors.Line, RoundedCornerShape(22.dp))
            .padding(16.dp),
    ) {
        IconBadge(icon, tint, size = 36.dp)
        Spacer(Modifier.height(10.dp))
        Text(value, style = MaterialTheme.typography.headlineMedium, color = ChildooColors.Navy)
        Text(label, style = MaterialTheme.typography.bodySmall, color = ChildooColors.InkSoft)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChildStep(name: String, age: Int, onName: (String) -> Unit, onAge: (Int) -> Unit) {
    ScrollStep {
        StepHeader(
            title = stringResource(R.string.ob_child_title),
            body = stringResource(R.string.ob_child_body),
        )
        OutlinedTextField(
            value = name,
            onValueChange = { onName(it.take(24)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            label = { Text(stringResource(R.string.ob_child_name)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                unfocusedBorderColor = ChildooColors.Line,
                focusedBorderColor = ChildooColors.Navy,
            ),
        )
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.ob_child_age), style = MaterialTheme.typography.titleMedium, color = ChildooColors.Navy)
        Spacer(Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ChildProfile.AGE_OPTIONS.forEach { option ->
                val selected = option == age
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selected) ChildooColors.Navy else Color.White)
                        .border(1.5.dp, if (selected) ChildooColors.Navy else ChildooColors.Line, RoundedCornerShape(20.dp))
                        .clickable { onAge(option) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (option == ChildProfile.MAX_AGE) "$option+" else option.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (selected) ChildooColors.Gold else ChildooColors.Navy,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.ob_child_age_hint), style = MaterialTheme.typography.bodySmall, color = ChildooColors.InkSoft)
    }
}
