package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.challenge.ThirtyDayChallengeManager
import com.example.data.challenge.ThirtyDayChallengeState
import com.example.ui.theme.AquaBlue
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.FireOrange
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextGray
import com.example.ui.theme.TextWhite

@Composable
fun ThirtyDayChallengeCard(
    challengeState: ThirtyDayChallengeState,
    onStartChallenge: (initialMaxReps: Int) -> Unit,
    onStartChallengeDay: () -> Unit,
    onRecordFinalRetest: (finalMaxReps: Int) -> Unit,
    onResetChallenge: () -> Unit
) {
    var showBaselineDialog by remember { mutableStateOf(false) }
    var showRetestDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            if (challengeState.isStarted) FireOrange.copy(0.4f) else NeonGreen.copy(0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                if (challengeState.isCompleted) NeonGreen.copy(0.15f)
                                else FireOrange.copy(0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (challengeState.isCompleted) Icons.Default.EmojiEvents else Icons.Default.Flag,
                            contentDescription = null,
                            tint = if (challengeState.isCompleted) NeonGreen else FireOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "30-DAY CHALLENGE",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = TextWhite,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = when {
                                !challengeState.isStarted -> "Day 1 baseline test to Day 30 peak"
                                challengeState.isCompleted -> "All 30 days completed!"
                                else -> "Day ${challengeState.currentDay} of 30 • ${challengeState.completedDays} done"
                            },
                            fontSize = 11.sp,
                            color = if (challengeState.isCompleted) NeonGreen else TextGray,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (challengeState.isStarted) {
                    IconButton(
                        onClick = { showResetConfirm = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("reset_30day_challenge_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset 30-Day Challenge",
                            tint = TextGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                !challengeState.isStarted -> {
                    Text(
                        text = "Enter your current single-set max reps to unlock a scaled 30-day plan with recovery days and a Day 30 final retest.",
                        fontSize = 12.sp,
                        color = TextGray,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { showBaselineDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("start_30day_challenge_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FireOrange,
                            contentColor = Color(0xFF2B0E00)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Start 30-Day Challenge",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }

                challengeState.isCompleted -> {
                    val baseline = challengeState.initialMaxReps
                    val finalMax = challengeState.finalMaxReps
                    val gain = finalMax - baseline

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("DAY 1 BASELINE", fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.Bold)
                            Text("$baseline reps", fontSize = 18.sp, color = TextWhite, fontWeight = FontWeight.Black)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PROGRESS", fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (gain >= 0) "+$gain reps" else "$gain reps",
                                fontSize = 16.sp,
                                color = if (gain >= 0) NeonGreen else AquaBlue,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("DAY 30 FINAL", fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (finalMax > 0) "$finalMax reps" else "Not tested",
                                fontSize = 18.sp,
                                color = NeonGreen,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showRetestDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("log_final_retest_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, NeonGreen.copy(0.5f))
                        ) {
                            Text(
                                text = if (finalMax > 0) "Update Retest" else "Log Final Max Test",
                                color = NeonGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = { showBaselineDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("restart_30day_challenge_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FireOrange,
                                contentColor = Color(0xFF2B0E00)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("New 30-Day Run", fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }

                else -> {
                    val activeDay = challengeState.currentDay
                    val isRecovery = activeDay % 5 == 0 && activeDay < 30
                    val todaySets = remember(challengeState.initialMaxReps, activeDay) {
                        ThirtyDayChallengeManager.buildSetsForDay(challengeState.initialMaxReps, activeDay)
                    }
                    val progress = (challengeState.completedDays / 30f).coerceIn(0f, 1f)

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = FireOrange,
                        trackColor = CardSurfaceElevated
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Day 1 Baseline: ${challengeState.initialMaxReps} max reps",
                            fontSize = 11.sp,
                            color = TextGray,
                            fontWeight = FontWeight.Medium
                        )
                        if (isRecovery) {
                            Box(
                                modifier = Modifier
                                    .background(AquaBlue.copy(0.16f), CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "RECOVERY DAY",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = AquaBlue
                                )
                            }
                        } else if (activeDay == 30) {
                            Box(
                                modifier = Modifier
                                    .background(NeonGreen.copy(0.16f), CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "FINAL PEAK DAY",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NeonGreen
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = Color.White.copy(0.06f)
                    )

                    Text(
                        text = "Today's target (${todaySets.sum()} total): ${todaySets.joinToString(" • ")} reps",
                        fontSize = 12.sp,
                        color = NeonGreen,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onStartChallengeDay,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("start_30day_workout_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonGreen,
                                contentColor = Color(0xFF003818)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Start Day $activeDay",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }

                        if (activeDay == 30) {
                            OutlinedButton(
                                onClick = { showRetestDialog = true },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("day30_retest_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, AquaBlue.copy(0.5f))
                            ) {
                                Text(
                                    text = "Log Max",
                                    color = AquaBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBaselineDialog) {
        MaxRepTestDialog(
            title = "Day 1 Baseline Test",
            subtitle = "How many clean push-ups can you do in one all-out set today? We'll scale Days 1–30 from your result.",
            confirmLabel = "Begin 30-Day Plan",
            presets = listOf(5, 10, 20, 35),
            onDismiss = { showBaselineDialog = false },
            onConfirm = { maxReps ->
                showBaselineDialog = false
                onStartChallenge(maxReps)
            }
        )
    }

    if (showRetestDialog) {
        MaxRepTestDialog(
            title = "Day 30 Final Max Test",
            subtitle = "Enter your max continuous push-ups after finishing the 30-day program to compare against your Day 1 baseline (${challengeState.initialMaxReps} reps).",
            confirmLabel = "Save Final Score",
            presets = listOf(15, 30, 50, 75),
            onDismiss = { showRetestDialog = false },
            onConfirm = { finalReps ->
                showRetestDialog = false
                onRecordFinalRetest(finalReps)
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset 30-Day Challenge?", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Your workout history stays saved, and you can start a fresh 30-day run with a new baseline test.",
                    color = TextGray,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        onResetChallenge()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FireOrange)
                ) {
                    Text("Reset", color = Color(0xFF2B0E00), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel", color = TextWhite)
                }
            },
            containerColor = CardSurfaceElevated,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun MaxRepTestDialog(
    title: String,
    subtitle: String,
    confirmLabel: String,
    presets: List<Int>,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var input by remember { mutableStateOf(presets.getOrElse(1) { 10 }.toString()) }
    var errorText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CardSurfaceElevated,
            border = BorderStroke(1.dp, FireOrange.copy(0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = TextWhite,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        OutlinedButton(
                            onClick = {
                                input = preset.toString()
                                errorText = ""
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.dp,
                                if (input == preset.toString()) NeonGreen else Color.White.copy(0.15f)
                            )
                        ) {
                            Text(
                                text = "$preset",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (input == preset.toString()) NeonGreen else TextWhite
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        errorText = ""
                    },
                    label = { Text("Max Push-ups in 1 Set") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = Color.White.copy(0.15f),
                        focusedLabelColor = NeonGreen
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("challenge_max_reps_input")
                )

                if (errorText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = errorText, color = FireOrange, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(0.2f))
                    ) {
                        Text("Cancel", color = TextWhite)
                    }

                    Button(
                        onClick = {
                            val parsed = input.toIntOrNull()
                            if (parsed == null || parsed <= 0) {
                                errorText = "Enter at least 1 push-up."
                            } else {
                                onConfirm(parsed)
                            }
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .testTag("confirm_challenge_max_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(confirmLabel, color = Color(0xFF003818), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
