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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrackChanges
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyPlan
import com.example.data.model.UserPreferences
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

@Composable
fun ScheduleScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val plans by viewModel.plans.collectAsState()
    val preferences by viewModel.preferences.collectAsState()

    var weeklyTargetInput by remember { mutableStateOf("") }
    var voiceCoachEnabled by remember { mutableStateOf(true) }
    var notificationsEnabled by remember { mutableStateOf(true) }

    // Synchronize local states when DB loads
    preferences?.let { prefs ->
        if (weeklyTargetInput.isEmpty()) {
            weeklyTargetInput = prefs.weeklyTarget.toString()
            voiceCoachEnabled = prefs.voiceCoachEnabled
            notificationsEnabled = prefs.notificationsEnabled
        }
    }

    var activeDayEditId by remember { mutableStateOf<Int?>(null) }

    val plannedTotalReps = plans.filter { it.isEnabled }.sumOf { it.dailyTarget }
    val activeDaysCount = plans.count { it.isEnabled }

    Box(modifier = modifier.fillMaxSize().background(PitchBlack)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Screen Heading Title
            Column {
                Text(
                    text = "WEEKLY SCHEDULE",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = TextWhite,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "CONFIGURE TARGET REPETITIONS & TIMERS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextGray,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Weekly Plan Summary banner
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
                            Column {
                                Text(
                                    text = "PLANNED VOLUME",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextGray,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "$plannedTotalReps",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = NeonGreen
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "/ ${weeklyTargetInput.ifEmpty { "300" }} target reps",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextGray
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .background(DarkSurface, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "$activeDaysCount Active Days",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AquaBlue
                                )
                            }
                        }
                    }
                }

                // Overall Settings preferences Panel
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(0.06f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Preferences & Reminders", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Weekly Target Textbox
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Weekly Cumulative Target", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                    Text("Reps goal across all sessions", fontSize = 11.sp, color = TextGray)
                                }

                                OutlinedTextField(
                                    value = weeklyTargetInput,
                                    onValueChange = {
                                        weeklyTargetInput = it
                                        val valInt = it.toIntOrNull()
                                        if (valInt != null && valInt > 0) {
                                            viewModel.updatePreferences(valInt, voiceCoachEnabled, notificationsEnabled)
                                        }
                                    },
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = NeonGreen, fontWeight = FontWeight.Black),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .width(96.dp)
                                        .testTag("weekly_target_edit"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonGreen,
                                        unfocusedBorderColor = Color.White.copy(0.15f)
                                    )
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(0.06f))

                            // Audio Switch Toggler
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Voice Coach Engine", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                    Text("Announces reps, rest counts, and motivational cues", fontSize = 11.sp, color = TextGray)
                                }

                                Switch(
                                    checked = voiceCoachEnabled,
                                    onCheckedChange = {
                                        voiceCoachEnabled = it
                                        val wt = weeklyTargetInput.toIntOrNull() ?: 300
                                        viewModel.updatePreferences(wt, it, notificationsEnabled)
                                        if (it) {
                                            viewModel.speak("Voice coach engine online!")
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF003818),
                                        checkedTrackColor = NeonGreen
                                    ),
                                    modifier = Modifier.testTag("voice_coach_toggle")
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(0.06f))

                            // Notification Switch Toggler
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Daily Training Reminders", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                    Text("Alerts on scheduled workout days", fontSize = 11.sp, color = TextGray)
                                }

                                Switch(
                                    checked = notificationsEnabled,
                                    onCheckedChange = {
                                        notificationsEnabled = it
                                        val wt = weeklyTargetInput.toIntOrNull() ?: 300
                                        viewModel.updatePreferences(wt, voiceCoachEnabled, it)
                                        if (it) {
                                            viewModel.speak("Reminders active!")
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF003818),
                                        checkedTrackColor = NeonGreen
                                    ),
                                    modifier = Modifier.testTag("notifications_toggle")
                                )
                            }
                        }
                    }
                }

                // Days Tracker Target title
                item {
                    Text(
                        text = "DAY-BY-DAY ROUTINE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = TextWhite,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Loop of daily plans Monday to Sunday
                items(plans) { plan ->
                    val isEditing = activeDayEditId == plan.dayOfWeek
                    DailyPlanPlannerRow(
                        plan = plan,
                        isEditing = isEditing,
                        onEditFinished = { updatedPlan ->
                            viewModel.updatePlan(updatedPlan)
                            activeDayEditId = null
                        },
                        onToggleEdit = {
                            activeDayEditId = if (isEditing) null else plan.dayOfWeek
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun DailyPlanPlannerRow(
    plan: DailyPlan,
    isEditing: Boolean,
    onEditFinished: (DailyPlan) -> Unit,
    onToggleEdit: () -> Unit
) {
    var targetStr by remember(plan) { mutableStateOf(plan.dailyTarget.toString()) }
    var timeStr by remember(plan) { mutableStateOf(plan.workoutTime) }
    var isDayEnabled by remember(plan) { mutableStateOf(plan.isEnabled) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleEdit() },
        colors = CardDefaults.cardColors(
            containerColor = if (plan.isEnabled) CardSurface else DarkSurface
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (plan.isEnabled) NeonGreen.copy(0.25f) else Color.White.copy(0.04f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (plan.isEnabled) NeonGreen.copy(0.15f) else CardSurfaceElevated,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = plan.dayName.substring(0, 3).uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (plan.isEnabled) NeonGreen else TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = plan.dayName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (plan.isEnabled) TextWhite else TextGray
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (plan.isEnabled) {
                                Icon(
                                    imageVector = Icons.Default.TrackChanges,
                                    contentDescription = null,
                                    tint = NeonGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${plan.dailyTarget} reps • Alert at ${plan.workoutTime}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NeonGreen
                                )
                            } else {
                                Text(
                                    text = "Rest & Recovery Day",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }

                // Checkbox status toggle
                Switch(
                    checked = isDayEnabled,
                    onCheckedChange = {
                        isDayEnabled = it
                        onEditFinished(plan.copy(isEnabled = it))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF003818),
                        checkedTrackColor = NeonGreen
                    ),
                    modifier = Modifier.testTag("day_switch_${plan.dayOfWeek}")
                )
            }

            // Expanded Editor panel
            if (isEditing) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(0.06f))
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Quick Target Presets:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextGray
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(25, 50, 75, 100).forEach { preset ->
                        OutlinedButton(
                            onClick = { targetStr = preset.toString() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (targetStr == preset.toString()) NeonGreen else Color.White.copy(0.12f))
                        ) {
                            Text(
                                text = "$preset",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (targetStr == preset.toString()) NeonGreen else TextWhite
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = targetStr,
                        onValueChange = { targetStr = it },
                        label = { Text("Target reps", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_target_day_${plan.dayOfWeek}"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = Color.White.copy(0.12f),
                            focusedLabelColor = NeonGreen
                        )
                    )

                    OutlinedTextField(
                        value = timeStr,
                        onValueChange = { timeStr = it },
                        label = { Text("Time (HH:mm)", fontSize = 10.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_time_day_${plan.dayOfWeek}"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = Color.White.copy(0.12f),
                            focusedLabelColor = NeonGreen
                        )
                    )

                    Button(
                        onClick = {
                            val targetInt = targetStr.toIntOrNull() ?: plan.dailyTarget
                            onEditFinished(
                                plan.copy(
                                    isEnabled = isDayEnabled,
                                    dailyTarget = targetInt,
                                    workoutTime = timeStr
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("save_day_edit_${plan.dayOfWeek}")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save settings", tint = Color(0xFF003818))
                    }
                }
            }
        }
    }
}
