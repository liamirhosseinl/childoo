package com.childoo.challenge

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val Navy = Color(0xFF1E2266)
private val Gold = Color(0xFFFFC145)
private val Cream = Color(0xFFFBF8F3)
private val Mint = Color(0xFF17917D)

/**
 * The 60-second challenge shown over the locked app.
 * The child answers [rounds] questions; a wrong answer never ends the challenge,
 * it only asks to try another way (effort praise, no shaming).
 */
@Composable
fun ChallengeScreen(
    onSolved: () -> Unit,
    rounds: Int = 3,
    modifier: Modifier = Modifier,
) {
    var round by remember { mutableIntStateOf(0) }
    var question by remember { mutableStateOf(MathQuestion.generate()) }
    var feedback by remember { mutableStateOf<Feedback?>(null) }
    var finished by remember { mutableStateOf(false) }
    var wrongPulse by remember { mutableIntStateOf(0) }
    val shake = remember { Animatable(0f) }

    LaunchedEffect(wrongPulse) {
        if (wrongPulse == 0) return@LaunchedEffect
        for (offset in listOf(18f, -18f, 10f, -10f, 0f)) {
            shake.animateTo(offset, spring(stiffness = 2_000f))
        }
    }

    LaunchedEffect(feedback) {
        if (feedback != Feedback.Correct) return@LaunchedEffect
        delay(600)
        if (round + 1 >= rounds) {
            finished = true
        } else {
            round += 1
            question = MathQuestion.generate()
            feedback = null
        }
    }

    LaunchedEffect(finished) {
        if (finished) {
            delay(1_200)
            onSolved()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Navy)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (finished) {
            Text(
                text = stringResource(R.string.challenge_done),
                color = Gold,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        } else Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.challenge_title),
                color = Cream,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            ProgressDots(total = rounds, done = round)
            Spacer(Modifier.height(8.dp))
            Text(
                text = question.prompt,
                color = Gold,
                fontSize = 64.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.graphicsLayer { translationX = shake.value },
            )
            Text(
                text = when (feedback) {
                    Feedback.Correct -> stringResource(R.string.challenge_correct)
                    Feedback.Wrong -> stringResource(R.string.challenge_try_again)
                    null -> stringResource(R.string.challenge_hint)
                },
                color = if (feedback == Feedback.Correct) Gold else Cream,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            question.options.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEach { option ->
                        Button(
                            onClick = {
                                if (feedback == Feedback.Correct) return@Button
                                if (option == question.answer) {
                                    feedback = Feedback.Correct
                                } else {
                                    feedback = Feedback.Wrong
                                    wrongPulse += 1
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Cream, contentColor = Navy),
                            modifier = Modifier.size(width = 132.dp, height = 88.dp),
                        ) {
                            Text(text = option.toString(), fontSize = 34.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressDots(total: Int, done: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(if (i < done) Mint else Cream.copy(alpha = 0.3f), CircleShape),
            )
        }
    }
}

private enum class Feedback { Correct, Wrong }
