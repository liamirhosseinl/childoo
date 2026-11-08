package com.childoo.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.childoo.core.ParentPin
import com.childoo.core.SettingsRepository
import com.childoo.designsystem.ChildooColors
import com.childoo.designsystem.IconBadge
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Four dots and a keypad. Shared by PIN creation and the parent gate. */
@Composable
fun PinPad(
    title: String,
    subtitle: String,
    error: String?,
    shakeKey: Int,
    enabled: Boolean,
    onComplete: (String) -> Unit,
    modifier: Modifier = Modifier,
    footer: @Composable () -> Unit = {},
) {
    var entered by remember(title) { mutableStateOf("") }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(shakeKey) {
        if (shakeKey == 0) return@LaunchedEffect
        entered = ""
        for (x in listOf(18f, -16f, 10f, -6f, 0f)) shake.animateTo(x, spring(stiffness = 2_000f))
    }

    fun press(d: Char) {
        if (!enabled || entered.length >= ParentPin.LENGTH) return
        entered += d
        if (entered.length == ParentPin.LENGTH) {
            val pin = entered
            onComplete(pin)
        }
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        IconBadge(Icons.Rounded.Lock, ChildooColors.Navy, size = 56.dp)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, color = ChildooColors.Navy, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = ChildooColors.InkSoft, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.graphicsLayer { translationX = shake.value },
        ) {
            repeat(ParentPin.LENGTH) { i ->
                val filled = i < entered.length
                Box(
                    Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (filled) ChildooColors.Navy else Color.Transparent)
                        .border(2.dp, ChildooColors.Navy, CircleShape),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            error ?: " ",
            style = MaterialTheme.typography.bodySmall,
            color = ChildooColors.CoralDeep,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        val rows = listOf("123", "456", "789", " 0<")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    row.forEach { key ->
                        when (key) {
                            ' ' -> Spacer(Modifier.size(72.dp))
                            '<' -> Box(
                                Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = entered.isNotEmpty()) { entered = entered.dropLast(1) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = stringResource(R.string.pin_delete), tint = ChildooColors.Navy)
                            }
                            else -> Box(
                                Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(1.5.dp, ChildooColors.Line, CircleShape)
                                    .clickable(enabled = enabled) { press(key) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(key.toString(), style = MaterialTheme.typography.headlineMedium, color = ChildooColors.Navy)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        footer()
    }
}

/** Create a PIN: enter, then confirm. */
@Composable
fun PinSetup(onPinChosen: (String) -> Unit, modifier: Modifier = Modifier) {
    var first by rememberSaveable { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var shakeKey by remember { mutableIntStateOf(0) }
    val mismatch = stringResource(R.string.pin_mismatch)

    PinPad(
        title = stringResource(if (first == null) R.string.pin_create_title else R.string.pin_confirm_title),
        subtitle = stringResource(if (first == null) R.string.pin_create_body else R.string.pin_confirm_body),
        error = error,
        shakeKey = shakeKey,
        enabled = true,
        modifier = modifier,
        onComplete = { pin ->
            val chosen = first
            when {
                chosen == null -> {
                    first = pin
                    error = null
                }
                chosen == pin -> onPinChosen(pin)
                else -> {
                    first = null
                    error = mismatch
                    shakeKey++
                }
            }
        },
    )
}

/**
 * Stands between a child and the parent settings. Five wrong tries lock the pad for 30 seconds.
 * "Forgot PIN" asks a grown-up multiplication question, then lets the parent pick a new PIN.
 */
@Composable
fun ParentGate(repo: SettingsRepository, onUnlocked: () -> Unit) {
    val scope = rememberCoroutineScope()
    var failures by rememberSaveable { mutableIntStateOf(0) }
    var lockedUntil by rememberSaveable { mutableLongStateOf(0L) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var shakeKey by remember { mutableIntStateOf(0) }
    var mode by rememberSaveable { mutableStateOf(GateMode.Enter) }
    val wrong = stringResource(R.string.pin_wrong)

    LaunchedEffect(lockedUntil) {
        while (System.currentTimeMillis() < lockedUntil) {
            now = System.currentTimeMillis()
            delay(500)
        }
        now = System.currentTimeMillis()
    }
    val locked = now < lockedUntil
    val secondsLeft = ((lockedUntil - now) / 1000L + 1).toInt()

    Box(
        Modifier
            .fillMaxSize()
            .background(ChildooColors.Cream)
            .safeDrawingPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (mode) {
            GateMode.Enter -> PinPad(
                title = stringResource(R.string.pin_gate_title),
                subtitle = stringResource(R.string.pin_gate_body),
                error = when {
                    locked -> stringResource(R.string.pin_locked, secondsLeft)
                    failures > 0 -> wrong
                    else -> null
                },
                shakeKey = shakeKey,
                enabled = !locked,
                onComplete = { pin ->
                    scope.launch {
                        if (repo.checkPin(pin)) {
                            failures = 0
                            onUnlocked()
                        } else {
                            failures++
                            shakeKey++
                            if (failures % 5 == 0) {
                                lockedUntil = System.currentTimeMillis() + 30_000L
                            }
                        }
                    }
                },
                footer = {
                    Text(
                        stringResource(R.string.pin_forgot),
                        style = MaterialTheme.typography.labelLarge,
                        color = ChildooColors.Mint,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { mode = GateMode.Recover }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                },
            )
            GateMode.Recover -> GrownUpCheck(
                onPassed = { mode = GateMode.Reset },
                onCancel = { mode = GateMode.Enter },
            )
            GateMode.Reset -> PinSetup(
                onPinChosen = { pin ->
                    scope.launch {
                        repo.setPin(pin)
                        onUnlocked()
                    }
                },
            )
        }
    }
}

private enum class GateMode { Enter, Recover, Reset }

/** A question most young children can't answer: a two-digit by one-digit multiplication. */
@Composable
private fun GrownUpCheck(onPassed: () -> Unit, onCancel: () -> Unit) {
    val a = rememberSaveable { Random.nextInt(13, 20) }
    val b = rememberSaveable { Random.nextInt(6, 10) }
    var answer by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.grownup_title), style = MaterialTheme.typography.headlineSmall, color = ChildooColors.Navy, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.grownup_body), style = MaterialTheme.typography.bodyMedium, color = ChildooColors.InkSoft, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Text("$a × $b = ${answer.ifEmpty { "?" }}", style = MaterialTheme.typography.displaySmall, color = ChildooColors.Navy)
        Spacer(Modifier.height(8.dp))
        Text(if (error) stringResource(R.string.grownup_wrong) else " ", style = MaterialTheme.typography.bodySmall, color = ChildooColors.CoralDeep)
        Spacer(Modifier.height(12.dp))
        val rows = listOf("123", "456", "789", "<0=")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEach { key ->
                        val label = when (key) {
                            '<' -> "⌫"
                            '=' -> "OK"
                            else -> key.toString()
                        }
                        Box(
                            Modifier
                                .size(width = 80.dp, height = 60.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (key == '=') ChildooColors.Gold else Color.White)
                                .border(1.5.dp, ChildooColors.Line, RoundedCornerShape(18.dp))
                                .clickable {
                                    when (key) {
                                        '<' -> answer = answer.dropLast(1)
                                        '=' -> if (answer.toIntOrNull() == a * b) onPassed() else {
                                            error = true
                                            answer = ""
                                        }
                                        else -> if (answer.length < 4) answer += key
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(label, style = MaterialTheme.typography.titleLarge, color = ChildooColors.Navy)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.cancel),
            style = MaterialTheme.typography.labelLarge,
            color = ChildooColors.InkSoft,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onCancel)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
        Spacer(Modifier.width(1.dp))
    }
}
