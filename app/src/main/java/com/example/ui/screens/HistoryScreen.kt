package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WorkoutSession
import com.example.ui.WorkoutViewModel
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.sessions.collectAsState()
    val totalPushups by viewModel.totalPushUps.collectAsState()
    val bestRecord by viewModel.personalBest.collectAsState()

    var sessionToDelete by remember { mutableStateOf<WorkoutSession?>(null) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(0) } // 0 = All, 1 = Past 7 Days, 2 = Past 30 Days

    val filteredSessions = remember(sessions, selectedFilter) {
        val now = System.currentTimeMillis()
        when (selectedFilter) {
            1 -> {
                val sevenDaysAgo = now - (7L * 24 * 60 * 60 * 1000)
                sessions.filter { it.timestamp >= sevenDaysAgo }
            }
            2 -> {
                val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)
                sessions.filter { it.timestamp >= thirtyDaysAgo }
            }
            else -> sessions
        }
    }

    val avgRepsPerSession = if (sessions.isNotEmpty()) totalPushups / sessions.size else 0
    val totalSetsCount = sessions.sumOf { it.setsCount }

    Box(modifier = modifier.fillMaxSize().background(PitchBlack)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // History page Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "WORKOUT LOGS",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = TextWhite,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "ANALYZE VOLUME & TRACK PROGRESSION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGray,
                        letterSpacing = 0.5.sp
                    )
                }

                // Reset action trigger
                if (sessions.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearConfirmation = true },
                        modifier = Modifier
                            .background(CardSurfaceElevated, CircleShape)
                            .size(40.dp)
                            .testTag("clear_history_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear History",
                            tint = FireOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (sessions.isEmpty()) {
                EmptyHistoryState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Glow Column bar graph item
                    item {
                        WorkoutChart(sessions = sessions)
                    }

                    // Summary performance 3-stat card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CardSurface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color.White.copy(0.06f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("LIFETIME REPS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextGray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("$totalPushups", fontSize = 20.sp, fontWeight = FontWeight.Black, color = NeonGreen)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("AVG / SESSION", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextGray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("$avgRepsPerSession", fontSize = 20.sp, fontWeight = FontWeight.Black, color = AquaBlue)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("BEST SESSION", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextGray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("$bestRecord", fontSize = 20.sp, fontWeight = FontWeight.Black, color = FireOrange)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("SETS LOGGED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextGray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("$totalSetsCount", fontSize = 20.sp, fontWeight = FontWeight.Black, color = TextWhite)
                                }
                            }
                        }
                    }

                    // Filter chips row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("All Workouts (${sessions.size})", "Past 7 Days", "Past 30 Days").forEachIndexed { index, label ->
                                FilterChip(
                                    selected = selectedFilter == index,
                                    onClick = { selectedFilter = index },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NeonGreen,
                                        selectedLabelColor = Color(0xFF003818),
                                        containerColor = CardSurface,
                                        labelColor = TextGray
                                    ),
                                    border = BorderStroke(1.dp, if (selectedFilter == index) NeonGreen else Color.White.copy(0.08f)),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "WORKOUT SESSIONS",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = TextWhite,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${filteredSessions.size} logged",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextGray
                            )
                        }
                    }

                    items(filteredSessions, key = { it.id }) { session ->
                        HistoryLogCard(
                            session = session,
                            onDelete = { sessionToDelete = session }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Delete single session confirmation dialog
        sessionToDelete?.let { session ->
            AlertDialog(
                onDismissRequest = { sessionToDelete = null },
                title = { Text("Delete Workout Session?", fontWeight = FontWeight.Bold, color = TextWhite) },
                text = {
                    Text(
                        text = "Are you sure you want to remove the session with ${session.totalPushUps} push-ups from ${session.formattedDate}? This cannot be undone.",
                        color = TextGray,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteLoggedSession(session)
                            sessionToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FireOrange)
                    ) {
                        Text("Delete", color = TextWhite, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { sessionToDelete = null }) {
                        Text("Cancel", color = TextWhite)
                    }
                },
                containerColor = CardSurfaceElevated,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Clear all history confirmation dialog
        if (showClearConfirmation) {
            AlertDialog(
                onDismissRequest = { showClearConfirmation = false },
                title = { Text("Clear All History?", fontWeight = FontWeight.Bold, color = TextWhite) },
                text = {
                    Text(
                        text = "This will erase all logged workouts and reset lifetime streak metrics. Are you sure?",
                        color = TextGray,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearHistory()
                            showClearConfirmation = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FireOrange)
                    ) {
                        Text("Erase Everything", color = TextWhite, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirmation = false }) {
                        Text("Keep My Data", color = TextWhite)
                    }
                },
                containerColor = CardSurfaceElevated,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun WorkoutChart(
    sessions: List<WorkoutSession>,
    modifier: Modifier = Modifier
) {
    val sdf = SimpleDateFormat("E", Locale.getDefault())
    val keySdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

    // List of past 7 days descending to ascending
    val pastDays = (0..6).map { i ->
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, -i)
        c
    }.reversed()

    val dayLabels = pastDays.map { sdf.format(it.time) }
    val dayKeys = pastDays.map { keySdf.format(it.time) }

    val sessionSdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val pushupsPerDay = dayKeys.map { key ->
        sessions.filter { sessionSdf.format(Date(it.timestamp)) == key }.sumOf { it.totalPushUps }
    }

    val maxDaily = pushupsPerDay.maxOrNull()?.coerceAtLeast(20) ?: 20
    val total7DayPushups = pushupsPerDay.sum()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardSurface, RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PAST 7 DAYS VOLUME",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = TextWhite,
                letterSpacing = 0.5.sp
            )
            Box(
                modifier = Modifier
                    .background(NeonGreen.copy(0.12f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "$total7DayPushups REPS THIS WEEK",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = NeonGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val barCount = 7
            val spacing = 16.dp.toPx()
            val totalSpacing = spacing * (barCount - 1)
            val barWidth = (canvasWidth - totalSpacing) / barCount

            // Horizontal grid accents
            drawLine(
                color = Color.White.copy(0.06f),
                start = Offset(0f, canvasHeight * 0.5f),
                end = Offset(canvasWidth, canvasHeight * 0.5f),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color.White.copy(0.06f),
                start = Offset(0f, 0f),
                end = Offset(canvasWidth, 0f),
                strokeWidth = 1.dp.toPx()
            )

            for (i in 0 until barCount) {
                val reps = pushupsPerDay[i]
                val pct = reps.toFloat() / maxDaily.toFloat()
                val barHeight = (canvasHeight * pct).coerceAtLeast(6.dp.toPx())
                val xLeft = i * (barWidth + spacing)
                val yTop = canvasHeight - barHeight

                val isPeak = reps > 0 && reps == maxDaily

                val brush = Brush.verticalGradient(
                    colors = if (isPeak) {
                        listOf(FireOrange, FireOrange.copy(0.3f))
                    } else if (reps > 0) {
                        listOf(NeonGreen, NeonGreen.copy(0.2f))
                    } else {
                        listOf(Color.White.copy(0.08f), Color.White.copy(0.04f))
                    }
                )

                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(xLeft, yTop),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Weekday axis labels row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dayLabels.forEachIndexed { i, label ->
                val count = pushupsPerDay[i]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(36.dp)
                ) {
                    Text(
                        text = label.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGray
                    )
                    Text(
                        text = if (count > 0) "$count" else "-",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = if (count > 0) NeonGreen else TextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryLogCard(
    session: WorkoutSession,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(0.06f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(NeonGreen.copy(0.12f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EventNote,
                        contentDescription = "Workout log",
                        tint = NeonGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${session.totalPushUps} Push-ups",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        if (session.programId != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(AquaBlue.copy(0.15f), CircleShape)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "PROGRAM",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = AquaBlue
                                )
                            }
                        }
                    }
                    Text(
                        text = "${session.formattedDate} at ${session.formattedTime}",
                        fontSize = 11.sp,
                        color = TextGray
                    )
                    Text(
                        text = "${session.setsCount} sets • Reps: ${session.repsListString.replace(",", " • ")}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeonGreen.copy(0.9f)
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_log_${session.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.RemoveCircleOutline,
                    contentDescription = "Delete Session",
                    tint = FireOrange.copy(0.8f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyHistoryState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(CardSurface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = "No workouts logged yet",
                tint = NeonGreen.copy(alpha = 0.5f),
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Workouts Logged Yet",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = TextWhite
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Complete your first live training session or log offline reps to start visualizing volume, streaks, and personal records here!",
            fontSize = 12.sp,
            color = TextGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
            lineHeight = 18.sp
        )
    }
}
