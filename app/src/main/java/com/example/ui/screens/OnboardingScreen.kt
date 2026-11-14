package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.UserPreferences
import com.example.notifications.ReminderScheduler
import com.example.ui.theme.AquaBlue
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.FireOrange
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.TextGray
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
    hasProximitySensor: Boolean = true,
    onCompleteSetup: (
        level: String,
        weeklyTarget: Int,
        enabledDays: Set<Int>,
        startImmediately: Boolean,
        preferredCounterMode: String,
        voiceCoachEnabled: Boolean,
        notificationsEnabled: Boolean
    ) -> Unit,
    onSkipAndStartWorkout: () -> Unit
) {
    val context = LocalContext.current

    val levelOptions = remember {
        listOf(
            StarterLevelOption(
                level = "Beginner",
                badge = "RECOMMENDED FIRST",
                subtitle = "Start steady with 5-rep sets & plenty of rest",
                suggestedWeeklyTarget = 100,
                suggestedDays = setOf(1, 3, 5),
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
    var selectedCounterMode by remember { mutableStateOf(UserPreferences.COUNTER_MODE_TAP) }
    var voiceCoachEnabled by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(false) }
    var modeHint by remember { mutableStateOf<String?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            selectedCounterMode = UserPreferences.COUNTER_MODE_CAMERA
            modeHint = null
        } else {
            selectedCounterMode = UserPreferences.COUNTER_MODE_TAP
            modeHint = "Camera permission declined — Tap Mode selected."
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsEnabled = granted
    }

    val dayLabels = remember {
        listOf(1 to "M", 2 to "T", 3 to "W", 4 to "T", 5 to "F", 6 to "S", 7 to "S")
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
                        text = "PUSHUP CLUB",
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
                        text = stringResource(R.string.onboarding_skip_start),
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
                        text = stringResource(R.string.onboarding_title),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = TextWhite,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Track reps, follow a progressive plan, and watch your weekly total climb.",
                        fontSize = 13.sp,
                        color = TextGray,
                        lineHeight = 18.sp
                    )
                }

                // 1. Starting Level
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
                                                    fontSize = 10.sp,
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

                // 2. Weekly Goal
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
                            fontSize = 12.sp,
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
                                        fontSize = 11.sp,
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
                                            fontSize = 12.sp,
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
                            text = "3. TRAINING DAYS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = TextGray,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "${selectedDays.size} days/week",
                            fontSize = 12.sp,
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

                // 4. Preferred Rep Counting Mode
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "4. REP COUNTING MODE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = TextGray,
                        letterSpacing = 0.8.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OnboardingModeChip(
                            title = "Tap",
                            icon = Icons.Default.TouchApp,
                            selected = selectedCounterMode == UserPreferences.COUNTER_MODE_TAP,
                            enabled = true,
                            accent = NeonGreen,
                            modifier = Modifier.weight(1f).testTag("onboarding_mode_tap"),
                            onClick = {
                                modeHint = null
                                selectedCounterMode = UserPreferences.COUNTER_MODE_TAP
                            }
                        )
                        OnboardingModeChip(
                            title = if (hasProximitySensor) "Sensor" else "No Sensor",
                            icon = Icons.Default.Sensors,
                            selected = selectedCounterMode == UserPreferences.COUNTER_MODE_PROXIMITY,
                            enabled = hasProximitySensor,
                            accent = AquaBlue,
                            modifier = Modifier.weight(1f).testTag("onboarding_mode_proximity"),
                            onClick = {
                                modeHint = null
                                selectedCounterMode = UserPreferences.COUNTER_MODE_PROXIMITY
                            }
                        )
                        OnboardingModeChip(
                            title = "Camera",
                            icon = Icons.Default.CameraAlt,
                            selected = selectedCounterMode == UserPreferences.COUNTER_MODE_CAMERA,
                            enabled = true,
                            accent = AquaBlue,
                            modifier = Modifier.weight(1f).testTag("onboarding_mode_camera"),
                            onClick = {
                                val hasCam = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasCam) {
                                    modeHint = null
                                    selectedCounterMode = UserPreferences.COUNTER_MODE_CAMERA
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                        )
                    }

                    Text(
                        text = when (selectedCounterMode) {
                            UserPreferences.COUNTER_MODE_PROXIMITY -> stringResource(R.string.mode_proximity_desc)
                            UserPreferences.COUNTER_MODE_CAMERA -> stringResource(R.string.mode_camera_desc)
                            else -> stringResource(R.string.mode_tap_desc)
                        },
                        fontSize = 12.sp,
                        color = TextGray,
                        lineHeight = 16.sp
                    )

                    if (modeHint != null) {
                        Text(
                            text = modeHint.orEmpty(),
                            fontSize = 12.sp,
                            color = FireOrange,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // 5. Audio & Optional Reminders
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(0.06f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.onboarding_voice_label),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                                Text(
                                    text = stringResource(R.string.onboarding_voice_desc),
                                    fontSize = 12.sp,
                                    color = TextGray
                                )
                            }
                            Switch(
                                checked = voiceCoachEnabled,
                                onCheckedChange = { voiceCoachEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF003818),
                                    checkedTrackColor = NeonGreen
                                ),
                                modifier = Modifier.testTag("onboarding_voice_toggle")
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color.White.copy(0.08f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.onboarding_notif_label),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                                Text(
                                    text = stringResource(R.string.onboarding_notif_reason),
                                    fontSize = 12.sp,
                                    color = TextGray,
                                    lineHeight = 16.sp
                                )
                            }
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { want ->
                                    if (!want) {
                                        notificationsEnabled = false
                                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        !ReminderScheduler.hasNotificationPermission(context)
                                    ) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        notificationsEnabled = true
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF003818),
                                    checkedTrackColor = NeonGreen
                                ),
                                modifier = Modifier.testTag("onboarding_notif_toggle")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        onCompleteSetup(
                            selectedLevel,
                            weeklyGoal,
                            selectedDays,
                            true,
                            selectedCounterMode,
                            voiceCoachEnabled,
                            notificationsEnabled
                        )
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
                        onCompleteSetup(
                            selectedLevel,
                            weeklyGoal,
                            selectedDays,
                            false,
                            selectedCounterMode,
                            voiceCoachEnabled,
                            notificationsEnabled
                        )
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

@Composable
private fun OnboardingModeChip(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    enabled: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = when {
            !enabled -> DarkSurface.copy(alpha = 0.45f)
            selected -> accent.copy(0.18f)
            else -> CardSurface
        },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = when {
                !enabled -> Color.White.copy(0.04f)
                selected -> accent
                else -> Color.White.copy(0.08f)
            }
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = when {
                    !enabled -> TextGray.copy(0.4f)
                    selected -> accent
                    else -> TextGray
                },
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
                color = when {
                    !enabled -> TextGray.copy(0.45f)
                    selected -> TextWhite
                    else -> TextGray
                },
                maxLines = 1
            )
        }
    }
}
