package com.childoo.challenge

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.childoo.designsystem.ChildooColors
import com.childoo.designsystem.ChildooTheme
import com.childoo.designsystem.ChunkySurface
import com.childoo.designsystem.Nuzo
import com.childoo.designsystem.NuzoMood
import com.childoo.designsystem.StarrySky
import com.childoo.designsystem.localDigits
import kotlinx.coroutines.delay

/** Candy colours for the four answers: face + lip. Fixed order so each slot has a stable colour. */
private val TileColors = listOf(
    ChildooColors.Coral to ChildooColors.CoralDeep,
    ChildooColors.Sky to ChildooColors.SkyDeep,
    ChildooColors.MintLight to ChildooColors.Mint,
    ChildooColors.Lilac to ChildooColors.LilacDeep,
)

/**
 * The short challenge shown over the locked app.
 *
 * Design rules (from the product brief): effort is praised, mistakes are never punished.
 * A wrong answer is greyed out and Nuzo says "let's try another way", so every child always
 * finishes. There is no timer pressure and nothing is lost by guessing.
 */
@Composable
fun ChallengeScreen(
    onSolved: () -> Unit,
    rounds: Int = 3,
    modifier: Modifier = Modifier,
) {
    ChildooTheme {
        var round by remember { mutableIntStateOf(0) }
        var question by remember { mutableStateOf(MathQuestion.generate()) }
        var wrong by remember { mutableStateOf(setOf<Int>()) }
        var solvedThisRound by remember { mutableStateOf(false) }
        var oopsPulse by remember { mutableIntStateOf(0) }
        var finished by remember { mutableStateOf(false) }

        val mood = when {
            finished || solvedThisRound -> NuzoMood.Cheer
            oopsPulse > 0 -> NuzoMood.Oops
            else -> NuzoMood.Thinking
        }

        // "Oops" face lasts a moment, then Nuzo goes back to thinking with the child.
        LaunchedEffect(oopsPulse) {
            if (oopsPulse == 0) return@LaunchedEffect
            delay(1_100)
            oopsPulse = 0
        }

        LaunchedEffect(solvedThisRound) {
            if (!solvedThisRound) return@LaunchedEffect
            delay(900)
            if (round + 1 >= rounds) {
                finished = true
            } else {
                round += 1
                question = MathQuestion.generate()
                wrong = emptySet()
                solvedThisRound = false
            }
        }

        LaunchedEffect(finished) {
            if (!finished) return@LaunchedEffect
            delay(2_400)
            onSolved()
        }

        StarrySky(modifier) {
            AnimatedContent(
                targetState = finished,
                transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.9f)) togetherWith fadeOut() },
                label = "phase",
            ) { done ->
                if (done) {
                    Celebration()
                } else {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        TopBar(rounds = rounds, done = round + if (solvedThisRound) 1 else 0)
                        Spacer(Modifier.weight(0.6f))

                        Nuzo(Modifier.width(132.dp), mood = mood)
                        Spacer(Modifier.height(14.dp))
                        SpeechBubble {
                            Text(
                                text = localDigits("${question.prompt} = ?"),
                                // Equations read left-to-right in Persian schoolbooks too.
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontSize = 44.sp,
                                    lineHeight = 52.sp,
                                    textDirection = TextDirection.Ltr,
                                ),
                                color = ChildooColors.Navy,
                                textAlign = TextAlign.Center,
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(
                                when {
                                    solvedThisRound -> R.string.challenge_correct
                                    mood == NuzoMood.Oops -> R.string.challenge_try_again
                                    else -> R.string.challenge_hint
                                },
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (solvedThisRound) ChildooColors.Gold else Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                        )

                        Spacer(Modifier.weight(1f))

                        key(question) {
                            AnswerGrid(
                                options = question.options,
                                answer = question.answer,
                                wrong = wrong,
                                solved = solvedThisRound,
                                onPick = { option ->
                                    if (solvedThisRound || option in wrong) return@AnswerGrid
                                    if (option == question.answer) {
                                        solvedThisRound = true
                                    } else {
                                        wrong = wrong + option
                                        oopsPulse += 1
                                    }
                                },
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(rounds: Int, done: Int) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .background(Color.White.copy(alpha = 0.12f), CircleShape)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Bolt, contentDescription = null, tint = ChildooColors.Gold, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.challenge_title),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
            )
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(rounds) { i ->
                val filled = i < done
                val scale by animateFloatAsState(
                    targetValue = if (filled) 1.15f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy),
                    label = "star",
                )
                Icon(
                    Icons.Rounded.Star,
                    contentDescription = null,
                    tint = if (filled) ChildooColors.Gold else Color.White.copy(alpha = 0.22f),
                    modifier = Modifier.size(28.dp).scale(scale),
                )
            }
        }
    }
}

/** White rounded bubble with a small tail pointing up at Nuzo. */
@Composable
private fun SpeechBubble(content: @Composable () -> Unit) {
    val tail = GenericShape { size, _ ->
        moveTo(0f, size.height)
        lineTo(size.width / 2f, 0f)
        lineTo(size.width, size.height)
        close()
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(width = 28.dp, height = 12.dp).background(Color.White, tail))
        Box(
            Modifier
                .background(Color.White, RoundedCornerShape(28.dp))
                .padding(horizontal = 32.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}

@Composable
private fun AnswerGrid(
    options: List<Int>,
    answer: Int,
    wrong: Set<Int>,
    solved: Boolean,
    onPick: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        options.chunked(2).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEachIndexed { colIndex, option ->
                    val slot = rowIndex * 2 + colIndex
                    AnswerTile(
                        value = option,
                        colors = TileColors[slot % TileColors.size],
                        state = when {
                            solved && option == answer -> TileState.Correct
                            option in wrong -> TileState.Wrong
                            solved -> TileState.Faded
                            else -> TileState.Idle
                        },
                        onClick = { onPick(option) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private enum class TileState { Idle, Correct, Wrong, Faded }

@Composable
private fun AnswerTile(
    value: Int,
    colors: Pair<Color, Color>,
    state: TileState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(state) {
        if (state == TileState.Wrong) {
            for (x in listOf(16f, -14f, 10f, -6f, 0f)) shake.animateTo(x, spring(stiffness = 2_200f))
        }
    }
    val pop by animateFloatAsState(
        targetValue = if (state == TileState.Correct) 1.06f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pop",
    )
    val (face, lip) = when (state) {
        TileState.Correct -> ChildooColors.Gold to ChildooColors.GoldDeep
        TileState.Wrong -> Color.White.copy(alpha = 0.10f) to Color.White.copy(alpha = 0.05f)
        else -> colors
    }
    val textColor = when (state) {
        TileState.Correct -> ChildooColors.Navy
        TileState.Wrong -> Color.White.copy(alpha = 0.35f)
        else -> Color.White
    }

    ChunkySurface(
        onClick = onClick,
        enabled = state == TileState.Idle,
        face = face,
        lip = lip,
        lipHeight = 7.dp,
        dimWhenDisabled = false,
        shape = RoundedCornerShape(26.dp),
        modifier = modifier
            .height(104.dp)
            .graphicsLayer {
                translationX = shake.value
                scaleX = pop
                scaleY = pop
                alpha = if (state == TileState.Faded) 0.45f else 1f
            },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = localDigits(value.toString()),
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 40.sp),
                color = textColor,
            )
            if (state == TileState.Correct) {
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Rounded.Check, contentDescription = null, tint = ChildooColors.Navy, modifier = Modifier.size(30.dp))
            }
        }
    }
}

@Composable
private fun Celebration() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Nuzo(Modifier.width(200.dp), mood = NuzoMood.Cheer)
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.challenge_done),
                style = MaterialTheme.typography.headlineLarge,
                color = ChildooColors.Gold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.challenge_done_sub),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )
        }
        Confetti(Modifier.fillMaxSize())
    }
}
