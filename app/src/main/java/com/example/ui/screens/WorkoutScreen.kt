package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TrainingProgram
import com.example.data.repository.WorkoutRepository
import com.example.ui.WorkoutViewModel
import com.example.ui.components.SensorCalibrationDialog
import com.example.ui.components.ShareCardType
import com.example.ui.components.ShareProgressDialog
import com.example.ui.theme.AquaBlue
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricEmerald
import com.example.ui.theme.FireOrange
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.TextGray
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val totalPushups by viewModel.totalPushUps.collectAsState()
    val weeklyPushups by viewModel.weeklyPushUps.collectAsState()
    val streak by viewModel.currentStreak.collectAsState()
    val best by viewModel.personalBest.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val workoutState by viewModel.workoutState.collectAsState()

    val targetWeekly = preferences?.weeklyTarget ?: 100
    val progressPercent = if (targetWeekly > 0) {
        (weeklyPushups.toFloat() / targetWeekly.toFloat()).coerceIn(0f, 1f)
    } else 0f

    var showManualDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }

    val preferredLevel = preferences?.experienceLevel ?: "Beginner"
    val orderedPrograms = remember(preferredLevel) {
        TrainingProgram.programs.sortedBy {
            if (it.difficulty.equals(preferredLevel, ignoreCase = true)) 0 else 1
        }
    }

    Box(modifier = modifier.fillMaxSize().background(PitchBlack)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                // App Welcome Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "PUSHUP",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = TextWhite,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PRO",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = NeonGreen,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = "PROGRESSIVE OVERLOAD & TRACKING",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGray,
                            letterSpacing = 1.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Optional Share Progress button
                        IconButton(
                            onClick = { showShareDialog = true },
                            modifier = Modifier
                                .background(CardSurfaceElevated, CircleShape)
                                .size(48.dp)
                                .testTag("share_progress_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Progress",
                                tint = AquaBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Voice coach quick indicator
                        IconButton(
                            onClick = {
                                preferences?.let {
                                    val nextState = !it.voiceCoachEnabled
                                    viewModel.updatePreferences(
                                        weeklyTarget = it.weeklyTarget,
                                        voiceEnabled = nextState,
                                        notificationsEnabled = it.notificationsEnabled
                                    )
                                    if (nextState) {
                                        viewModel.speak("Voice coach enabled")
                                    }
                                }
                            },
                            modifier = Modifier
                                .background(CardSurfaceElevated, CircleShape)
                                .size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (preferences?.voiceCoachEnabled == true) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = "Voice Coach Toggle",
                                tint = if (preferences?.voiceCoachEnabled == true) NeonGreen else TextMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Stat Cards Grid Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatMetricCard(
                        title = "LIFETIME",
                        value = "$totalPushups",
                        unit = "reps",
                        icon = Icons.Default.FitnessCenter,
                        tint = NeonGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        title = "STREAK",
                        value = "$streak",
                        unit = "days",
                        icon = Icons.Default.DirectionsRun,
                        tint = FireOrange,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        title = "RECORD",
                        value = "$best",
                        unit = "max reps",
                        icon = Icons.Default.CheckCircle,
                        tint = AquaBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Weekly Goal Indicator Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color.White.copy(0.08f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier.size(170.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Background circle style track
                                drawCircle(
                                    color = CardSurfaceElevated,
                                    radius = size.minDimension / 2.0f - 10.dp.toPx(),
                                    style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                                )
                                // Highlighted path element representing complete percentage
                                if (progressPercent > 0f) {
                                    drawArc(
                                        brush = Brush.sweepGradient(
                                            listOf(NeonGreen, AquaBlue, NeonGreen)
                                        ),
                                        startAngle = -90f,
                                        sweepAngle = progressPercent * 360f,
                                        useCenter = false,
                                        topLeft = Offset(10.dp.toPx(), 10.dp.toPx()),
                                        size = Size(size.width - 20.dp.toPx(), size.height - 20.dp.toPx()),
                                        style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "THIS WEEK",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = AquaBlue,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "$weeklyPushups",
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextWhite,
                                    lineHeight = 44.sp
                                )
                                Text(
                                    text = "OF $targetWeekly GOAL",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextGray,
                                    letterSpacing = 1.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .background(NeonGreen.copy(0.15f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${(progressPercent * 100).toInt()}% COMPLETED",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = NeonGreen
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val remaining = targetWeekly - weeklyPushups
                        val messageStr = if (remaining > 0) {
                            "$remaining push-ups remaining in the last 7 days"
                        } else {
                            "Weekly goal achieved! Ahead by ${-remaining} reps"
                        }
                        Text(
                            text = messageStr,
                            color = if (remaining <= 0) NeonGreen else AquaBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Interactive Launch actions Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.startManualWorkout() },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(56.dp)
                            .testTag("start_workout_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            contentColor = Color(0xFF003818)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("START SESSION", fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }

                    OutlinedButton(
                        onClick = { showManualDialog = true },
                        modifier = Modifier
                            .weight(1.0f)
                            .height(56.dp)
                            .testTag("log_offline_button"),
                        border = BorderStroke(1.5.dp, Color.White.copy(0.2f)),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite)
                    ) {
                        Text("Log Offline", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // Form Pro Tip Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(0.06f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(AquaBlue.copy(0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = AquaBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "COACH TIP",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = AquaBlue,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Keep elbows at 45°, core braced, and touch chest to floor for full pectoral recruitment.",
                                fontSize = 11.sp,
                                color = TextGray,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            // Quick Guides / Built-in Guided Programs Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TRAINING PROGRAMS",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = TextWhite,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${TrainingProgram.programs.size} PRESETS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGray
                    )
                }
            }

            items(orderedPrograms) { program ->
                TrainingProgramItemCard(
                    program = program,
                    isRecommended = program.difficulty.equals(preferredLevel, ignoreCase = true),
                    onStart = { viewModel.startProgramWorkout(program) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp)) // padding for navigation
            }
        }

        // Dialog for Manual Logging (offline logging)
        if (showManualDialog) {
            ManualLoggingDialog(
                onDismiss = { showManualDialog = false },
                onSave = { reps, sets ->
                    viewModel.logManualCount(reps, sets)
                    showManualDialog = false
                }
            )
        }

        if (showShareDialog) {
            ShareProgressDialog(
                weeklyPushUps = weeklyPushups,
                weeklyTarget = targetWeekly,
                streakDays = streak,
                personalBest = best,
                initialType = ShareCardType.WEEKLY,
                onShareTriggered = { viewModel.recordShareInitiated() },
                onDismiss = { showShareDialog = false }
            )
        }

        // Immersive Live Active Session Screen Overlay
        AnimatedVisibility(
            visible = (workoutState != "IDLE"),
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            WorkoutSessionOverlay(viewModel = viewModel)
        }
    }
}

@Composable
fun StatMetricCard(
    title: String,
    value: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(0.06f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(tint.copy(0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = TextWhite
            )
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextGray,
                letterSpacing = 0.5.sp
            )
            Text(
                text = unit,
                fontSize = 9.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
fun TrainingProgramItemCard(
    program: TrainingProgram,
    isRecommended: Boolean = false,
    onStart: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (isRecommended) NeonGreen.copy(0.35f) else Color.White.copy(0.06f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = program.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val badgeColor = when (program.difficulty) {
                            "Beginner" -> NeonGreen
                            "Intermediate" -> AquaBlue
                            else -> FireOrange
                        }
                        Box(
                            modifier = Modifier
                                .background(badgeColor.copy(0.15f), CircleShape)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isRecommended) "START HERE" else program.difficulty.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = badgeColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = program.description,
                        fontSize = 12.sp,
                        color = TextGray,
                        lineHeight = 16.sp
                    )
                }

                IconButton(
                    onClick = onStart,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .background(NeonGreen, CircleShape)
                        .size(44.dp)
                        .testTag("launch_program_${program.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.NavigateNext,
                        contentDescription = "Start Program",
                        tint = Color(0xFF003818),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color.White.copy(0.06f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${program.sets.size} sets: ${program.sets.joinToString(" • ")} reps",
                    fontSize = 11.sp,
                    color = NeonGreen,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Rest: ${program.restTimeSeconds}s",
                    fontSize = 11.sp,
                    color = TextGray,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ManualLoggingDialog(
    onDismiss: () -> Unit,
    onSave: (reps: Int, sets: Int) -> Unit
) {
    var repsInput by remember { mutableStateOf("") }
    var setsInput by remember { mutableStateOf("3") }
    var errorText by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CardSurfaceElevated,
            border = BorderStroke(1.dp, NeonGreen.copy(0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Log Offline Workout",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = TextWhite,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Quickly add reps from a session completed away from the phone.",
                    fontSize = 12.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Quick preset buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15, 25, 50, 100).forEach { preset ->
                        OutlinedButton(
                            onClick = { repsInput = preset.toString() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (repsInput == preset.toString()) NeonGreen else Color.White.copy(0.15f))
                        ) {
                            Text(
                                text = "$preset",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (repsInput == preset.toString()) NeonGreen else TextWhite
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = repsInput,
                    onValueChange = { repsInput = it; errorText = "" },
                    label = { Text("Total Push-ups Completed") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = Color.White.copy(0.15f),
                        focusedLabelColor = NeonGreen
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("manual_reps_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = setsInput,
                    onValueChange = { setsInput = it; errorText = "" },
                    label = { Text("Number of Sets") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = Color.White.copy(0.15f),
                        focusedLabelColor = NeonGreen
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("manual_sets_input")
                )

                val previewReps = repsInput.toIntOrNull()
                val previewSets = setsInput.toIntOrNull()
                if (previewReps != null && previewReps > 0 && previewSets != null && previewSets > 0) {
                    val breakdown = WorkoutRepository.splitRepsAcrossSets(previewReps, previewSets)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Set breakdown (${breakdown.size} sets): ${breakdown.joinToString(" • ")} reps",
                        color = NeonGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }

                if (errorText.isNotEmpty()) {
                    Text(
                        text = errorText,
                        color = FireOrange,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(0.2f))
                    ) {
                        Text("Cancel", color = TextWhite)
                    }

                    Button(
                        onClick = {
                            val reps = repsInput.toIntOrNull()
                            val sets = setsInput.toIntOrNull()
                            when {
                                reps == null || reps <= 0 || sets == null || sets <= 0 -> {
                                    errorText = "Please enter valid numbers higher than 0."
                                }
                                sets > reps -> {
                                    errorText = "Number of sets cannot exceed total push-ups."
                                }
                                else -> {
                                    onSave(reps, sets)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("save_manual_log")
                    ) {
                        Text("Save Workout", color = Color(0xFF003818), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutSessionOverlay(
    viewModel: WorkoutViewModel
) {
    val workoutState by viewModel.workoutState.collectAsState()
    val activeProgram by viewModel.activeProgram.collectAsState()
    val activeSets by viewModel.activeSets.collectAsState()
    val completedReps by viewModel.completedReps.collectAsState()
    val currentSetIndex by viewModel.currentSetIndex.collectAsState()
    val currentRepCount by viewModel.currentRepCount.collectAsState()
    val restTimeLeft by viewModel.restTimeLeft.collectAsState()
    val handsFreeCounting by viewModel.isProximityCountingEnabled.collectAsState()
    val showCalibrationDialog by viewModel.showSensorCalibrationDialog.collectAsState()
    val isSensorNear by viewModel.isSensorNear.collectAsState()
    val calibrationStep by viewModel.calibrationStep.collectAsState()
    val weeklyPushups by viewModel.weeklyPushUps.collectAsState()
    val streak by viewModel.currentStreak.collectAsState()
    val personalBest by viewModel.personalBest.collectAsState()
    val preferences by viewModel.preferences.collectAsState()

    var showExitDialog by remember { mutableStateOf(false) }
    var showShareResultDialog by remember { mutableStateOf(false) }

    // Intercept Back button during workout to prevent losing progress
    BackHandler(enabled = true) {
        showExitDialog = true
    }

    // Pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = PitchBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Overlay Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = activeProgram?.name ?: "MANUAL SESSION",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonGreen
                    )
                    Text(
                        text = if (workoutState == "REST") "REST INTERVAL ACTIVE" else "SET ${currentSetIndex + 1} OF ${activeSets.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextGray
                    )
                }

                IconButton(
                    onClick = { showExitDialog = true },
                    modifier = Modifier
                        .background(CardSurfaceElevated, CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Abort Workout",
                        tint = FireOrange,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Sets Progress Indicator Strip
            if (activeSets.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    activeSets.forEachIndexed { idx, targetReps ->
                        val isDone = idx < currentSetIndex
                        val isCurrent = idx == currentSetIndex
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .background(
                                    when {
                                        isDone -> NeonGreen
                                        isCurrent -> if (workoutState == "REST") FireOrange else NeonGreen.copy(0.3f)
                                        else -> CardSurfaceElevated
                                    },
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (isCurrent) (if (workoutState == "REST") FireOrange else NeonGreen) else Color.Transparent
                                    ),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isDone) "✓" else "S${idx + 1}: $targetReps",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDone) Color(0xFF003818) else TextWhite
                            )
                        }
                    }
                }
            }

            // Central Interactive Ring
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(CircleShape)
                    .background(DarkSurface)
                    .border(
                        BorderStroke(
                            6.dp,
                            when (workoutState) {
                                "REST" -> FireOrange
                                "FINISHED" -> NeonGreen
                                else -> NeonGreen
                            }
                        ),
                        CircleShape
                    )
                    .clickable(enabled = (workoutState == "RUNNING")) {
                        viewModel.incrementRep()
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (workoutState == "REST") {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = FireOrange,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$restTimeLeft",
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Black,
                            color = FireOrange,
                            lineHeight = 72.sp
                        )
                        Text(
                            text = "SECONDS REST",
                            fontSize = 11.sp,
                            color = TextGray,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    } else if (workoutState == "RUNNING") {
                        val currentTarget = if (activeSets.isNotEmpty() && currentSetIndex < activeSets.size) activeSets[currentSetIndex] else 10
                        Text(
                            text = "$currentRepCount",
                            fontSize = 86.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonGreen,
                            lineHeight = 86.sp,
                            modifier = Modifier.scale(pulseScale)
                        )
                        Text(
                            text = "TARGET: $currentTarget REPS",
                            fontSize = 12.sp,
                            color = TextWhite,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "TAP CIRCLE TO COUNT",
                            fontSize = 10.sp,
                            color = NeonGreen.copy(0.8f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    } else {
                        // Finished workout screen inside ring
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "CRUSHED!",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonGreen
                        )
                        Text(
                            text = "${completedReps.sum()} Total Reps",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    }
                }
            }

            // Target adjustment & Proximity toggle during workout
            AnimatedVisibility(visible = (workoutState == "RUNNING")) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick target adjustment stepper
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardSurface, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Adjust Set Target:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGray
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.updateCurrentSetTarget(-5) },
                                modifier = Modifier.size(32.dp).background(CardSurfaceElevated, CircleShape)
                            ) {
                                Text("-5", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { viewModel.updateCurrentSetTarget(+5) },
                                modifier = Modifier.size(32.dp).background(CardSurfaceElevated, CircleShape)
                            ) {
                                Text("+5", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                            }
                        }
                    }

                    // Hands-Free proximity auto-counter card with sensor availability & calibration
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (handsFreeCounting) NeonGreen.copy(0.3f) else Color.White.copy(0.06f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (viewModel.hasProximitySensor) Icons.Default.Sensors else Icons.Default.TouchApp,
                                    contentDescription = "Sensor config",
                                    tint = if (handsFreeCounting) NeonGreen else TextGray,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (viewModel.hasProximitySensor) "Hands-Free Sensor" else "Manual Tap Counter",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (handsFreeCounting) NeonGreen else TextWhite
                                    )
                                    Text(
                                        text = when {
                                            !viewModel.hasProximitySensor -> "Proximity sensor unavailable — tap circle or +1 to count"
                                            handsFreeCounting -> "Active • Counts 1 rep per full down-and-up"
                                            else -> "Place phone under chest; calibrates before counting"
                                        },
                                        fontSize = 10.sp,
                                        color = TextGray
                                    )
                                }
                            }

                            if (viewModel.hasProximitySensor) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (handsFreeCounting) {
                                        TextButton(
                                            onClick = { viewModel.openSensorCalibration() },
                                            modifier = Modifier.testTag("calibrate_sensor_button")
                                        ) {
                                            Text("Test", color = AquaBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Switch(
                                        checked = handsFreeCounting,
                                        onCheckedChange = { viewModel.toggleProximityCounting(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFF003818),
                                            checkedTrackColor = NeonGreen
                                        ),
                                        modifier = Modifier.testTag("proximity_switch")
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .background(AquaBlue.copy(0.15f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "TAP MODE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = AquaBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Actions panel based on State
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (workoutState == "RUNNING") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.decrementRep() },
                            modifier = Modifier
                                .background(CardSurfaceElevated, CircleShape)
                                .size(52.dp)
                                .testTag("rep_minus_one")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Minus One", tint = Color.White)
                        }

                        Button(
                            onClick = { viewModel.completeCurrentSet() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonGreen,
                                contentColor = Color(0xFF003818)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("complete_set_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("COMPLETE SET", fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }

                        IconButton(
                            onClick = { viewModel.incrementRep() },
                            modifier = Modifier
                                .background(CardSurfaceElevated, CircleShape)
                                .size(52.dp)
                                .testTag("rep_plus_one")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Plus One", tint = Color.White)
                        }
                    }

                    // Secondary action buttons (Finish Early & Save, or Append Set)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.finishWorkout() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, AquaBlue.copy(0.5f))
                        ) {
                            Text("Finish & Save", color = AquaBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        if (activeProgram == null) {
                            OutlinedButton(
                                onClick = { viewModel.appendManualSet(10) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, NeonGreen.copy(0.4f))
                            ) {
                                Text("+ Extra Set (10)", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else if (workoutState == "REST") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.addRestTime(15) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, FireOrange.copy(0.6f))
                        ) {
                            Text("+15s Rest", fontWeight = FontWeight.Bold, color = FireOrange, fontSize = 13.sp)
                        }

                        Button(
                            onClick = { viewModel.skipRest() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FireOrange,
                                contentColor = PitchBlack
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(52.dp)
                                .testTag("skip_rest_button")
                        ) {
                            Text("SKIP REST", fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                    }
                } else if (workoutState == "FINISHED") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (completedReps.sum() > 0) {
                            OutlinedButton(
                                onClick = { showShareResultDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, AquaBlue.copy(0.6f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("share_finished_workout")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = AquaBlue, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SHARE RESULT / CHALLENGE FRIEND", color = AquaBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Button(
                            onClick = { viewModel.cancelWorkout() }, // close overlay
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonGreen,
                                contentColor = Color(0xFF003818)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("dismiss_workout_overlay")
                        ) {
                            Text("AWESOME! LOGGED TO HISTORY", fontWeight = FontWeight.Black, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }

    if (showCalibrationDialog) {
        SensorCalibrationDialog(
            calibrationStep = calibrationStep,
            isSensorNear = isSensorNear,
            onDone = { viewModel.dismissSensorCalibration() },
            onUseManualFallback = { viewModel.switchToManualTapCounting() }
        )
    }

    if (showShareResultDialog) {
        ShareProgressDialog(
            weeklyPushUps = weeklyPushups,
            weeklyTarget = preferences?.weeklyTarget ?: 100,
            streakDays = streak,
            personalBest = personalBest,
            lastSessionReps = completedReps.sum(),
            initialType = ShareCardType.CHALLENGE,
            onShareTriggered = { viewModel.recordShareInitiated() },
            onDismiss = { showShareResultDialog = false }
        )
    }

    // Exit confirmation dialog to prevent accidental loss
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(text = "Exit Workout Session?", fontWeight = FontWeight.Bold, color = TextWhite)
            },
            text = {
                Text(
                    text = "You have active reps in progress. Would you like to finish and save this workout, or discard it?",
                    color = TextGray,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        viewModel.finishWorkout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    Text("Save & Finish", color = Color(0xFF003818), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showExitDialog = false
                            viewModel.cancelWorkout()
                        }
                    ) {
                        Text("Discard", color = FireOrange)
                    }
                    TextButton(onClick = { showExitDialog = false }) {
                        Text("Resume", color = TextWhite)
                    }
                }
            },
            containerColor = CardSurfaceElevated,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
