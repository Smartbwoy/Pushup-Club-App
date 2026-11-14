package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.UploadFile
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.backup.BackupManager
import com.example.data.backup.BackupPayload
import com.example.data.model.DailyPlan
import com.example.notifications.ReminderScheduler
import com.example.ui.WorkoutViewModel
import com.example.ui.components.AppInfoPrivacyDialog
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

@Composable
fun ScheduleScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val plans by viewModel.plans.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val milestones by viewModel.milestones.collectAsState()
    val suggestedTrainingTime by viewModel.suggestedTrainingTime.collectAsState()

    var weeklyTargetInput by remember { mutableStateOf("") }
    var voiceCoachEnabled by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember {
        mutableStateOf(ReminderScheduler.hasNotificationPermission(context))
    }
    var permissionDeniedPrompt by remember { mutableStateOf(false) }
    var showInfoPrivacyDialog by remember { mutableStateOf(false) }
    var showReminderHelpDialog by remember { mutableStateOf(false) }
    var testNotificationNote by remember { mutableStateOf("") }
    var backupStatusMessage by remember { mutableStateOf("") }
    var backupStatusError by remember { mutableStateOf(false) }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }
    var pendingRestorePayload by remember { mutableStateOf<BackupPayload?>(null) }

    // Synchronize local states cleanly when DB updates
    LaunchedEffect(preferences) {
        preferences?.let { prefs ->
            weeklyTargetInput = prefs.weeklyTarget.toString()
            voiceCoachEnabled = prefs.voiceCoachEnabled
            notificationsEnabled = prefs.notificationsEnabled
        }
    }

    // Re-check notification permission when returning from system Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission = ReminderScheduler.hasNotificationPermission(context)
                if (hasNotificationPermission) {
                    permissionDeniedPrompt = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
        val wt = weeklyTargetInput.toIntOrNull() ?: preferences?.weeklyTarget ?: 100
        if (granted) {
            permissionDeniedPrompt = false
            notificationsEnabled = true
            viewModel.updatePreferences(wt, voiceCoachEnabled, true)
            viewModel.speak("Reminders active!")
        } else {
            permissionDeniedPrompt = true
            notificationsEnabled = false
            viewModel.updatePreferences(wt, voiceCoachEnabled, false)
        }
    }

    var activeDayEditId by remember { mutableStateOf<Int?>(null) }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val json = viewModel.buildBackupJson()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(json.toByteArray(Charsets.UTF_8))
                        } ?: throw IllegalStateException("Unable to open destination file.")
                    }
                    backupStatusError = false
                    backupStatusMessage = "Backup exported to JSON successfully."
                } catch (e: Exception) {
                    backupStatusError = true
                    backupStatusMessage = "Export failed: ${e.message ?: "Unknown error"}"
                }
            }
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val json = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                            it.readText()
                        } ?: ""
                    }
                    val parsed = BackupManager.parseBackupJson(json)
                    if (parsed == null) {
                        backupStatusError = true
                        backupStatusMessage = "Invalid backup file format."
                    } else {
                        pendingRestoreJson = json
                        pendingRestorePayload = parsed
                    }
                } catch (e: Exception) {
                    backupStatusError = true
                    backupStatusMessage = "Restore failed: ${e.message ?: "Unknown error"}"
                }
            }
        }
    }

    val plannedTotalReps = plans.filter { it.isEnabled }.sumOf { it.dailyTarget }
    val activeDaysCount = plans.count { it.isEnabled }
    val effectiveRemindersActive = notificationsEnabled && hasNotificationPermission
    val nextReminderSummary = remember(plans, effectiveRemindersActive) {
        ReminderScheduler.getNextScheduledReminderSummary(plans, effectiveRemindersActive)
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
                        text = "PLAN & SETTINGS",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = TextWhite,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "CONFIGURE SCHEDULE, REMINDERS & BACKUPS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGray,
                        letterSpacing = 0.5.sp
                    )
                }

                IconButton(
                    onClick = { showInfoPrivacyDialog = true },
                    modifier = Modifier
                        .background(CardSurfaceElevated, CircleShape)
                        .size(48.dp)
                        .testTag("app_info_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "App Info, Milestones, and Privacy",
                        tint = AquaBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
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
                                    fontSize = 11.sp,
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
                                        text = "/ ${weeklyTargetInput.ifEmpty { "100" }} target reps",
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
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
                                    Text("7-day push-ups goal", fontSize = 11.sp, color = TextGray)
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
                                        val wt = weeklyTargetInput.toIntOrNull() ?: 100
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

                            // Notification Switch Toggler with Real Permission + Scheduling
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Daily Training Reminders", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                    val statusText = when {
                                        notificationsEnabled && !hasNotificationPermission -> "Notification permission required to send alerts"
                                        effectiveRemindersActive && nextReminderSummary != null -> "Active • Next reminder: $nextReminderSummary"
                                        effectiveRemindersActive -> "Active • Enable at least one workout day below"
                                        else -> "Off • Toggle on for alerts on training days"
                                    }
                                    Text(
                                        text = statusText,
                                        fontSize = 11.sp,
                                        color = when {
                                            notificationsEnabled && !hasNotificationPermission -> FireOrange
                                            effectiveRemindersActive && nextReminderSummary != null -> NeonGreen
                                            else -> TextGray
                                        }
                                    )
                                }

                                Switch(
                                    checked = effectiveRemindersActive,
                                    onCheckedChange = { wantEnabled ->
                                        val wt = weeklyTargetInput.toIntOrNull() ?: 100
                                        if (wantEnabled) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                                !ReminderScheduler.hasNotificationPermission(context)
                                            ) {
                                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            } else {
                                                permissionDeniedPrompt = false
                                                notificationsEnabled = true
                                                viewModel.updatePreferences(wt, voiceCoachEnabled, true)
                                                viewModel.speak("Reminders active!")
                                            }
                                        } else {
                                            permissionDeniedPrompt = false
                                            notificationsEnabled = false
                                            viewModel.updatePreferences(wt, voiceCoachEnabled, false)
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF003818),
                                        checkedTrackColor = NeonGreen
                                    ),
                                    modifier = Modifier.testTag("notifications_toggle")
                                )
                            }

                            if (permissionDeniedPrompt || (notificationsEnabled && !hasNotificationPermission)) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(FireOrange.copy(0.12f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsOff,
                                            contentDescription = null,
                                            tint = FireOrange,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Allow notifications so reminders can arrive on time.",
                                            fontSize = 11.sp,
                                            color = TextWhite
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            val settingsIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                            }
                                            try {
                                                context.startActivity(settingsIntent)
                                            } catch (_: Exception) {}
                                        }
                                    ) {
                                        Text("Settings", color = FireOrange, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Quick Test Alert & Battery Optimization Help row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { showReminderHelpDialog = true },
                                    modifier = Modifier.testTag("reminder_help_button")
                                ) {
                                    Text(
                                        text = "Reminder didn't fire?",
                                        color = AquaBlue,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            !ReminderScheduler.hasNotificationPermission(context)
                                        ) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            val sent = ReminderScheduler.sendTestNotification(context)
                                            testNotificationNote = if (sent) {
                                                "Test notification sent!"
                                            } else {
                                                "Unable to post notification — check system settings."
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, NeonGreen.copy(0.4f)),
                                    modifier = Modifier.testTag("send_test_notification_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = NeonGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Send Test Alert",
                                        color = NeonGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (testNotificationNote.isNotEmpty()) {
                                Text(
                                    text = testNotificationNote,
                                    color = NeonGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Smart Adaptive Reminder Timing Nudge
                val learnedTime = suggestedTrainingTime
                val needsTimingSync = learnedTime != null && plans.any { it.isEnabled && it.workoutTime != learnedTime }
                if (learnedTime != null && needsTimingSync) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CardSurface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, AquaBlue.copy(0.35f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(AquaBlue.copy(0.14f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = AquaBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "SMART TIMING NUDGE",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = AquaBlue,
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = "You usually train around $learnedTime. Match reminders to $learnedTime?",
                                            fontSize = 12.sp,
                                            color = TextWhite,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Button(
                                    onClick = { viewModel.applySuggestedTrainingTime(learnedTime) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AquaBlue),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("apply_suggested_time_button")
                                ) {
                                    Text(
                                        text = "Apply",
                                        color = Color(0xFF002438),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp
                                    )
                                }
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
                items(plans, key = { it.dayOfWeek }) { plan ->
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

                // Offline JSON Backup & Restore Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(0.08f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "BACKUP & RESTORE",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = TextWhite,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Export your workouts, schedule, 30-day challenge, and settings to a JSON file, or restore on a new phone.",
                                fontSize = 11.sp,
                                color = TextGray,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        exportBackupLauncher.launch("pushup-club-backup.json")
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("export_backup_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeonGreen,
                                        contentColor = Color(0xFF003818)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Export JSON", fontWeight = FontWeight.Black, fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        restoreBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("restore_backup_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, AquaBlue.copy(0.5f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.UploadFile,
                                        contentDescription = null,
                                        tint = AquaBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Restore JSON", color = AquaBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            if (backupStatusMessage.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = backupStatusMessage,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (backupStatusError) FireOrange else NeonGreen
                                )
                            }
                        }
                    }
                }

                // Discreet App Info, Milestones & Privacy Footer Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showInfoPrivacyDialog = true },
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(0.06f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Privacy, Support & Milestones",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                                Text(
                                    text = "Offline storage policy • Contact support • View app milestones",
                                    fontSize = 11.sp,
                                    color = TextGray
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Open Privacy and Support",
                                tint = AquaBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        if (showInfoPrivacyDialog) {
            AppInfoPrivacyDialog(
                milestones = milestones,
                onDismiss = { showInfoPrivacyDialog = false }
            )
        }

        val restorePayload = pendingRestorePayload
        val restoreJson = pendingRestoreJson
        if (restorePayload != null && restoreJson != null) {
            AlertDialog(
                onDismissRequest = {
                    pendingRestorePayload = null
                    pendingRestoreJson = null
                },
                title = {
                    Text(
                        text = "Restore Backup?",
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                },
                text = {
                    Text(
                        text = "This will replace your current workout history with ${restorePayload.sessions.size} backed-up sessions and your saved schedule. A safety snapshot of your current history will be saved automatically on this device before restoring.",
                        color = TextGray,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val jsonToRestore = restoreJson
                            pendingRestorePayload = null
                            pendingRestoreJson = null
                            viewModel.restoreBackupFromJson(jsonToRestore) { success, message ->
                                backupStatusError = !success
                                backupStatusMessage = message
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        modifier = Modifier.testTag("confirm_restore_backup_button")
                    ) {
                        Text(
                            text = "Restore (${restorePayload.sessions.size})",
                            color = Color(0xFF003818),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            pendingRestorePayload = null
                            pendingRestoreJson = null
                        }
                    ) {
                        Text("Cancel", color = TextWhite)
                    }
                },
                containerColor = CardSurfaceElevated,
                shape = RoundedCornerShape(16.dp)
            )
        }

        if (showReminderHelpDialog) {
            AlertDialog(
                onDismissRequest = { showReminderHelpDialog = false },
                title = {
                    Text(
                        text = "Reminders Not Firing?",
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Some phones (especially Samsung, Xiaomi, and Tecno) pause background alarms to save battery.",
                            color = TextGray,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                        Text(
                            text = "1. Ensure Notifications are allowed for Pushup Club.\n2. Set Battery usage to \"Unrestricted\" so scheduled reminders arrive on time.\n3. Confirm at least one workout day is switched on below.",
                            color = TextWhite,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val batteryIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            try {
                                context.startActivity(batteryIntent)
                            } catch (_: Exception) {
                                try {
                                    context.startActivity(fallbackIntent)
                                } catch (_: Exception) {}
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                    ) {
                        Text("Battery Settings", color = Color(0xFF003818), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = {
                                val notifIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                                try {
                                    context.startActivity(notifIntent)
                                } catch (_: Exception) {}
                            }
                        ) {
                            Text("Notification Settings", color = AquaBlue)
                        }
                        TextButton(onClick = { showReminderHelpDialog = false }) {
                            Text("Close", color = TextWhite)
                        }
                    }
                },
                containerColor = CardSurfaceElevated,
                shape = RoundedCornerShape(16.dp)
            )
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
    var timeError by remember(plan) { mutableStateOf("") }

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

                Text(
                    text = "Quick Reminder Times:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextGray
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("07:00", "12:00", "18:00", "20:00").forEach { presetTime ->
                        OutlinedButton(
                            onClick = {
                                timeStr = presetTime
                                timeError = ""
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (timeStr == presetTime) AquaBlue else Color.White.copy(0.12f))
                        ) {
                            Text(
                                text = presetTime,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (timeStr == presetTime) AquaBlue else TextWhite
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
                        onValueChange = {
                            timeStr = it
                            timeError = ""
                        },
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
                            val parsedTime = ReminderScheduler.parseTimeOrNull(timeStr)
                            if (parsedTime == null) {
                                timeError = "Use 24h HH:mm format (e.g. 18:00)"
                            } else {
                                val targetInt = (targetStr.toIntOrNull() ?: plan.dailyTarget).coerceAtLeast(1)
                                val formattedTime = ReminderScheduler.formatTime(parsedTime.first, parsedTime.second)
                                onEditFinished(
                                    plan.copy(
                                        isEnabled = isDayEnabled,
                                        dailyTarget = targetInt,
                                        workoutTime = formattedTime
                                    )
                                )
                            }
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

                if (timeError.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = timeError,
                        color = FireOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
