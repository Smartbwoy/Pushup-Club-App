package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AquaBlue
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.FireOrange
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.TextGray
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

private data class StarterLevelOption(
    val level: String,
    val badge: String?,
    val subtitle: String,
    val suggestedWeeklyTarget: Int,
    val suggestedDays: Set<Int>,
    val accentColor: Color
)

@Composable
fun OnboardingScreen(
    onCompleteSetup: (level: String, weeklyTarget: Int, enabledDays: Set<Int>, startImmediately: Boolean) -> Unit,
    onSkipAndStartWorkout: () -> Unit
) {
    // Beginner-friendly option first and pre-selected
    val levelOptions = remember {
        listOf(
            StarterLevelOption(
                level = "Beginner",
                badge = "RECOMMENDED FIRST",
                subtitle = "Start steady with 5-rep sets & plenty of rest",
                suggestedWeeklyTarget = 100,
                suggestedDays = setOf(1, 3, 5), // Mon, Wed, Fri
                accentColor = NeonGreen
            ),
            StarterLevelOption(
                level = "Intermediate",
                badge = null,
                subtitle = "Can do 15+ clean reps per set",
                suggestedWeeklyTarget = 250,
                suggestedDays = setOf(1, 2, 4, 6),
                accentColor = AquaBlue
            ),
            StarterLevelOption(
                level = "Advanced",
                badge = null,
                subtitle = "Chasing 100-rep endurance & high volume",
                suggestedWeeklyTarget = 450,
                suggestedDays = setOf(1, 2, 3, 4, 5),
                accentColor = FireOrange
            )
        )
    }

    var selectedLevel by remember { mutableStateOf("Beginner") }
    var weeklyGoal by remember { mutableStateOf(100) }
    var selectedDays by remember { mutableStateOf(setOf(1, 3, 5)) }

    val dayLabels = remember {
        listOf(
            1 to "M",
            2 to "T",
            3 to "W",
            4 to "T",
            5 to "F",
            6 to "S",
            7 to "S"
        )
    }

    val goalPresets = remember { listOf(75, 100, 150, 250, 350, 500) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack)
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = PitchBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Top bar with quick Skip & Start Workout option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(NeonGreen.copy(0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PUSHUP PRO",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = TextWhite,
                        letterSpacing = 1.sp
                    )
                }

                TextButton(
                    onClick = onSkipAndStartWorkout,
                    modifier = Modifier.testTag("onboarding_skip_start")
                ) {
                    Text(
                        text = "Skip & Start Workout",
                        color = NeonGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Column {
                    Text(
                        text = "BUILD YOUR PLAN",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = TextWhite,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Track reps, follow a plan, and see weekly progress. You can change any of this later.",
                        fontSize = 13.sp,
                        color = TextGray,
                        lineHeight = 18.sp
                    )
                }

                // 1. Starting Level (Beginner First)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "1. STARTING LEVEL",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = TextGray,
                        letterSpacing = 0.8.sp
                    )

                    levelOptions.forEach { option ->
                        val isSelected = selectedLevel == option.level
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    selectedLevel = option.level
                                    weeklyGoal = option.suggestedWeeklyTarget
                                    selectedDays = option.suggestedDays
                                }
                                .testTag("onboarding_level_${option.level.lowercase()}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) CardSurfaceElevated else CardSurface
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) option.accentColor else Color.White.copy(0.06f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = option.level,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextWhite
                                        )
                                        if (option.badge != null) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(NeonGreen.copy(0.16f), CircleShape)
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = option.badge,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = NeonGreen
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = option.subtitle,
                                        fontSize = 12.sp,
                                        color = TextGray
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .background(
                                            if (isSelected) option.accentColor else DarkSurface,
                                            CircleShape
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) option.accentColor else Color.White.copy(0.15f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color(0xFF003818),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Realistic Weekly Goal
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. WEEKLY GOAL",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = TextGray,
                            letterSpacing = 0.8.sp
                        )
                        val daysCount = selectedDays.size.coerceAtLeast(1)
                        Text(
                            text = "~${(weeklyGoal + daysCount - 1) / daysCount} reps / session",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AquaBlue
                        )
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(0.06f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { weeklyGoal = (weeklyGoal - 25).coerceAtLeast(25) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(CardSurfaceElevated, CircleShape)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease weekly goal", tint = TextWhite)
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$weeklyGoal",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black,
                                        color = NeonGreen
                                    )
                                    Text(
                                        text = "PUSH-UPS / WEEK",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextGray,
                                        letterSpacing = 0.8.sp
                                    )
                                }

                                IconButton(
                                    onClick = { weeklyGoal = (weeklyGoal + 25).coerceAtMost(2000) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(CardSurfaceElevated, CircleShape)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase weekly goal", tint = NeonGreen)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                goalPresets.forEach { preset ->
                                    val active = weeklyGoal == preset
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (active) NeonGreen.copy(0.18f) else DarkSurface)
                                            .border(
                                                1.dp,
                                                if (active) NeonGreen else Color.White.copy(0.08f),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable { weeklyGoal = preset }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$preset",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (active) NeonGreen else TextGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Preferred Workout Days
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "3. PREFERRED TRAINING DAYS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = TextGray,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "${selectedDays.size} days/week",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        dayLabels.forEach { (dayNum, label) ->
                            val isSelected = dayNum in selectedDays
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) NeonGreen else CardSurface)
                                    .border(
                                        1.dp,
                                        if (isSelected) NeonGreen else Color.White.copy(0.08f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        val updated = selectedDays.toMutableSet()
                                        if (dayNum in updated && updated.size > 1) {
                                            updated.remove(dayNum)
                                        } else {
                                            updated.add(dayNum)
                                        }
                                        selectedDays = updated
                                    }
                                    .testTag("onboarding_day_$dayNum"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isSelected) Color(0xFF003818) else TextGray
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Bottom Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        onCompleteSetup(selectedLevel, weeklyGoal, selectedDays, true)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("onboarding_save_and_start"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        contentColor = Color(0xFF003818)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SAVE & START FIRST WORKOUT",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }

                OutlinedButton(
                    onClick = {
                        onCompleteSetup(selectedLevel, weeklyGoal, selectedDays, false)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("onboarding_save_dashboard"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(0.18f))
                ) {
                    Text(
                        text = "Save Plan & View Dashboard",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
