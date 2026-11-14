package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChallengeGoal
import com.example.ui.WorkoutViewModel
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
fun AchievementsScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val goals by viewModel.goals.collectAsState()
    val totalPushups by viewModel.totalPushUps.collectAsState()
    val weeklyPushups by viewModel.weeklyPushUps.collectAsState()
    val streak by viewModel.currentStreak.collectAsState()
    val personalBest by viewModel.personalBest.collectAsState()
    val preferences by viewModel.preferences.collectAsState()

    var selectedFilter by remember { mutableStateOf(0) } // 0 = All, 1 = Unlocked, 2 = Locked
    var showShareDialog by remember { mutableStateOf(false) }

    val achievements = goals.filter { it.isCompleted }
    val displayGoals = remember(goals, selectedFilter) {
        when (selectedFilter) {
            1 -> goals.filter { it.isCompleted }
            2 -> goals.filter { !it.isCompleted }
            else -> goals
        }
    }

    val beginnerGoals = displayGoals.filter { it.category == "BEGINNER" }
    val intermediateGoals = displayGoals.filter { it.category == "INTERMEDIATE" }
    val advancedGoals = displayGoals.filter { it.category == "ADVANCED" }

    // Rank title based on lifetime push-ups
    val userRank = when {
        totalPushups >= 5000 -> "TITAN"
        totalPushups >= 2500 -> "CENTURION"
        totalPushups >= 1000 -> "COMMANDO"
        totalPushups >= 500 -> "IRON WARRIOR"
        totalPushups >= 100 -> "STRIKER"
        else -> "NOVICE RECRUIT"
    }

    Box(modifier = modifier.fillMaxSize().background(PitchBlack)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Screen Heading Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TROPHIES & RANKS",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = TextWhite,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "UNLOCK BADGES & ASCEND ATHLETIC TIERS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGray,
                        letterSpacing = 0.5.sp
                    )
                }

                IconButton(
                    onClick = { showShareDialog = true },
                    modifier = Modifier
                        .background(CardSurfaceElevated, CircleShape)
                        .size(48.dp)
                        .testTag("achievements_share_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Rank and Streak",
                        tint = AquaBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // User Rank Showcase Card
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .background(NeonGreen.copy(0.15f), CircleShape)
                                        .border(2.dp, NeonGreen, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = NeonGreen,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = "CURRENT DIVISION",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextGray,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = userRank,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TextWhite
                                    )
                                    Text(
                                        text = "$totalPushups lifetime push-ups",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NeonGreen
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .background(DarkSurface, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${achievements.size} / ${goals.size} Trophies",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AquaBlue
                                )
                            }
                        }
                    }
                }

                // Filter chips
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "All (${goals.size})",
                            "Unlocked (${achievements.size})",
                            "In Progress (${goals.size - achievements.size})"
                        ).forEachIndexed { index, label ->
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

                // Challenge Tiers list
                if (beginnerGoals.isNotEmpty()) {
                    item {
                        ChallengeTierHeader(title = "BEGINNER FOUNDATIONS TIER")
                    }
                    items(beginnerGoals, key = { it.id }) { goal ->
                        ChallengeGoalProgressCard(goal = goal)
                    }
                }

                if (intermediateGoals.isNotEmpty()) {
                    item {
                        ChallengeTierHeader(title = "INTERMEDIATE STANDARDS TIER")
                    }
                    items(intermediateGoals, key = { it.id }) { goal ->
                        ChallengeGoalProgressCard(goal = goal)
                    }
                }

                if (advancedGoals.isNotEmpty()) {
                    item {
                        ChallengeTierHeader(title = "ADVANCED MASTERY TIER")
                    }
                    items(advancedGoals, key = { it.id }) { goal ->
                        ChallengeGoalProgressCard(goal = goal)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        if (showShareDialog) {
            ShareProgressDialog(
                weeklyPushUps = weeklyPushups,
                weeklyTarget = preferences?.weeklyTarget ?: 100,
                streakDays = streak,
                personalBest = personalBest,
                initialType = ShareCardType.STREAK,
                onShareTriggered = { viewModel.recordShareInitiated() },
                onDismiss = { showShareDialog = false }
            )
        }
    }
}

@Composable
fun ChallengeTierHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        color = TextGray,
        letterSpacing = 0.7.sp,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
fun ChallengeGoalProgressCard(goal: ChallengeGoal) {
    val progressPct = if (goal.targetValue > 0) {
        (goal.currentValue.toFloat() / goal.targetValue.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val isCompleted = goal.isCompleted

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) CardSurface else DarkSurface
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (isCompleted) NeonGreen.copy(0.3f) else Color.White.copy(0.04f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        color = if (isCompleted) NeonGreen.copy(0.15f) else CardSurfaceElevated,
                        shape = CircleShape
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (isCompleted) NeonGreen else Color.White.copy(0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (goal.iconName) {
                        "star" -> Icons.Default.Star
                        "star_half" -> Icons.Default.StarHalf
                        "fitness_center" -> Icons.Default.FitnessCenter
                        "local_fire_department" -> Icons.Default.LocalFireDepartment
                        "workspace_premium" -> Icons.Default.WorkspacePremium
                        "military_tech" -> Icons.Default.MilitaryTech
                        "emoji_events" -> Icons.Default.EmojiEvents
                        "trending_up" -> Icons.Default.TrendingUp
                        else -> Icons.Default.Stars
                    },
                    contentDescription = null,
                    tint = if (isCompleted) NeonGreen else TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = goal.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) TextWhite else TextWhite.copy(0.85f)
                    )

                    if (isCompleted) {
                        Box(
                            modifier = Modifier
                                .background(NeonGreen.copy(0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "UNLOCKED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = NeonGreen
                            )
                        }
                    } else {
                        Text(
                            text = "${goal.currentValue} / ${goal.targetValue}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = goal.description,
                    fontSize = 11.sp,
                    color = TextGray,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progressPct },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isCompleted) NeonGreen else AquaBlue,
                    trackColor = CardSurfaceElevated
                )
            }
        }
    }
}
