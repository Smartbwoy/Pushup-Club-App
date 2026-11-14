package com.example.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.speech.tts.TextToSpeech
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.adaptive.AdaptiveProgramManager
import com.example.data.backup.BackupManager
import com.example.data.challenge.ThirtyDayChallengeManager
import com.example.data.challenge.ThirtyDayChallengeState
import com.example.data.cloud.ClubMemberStats
import com.example.data.cloud.ClubNicknameStore
import com.example.data.cloud.ClubRepository
import com.example.data.cloud.ClubSyncWorker
import com.example.data.db.AppDatabase
import com.example.data.model.ChallengeGoal
import com.example.data.model.DailyPlan
import com.example.data.model.TrainingProgram
import com.example.data.model.UserPreferences
import com.example.data.model.WorkoutSession
import com.example.data.repository.WorkoutRepository
import com.example.data.tracking.ActiveWorkoutDraft
import com.example.data.tracking.ActiveWorkoutDraftStore
import com.example.data.tracking.MilestoneSnapshot
import com.example.data.tracking.MilestoneTracker
import com.example.notifications.ReminderScheduler
import com.example.widget.WorkoutWidgetProvider
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class WorkoutViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {
    private val db = AppDatabase.getInstance(application)
    private val repository = WorkoutRepository(db.workoutDao)
    private val milestoneTracker = MilestoneTracker(application)
    private val challengeManager = ThirtyDayChallengeManager(application)
    private val adaptiveProgramManager = AdaptiveProgramManager(application)
    private val draftStore = ActiveWorkoutDraftStore(application)
    private val nicknameStore = ClubNicknameStore(application)

    // TTS Voice Coach
    private var tts: TextToSpeech? = null
    private val _ttsReady = MutableStateFlow(false)

    // Sensor hardware
    private val sensorManager = application.getSystemService(Application.SENSOR_SERVICE) as SensorManager
    private val proximitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    val hasProximitySensor: Boolean = proximitySensor != null

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
            repository.recalculateProgress()
            val prefs = repository.getUserPreferencesSync()
            val plans = repository.getAllDailyPlansSync()
            ReminderScheduler.syncReminders(
                context = getApplication(),
                plans = plans,
                notificationsEnabled = prefs?.notificationsEnabled == true
            )
            try {
                ClubSyncWorker.scheduleDailySync(getApplication())
            } catch (_: Exception) {
                // WorkManager may not be initialized under Robolectric unit tests
            }
            restoreActiveDraftIfPresent()
        }
        initializeTts()
    }

    private fun initializeTts() {
        tts = TextToSpeech(getApplication()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    _ttsReady.value = true
                }
            }
        }
    }

    fun speak(text: String) {
        val prefs = preferences.value ?: UserPreferences()
        if (prefs.voiceCoachEnabled && _ttsReady.value) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    // Expose Data Flow
    val sessions: StateFlow<List<WorkoutSession>> = repository.allSessions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val plans: StateFlow<List<DailyPlan>> = repository.allDailyPlans.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val goals: StateFlow<List<ChallengeGoal>> = repository.allChallengeGoals.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val preferences: StateFlow<UserPreferences?> = repository.userPreferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val milestones: StateFlow<MilestoneSnapshot> = milestoneTracker.snapshot

    // Metrics
    val currentStreak: StateFlow<Int> = combine(sessions, plans) { sessionList, planList ->
        repository.calculateStreak(sessionList, planList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val personalBest: StateFlow<Int> = sessions.map { list ->
        list.maxOfOrNull { it.totalPushUps } ?: 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalPushUps: StateFlow<Int> = sessions.map { list ->
        list.sumOf { it.totalPushUps }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val weeklyPushUps: StateFlow<Int> = sessions.map { list ->
        WorkoutRepository.calculateWeeklyPushUps(list)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val thirtyDayChallengeState: StateFlow<ThirtyDayChallengeState> = challengeManager.state
    val adaptiveMultipliers: StateFlow<Map<String, Float>> = adaptiveProgramManager.multipliers

    val suggestedTrainingTime: StateFlow<String?> = sessions.map { list ->
        WorkoutRepository.detectPreferredTrainingTime(list)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Workout Progress state
    private val _activeProgram = MutableStateFlow<TrainingProgram?>(null)
    val activeProgram = _activeProgram.asStateFlow()

    private val _workoutState = MutableStateFlow("IDLE") // IDLE, RUNNING, REST, FINISHED
    val workoutState = _workoutState.asStateFlow()

    private val _activeSets = MutableStateFlow<List<Int>>(emptyList())
    val activeSets = _activeSets.asStateFlow()

    private val _completedReps = MutableStateFlow<List<Int>>(emptyList())
    val completedReps = _completedReps.asStateFlow()

    private val _currentSetIndex = MutableStateFlow(0)
    val currentSetIndex = _currentSetIndex.asStateFlow()

    private val _currentRepCount = MutableStateFlow(0)
    val currentRepCount = _currentRepCount.asStateFlow()

    private val _restTimeLeft = MutableStateFlow(0)
    val restTimeLeft = _restTimeLeft.asStateFlow()

    private var restEndTimeMillis: Long = 0L

    // Proximity Counting & Calibration State
    private val _isProximityCountingEnabled = MutableStateFlow(false)
    val isProximityCountingEnabled = _isProximityCountingEnabled.asStateFlow()

    private val _isCameraCountingEnabled = MutableStateFlow(false)
    val isCameraCountingEnabled = _isCameraCountingEnabled.asStateFlow()

    private val _showSensorCalibrationDialog = MutableStateFlow(false)
    val showSensorCalibrationDialog = _showSensorCalibrationDialog.asStateFlow()

    private val _isSensorNear = MutableStateFlow(false)
    val isSensorNear = _isSensorNear.asStateFlow()

    // 0 = Waiting for wave/dip close, 1 = Close detected (pull back), 2 = Verified & ready
    private val _calibrationStep = MutableStateFlow(0)
    val calibrationStep = _calibrationStep.asStateFlow()

    private var lastProximityTime = 0L
    private var wasNear = false
    private var hasSeenFarSinceEnable = false

    // Haptic feedback engine
    private val vibrator: android.os.Vibrator? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        val vibratorManager = application.getSystemService(Application.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(Application.VIBRATOR_SERVICE) as? android.os.Vibrator
    }

    fun triggerHaptic(durationMs: Long = 40L) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator?.vibrate(android.os.VibrationEffect.createOneShot(durationMs, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    private fun persistActiveDraft(restEndMillis: Long = restEndTimeMillis) {
        val state = _workoutState.value
        if (state != "RUNNING" && state != "REST") {
            draftStore.clear()
            return
        }
        draftStore.saveDraft(
            ActiveWorkoutDraft(
                workoutState = state,
                activeProgramId = _activeProgram.value?.id,
                activeSets = _activeSets.value,
                completedReps = _completedReps.value,
                currentSetIndex = _currentSetIndex.value,
                currentRepCount = _currentRepCount.value,
                restEndTimeMillis = if (state == "REST") restEndMillis else 0L,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    private fun resolveProgramForId(programId: String?): TrainingProgram? {
        if (programId.isNullOrBlank()) return null
        val preset = TrainingProgram.programs.firstOrNull { it.id == programId }
        if (preset != null) return preset
        val challengeDay = ThirtyDayChallengeManager.parseChallengeDay(programId) ?: return null
        val state = challengeManager.state.value
        val sets = ThirtyDayChallengeManager.buildSetsForDay(state.initialMaxReps.coerceAtLeast(5), challengeDay)
        val isRecovery = challengeDay % 5 == 0 && challengeDay < 30
        return TrainingProgram(
            id = programId,
            name = "30-Day Challenge: Day $challengeDay",
            description = if (isRecovery) "Active recovery day at 65% volume" else "Day $challengeDay of 30 progressive push-up challenge",
            difficulty = "Challenge",
            sets = sets,
            restTimeSeconds = if (isRecovery) 45 else 35
        )
    }

    private fun restoreActiveDraftIfPresent() {
        if (_workoutState.value != "IDLE") return
        val draft = draftStore.loadValidDraft() ?: return
        _activeProgram.value = resolveProgramForId(draft.activeProgramId)
        _activeSets.value = draft.activeSets
        _completedReps.value = draft.completedReps
        _currentSetIndex.value = draft.currentSetIndex
        _currentRepCount.value = draft.currentRepCount

        if (draft.workoutState == "REST") {
            val remainingSec = ((draft.restEndTimeMillis - System.currentTimeMillis()) / 1000L).toInt()
            if (remainingSec > 0) {
                startRestTimerLoop(remainingSec, draft.restEndTimeMillis)
            } else {
                proceedToNextSet()
            }
        } else {
            _restTimeLeft.value = 0
            _workoutState.value = "RUNNING"
        }
    }

    private fun applyPreferredCounterModeIfPermitted(mode: String?) {
        when (mode) {
            UserPreferences.COUNTER_MODE_PROXIMITY -> {
                if (hasProximitySensor) {
                    toggleProximityCounting(true)
                }
            }
            UserPreferences.COUNTER_MODE_CAMERA -> {
                val hasCam = ContextCompat.checkSelfPermission(
                    getApplication(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                if (hasCam) {
                    toggleCameraCounting(true)
                }
            }
            else -> switchToManualTapCounting()
        }
    }

    fun toggleProximityCounting(enabled: Boolean) {
        if (!hasProximitySensor) {
            _isProximityCountingEnabled.value = false
            _showSensorCalibrationDialog.value = false
            return
        }
        if (enabled) {
            _isCameraCountingEnabled.value = false
            triggerHaptic(60L)
            wasNear = false
            hasSeenFarSinceEnable = false
            _isSensorNear.value = false
            _calibrationStep.value = 0
            _isProximityCountingEnabled.value = true
            _showSensorCalibrationDialog.value = true
            proximitySensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        } else {
            disableProximityCounting()
        }
    }

    fun toggleCameraCounting(enabled: Boolean) {
        if (enabled) {
            disableProximityCounting()
            triggerHaptic(60L)
            _isCameraCountingEnabled.value = true
        } else {
            _isCameraCountingEnabled.value = false
        }
    }

    private fun disableProximityCounting() {
        _isProximityCountingEnabled.value = false
        _showSensorCalibrationDialog.value = false
        _isSensorNear.value = false
        wasNear = false
        hasSeenFarSinceEnable = false
        sensorManager.unregisterListener(this)
    }

    fun openSensorCalibration() {
        if (!hasProximitySensor) return
        _calibrationStep.value = 0
        _showSensorCalibrationDialog.value = true
    }

    fun dismissSensorCalibration() {
        _showSensorCalibrationDialog.value = false
        // Reset state so user must move from FAR -> NEAR on next rep
        hasSeenFarSinceEnable = !wasNear
    }

    fun switchToManualTapCounting() {
        disableProximityCounting()
        _isCameraCountingEnabled.value = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_PROXIMITY) return
        if (!_isProximityCountingEnabled.value) return

        val maxRange = event.sensor.maximumRange
        if (maxRange <= 0f) return
        val distance = event.values[0]
        val nearThreshold = if (maxRange <= 2.0f) maxRange * 0.5f else minOf(maxRange * 0.6f, 5.0f)
        val isNear = distance < nearThreshold
        val now = System.currentTimeMillis()

        _isSensorNear.value = isNear

        if (_showSensorCalibrationDialog.value) {
            if (_calibrationStep.value == 0 && isNear) {
                _calibrationStep.value = 1
                triggerHaptic(40L)
            } else if (_calibrationStep.value == 1 && !isNear) {
                _calibrationStep.value = 2
                hasSeenFarSinceEnable = true
                triggerHaptic(80L)
            }
            wasNear = isNear
            return
        }

        if (_workoutState.value == "RUNNING") {
            if (!isNear) {
                hasSeenFarSinceEnable = true
            } else if (!wasNear && hasSeenFarSinceEnable) {
                // Only count once per FAR -> NEAR transition with cooldown
                if (now - lastProximityTime >= 550L) {
                    incrementRep()
                    lastProximityTime = now
                }
            }
        }

        wasNear = isNear
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun startManualWorkout() {
        _activeProgram.value = null
        _activeSets.value = listOf(15)
        _completedReps.value = listOf(0)
        _currentSetIndex.value = 0
        _currentRepCount.value = 0
        _workoutState.value = "RUNNING"
        _restTimeLeft.value = 0
        restEndTimeMillis = 0L
        applyPreferredCounterModeIfPermitted(preferences.value?.preferredCounterMode)
        persistActiveDraft()
        milestoneTracker.recordWorkoutStarted()
        triggerHaptic(80L)
        speak("Session started. Begin when ready.")
    }

    fun startProgramWorkout(program: TrainingProgram) {
        _activeProgram.value = program
        _activeSets.value = program.sets
        _completedReps.value = List(program.sets.size) { 0 }
        _currentSetIndex.value = 0
        _currentRepCount.value = 0
        _workoutState.value = "RUNNING"
        _restTimeLeft.value = 0
        restEndTimeMillis = 0L
        applyPreferredCounterModeIfPermitted(preferences.value?.preferredCounterMode)
        persistActiveDraft()
        milestoneTracker.recordWorkoutStarted()
        triggerHaptic(80L)
        speak("Starting ${program.name}. Set one target is ${program.sets[0]} reps.")
    }

    fun startThirtyDayChallenge(initialMaxReps: Int) {
        challengeManager.startChallenge(initialMaxReps)
    }

    fun startThirtyDayChallengeWorkout() {
        val state = challengeManager.state.value
        if (!state.isStarted || state.isCompleted) return
        val day = state.currentDay
        val sets = ThirtyDayChallengeManager.buildSetsForDay(state.initialMaxReps, day)
        val isRecovery = day % 5 == 0 && day < 30
        val program = TrainingProgram(
            id = "${ThirtyDayChallengeManager.PROGRAM_ID_PREFIX}$day",
            name = "30-Day Challenge: Day $day",
            description = if (isRecovery) "Active recovery day at 65% volume" else "Day $day of 30 progressive push-up challenge",
            difficulty = "Challenge",
            sets = sets,
            restTimeSeconds = if (isRecovery) 45 else 35
        )
        startProgramWorkout(program)
    }

    fun recordThirtyDayFinalRetest(finalMaxReps: Int) {
        challengeManager.recordFinalRetest(finalMaxReps)
    }

    fun resetThirtyDayChallenge() {
        challengeManager.resetChallenge()
    }

    fun appendManualSet(targetReps: Int) {
        if (_workoutState.value == "FINISHED" || _workoutState.value == "IDLE") return
        val currentSets = _activeSets.value.toMutableList()
        val currentCompleted = _completedReps.value.toMutableList()
        currentSets.add(targetReps)
        currentCompleted.add(0)
        _activeSets.value = currentSets
        _completedReps.value = currentCompleted
        persistActiveDraft()
        triggerHaptic(60L)
        speak("Added set of $targetReps reps.")
    }

    fun updateCurrentSetTarget(delta: Int) {
        val currentSets = _activeSets.value.toMutableList()
        val index = _currentSetIndex.value
        if (index in currentSets.indices) {
            val updated = (currentSets[index] + delta).coerceAtLeast(1)
            currentSets[index] = updated
            _activeSets.value = currentSets
            persistActiveDraft()
            triggerHaptic(30L)
        }
    }

    fun incrementRep() {
        if (_workoutState.value != "RUNNING") return

        val target = if (_activeSets.value.isNotEmpty() && _currentSetIndex.value < _activeSets.value.size) {
            _activeSets.value[_currentSetIndex.value]
        } else Int.MAX_VALUE

        _currentRepCount.value += 1
        persistActiveDraft()
        triggerHaptic(35L)

        val count = _currentRepCount.value

        if (count == target) {
            triggerHaptic(120L)
            speak("$count reps. Set target reached.")
        } else if (count > 0 && count % 5 == 0) {
            speak("$count push-ups")
        } else if (target - count == 3) {
            speak("Three reps left.")
        }
    }

    fun decrementRep() {
        if (_workoutState.value != "RUNNING") return
        if (_currentRepCount.value > 0) {
            _currentRepCount.value -= 1
            persistActiveDraft()
            triggerHaptic(25L)
        }
    }

    fun completeCurrentSet() {
        if (_workoutState.value != "RUNNING") return
        val index = _currentSetIndex.value
        val currentCompleted = _completedReps.value.toMutableList()

        if (index < currentCompleted.size) {
            currentCompleted[index] = _currentRepCount.value
            _completedReps.value = currentCompleted
        }

        triggerHaptic(100L)
        val hasNextSet = index + 1 < _activeSets.value.size
        if (hasNextSet) {
            startRestInterval()
        } else {
            finishWorkout()
        }
    }

    private var restJob: Job? = null
    fun startRestInterval() {
        val restTime = _activeProgram.value?.restTimeSeconds ?: 30
        val endMillis = System.currentTimeMillis() + restTime * 1000L
        speak("Rest for $restTime seconds.")
        startRestTimerLoop(restTime, endMillis)
    }

    private fun startRestTimerLoop(initialSeconds: Int, endMillis: Long) {
        restJob?.cancel()
        restEndTimeMillis = endMillis
        _workoutState.value = "REST"
        _restTimeLeft.value = initialSeconds
        persistActiveDraft(endMillis)

        restJob = viewModelScope.launch {
            while (_restTimeLeft.value > 0) {
                delay(1000)
                _restTimeLeft.value -= 1
                if (_restTimeLeft.value in 1..3) {
                    triggerHaptic(35L)
                }
                if (_restTimeLeft.value == 5) {
                    speak("Five seconds left.")
                }
            }
            proceedToNextSet()
        }
    }

    fun addRestTime(seconds: Int = 15) {
        if (_workoutState.value == "REST") {
            _restTimeLeft.value += seconds
            restEndTimeMillis += seconds * 1000L
            persistActiveDraft(restEndTimeMillis)
            triggerHaptic(50L)
            speak("Added $seconds seconds.")
        }
    }

    fun skipRest() {
        restJob?.cancel()
        triggerHaptic(60L)
        proceedToNextSet()
    }

    fun undoCompleteSet() {
        if (_workoutState.value != "REST") return
        restJob?.cancel()
        val index = _currentSetIndex.value
        val previousReps = _completedReps.value.getOrElse(index) { 0 }
        _currentRepCount.value = previousReps
        _restTimeLeft.value = 0
        restEndTimeMillis = 0L
        _workoutState.value = "RUNNING"
        hasSeenFarSinceEnable = !wasNear
        persistActiveDraft()
        triggerHaptic(40L)
    }

    private fun proceedToNextSet() {
        _currentSetIndex.value += 1
        _currentRepCount.value = 0
        _restTimeLeft.value = 0
        restEndTimeMillis = 0L
        _workoutState.value = "RUNNING"
        hasSeenFarSinceEnable = !wasNear
        persistActiveDraft()
        val nextSetIndex = _currentSetIndex.value
        val nextTarget = if (nextSetIndex < _activeSets.value.size) _activeSets.value[nextSetIndex] else 10
        triggerHaptic(80L)
        speak("Set ${nextSetIndex + 1}, target $nextTarget reps.")
    }

    fun finishWorkout() {
        if (_workoutState.value == "FINISHED" || _workoutState.value == "IDLE") return

        restJob?.cancel()
        disableProximityCounting()
        _isCameraCountingEnabled.value = false
        _workoutState.value = "FINISHED"
        restEndTimeMillis = 0L
        draftStore.clear()

        val finalCompleted = _completedReps.value.toMutableList()
        val index = _currentSetIndex.value
        if (index < finalCompleted.size) {
            finalCompleted[index] = _currentRepCount.value
            _completedReps.value = finalCompleted
        }

        // Take sets up to the active set index
        val activeCompleted = finalCompleted.take(index + 1)
        val totalPushups = activeCompleted.sum()
        val setsRecorded = activeCompleted.size.coerceAtLeast(1)
        val repsString = activeCompleted.joinToString(",")
        val activeProg = _activeProgram.value
        val restTime = activeProg?.restTimeSeconds ?: 30
        val prescribedTotal = _activeSets.value.sum()

        viewModelScope.launch {
            if (totalPushups > 0) {
                val session = WorkoutSession(
                    totalPushUps = totalPushups,
                    setsCount = setsRecorded,
                    repsListString = repsString,
                    restTimeSeconds = restTime,
                    programId = activeProg?.id
                )
                repository.logSession(session)
                val challengeDay = ThirtyDayChallengeManager.parseChallengeDay(activeProg?.id)
                if (challengeDay != null) {
                    challengeManager.recordDayCompleted(challengeDay, totalPushups)
                } else if (activeProg != null) {
                    adaptiveProgramManager.recordProgramOutcome(
                        programId = activeProg.id,
                        targetTotalReps = prescribedTotal,
                        completedTotalReps = totalPushups
                    )
                }
                milestoneTracker.recordWorkoutSaved()
                WorkoutWidgetProvider.updateAllWidgets(getApplication())
                syncClubsInBackground()
                triggerHaptic(150L)
                speak("Session saved. $totalPushups push-ups.")
            } else {
                speak("Workout finished.")
            }
        }
    }

    fun cancelWorkout() {
        restJob?.cancel()
        disableProximityCounting()
        _isCameraCountingEnabled.value = false
        _workoutState.value = "IDLE"
        _activeProgram.value = null
        _activeSets.value = emptyList()
        _completedReps.value = emptyList()
        _currentSetIndex.value = 0
        _currentRepCount.value = 0
        _restTimeLeft.value = 0
        restEndTimeMillis = 0L
        draftStore.clear()
        speak("Workout closed.")
    }

    fun logManualCount(count: Int, sets: Int) {
        if (count <= 0) return
        viewModelScope.launch {
            val splitSets = WorkoutRepository.splitRepsAcrossSets(count, sets)
            val session = WorkoutSession(
                totalPushUps = count,
                setsCount = splitSets.size,
                repsListString = splitSets.joinToString(","),
                restTimeSeconds = 30
            )
            repository.logSession(session)
            milestoneTracker.recordWorkoutStarted()
            milestoneTracker.recordWorkoutSaved()
            WorkoutWidgetProvider.updateAllWidgets(getApplication())
            syncClubsInBackground()
            speak("Logged $count push-ups.")
        }
    }

    fun deleteLoggedSession(session: WorkoutSession) {
        viewModelScope.launch {
            repository.deleteSession(session)
            WorkoutWidgetProvider.updateAllWidgets(getApplication())
            syncClubsInBackground()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAllData()
            WorkoutWidgetProvider.updateAllWidgets(getApplication())
            syncClubsInBackground()
            speak("Workout logs cleared.")
        }
    }

    fun updatePlan(plan: DailyPlan) {
        viewModelScope.launch {
            repository.updateDailyPlan(plan)
            val allPlans = repository.getAllDailyPlansSync()
            val prefs = repository.getUserPreferencesSync()
            ReminderScheduler.syncReminders(
                context = getApplication(),
                plans = allPlans,
                notificationsEnabled = prefs?.notificationsEnabled == true
            )
            WorkoutWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun updatePreferredCounterMode(mode: String) {
        viewModelScope.launch {
            val current = preferences.value ?: repository.getUserPreferencesSync() ?: UserPreferences()
            repository.updateUserPreferences(current.copy(preferredCounterMode = mode))
        }
    }

    fun updatePreferences(weeklyTarget: Int, voiceEnabled: Boolean, notificationsEnabled: Boolean) {
        viewModelScope.launch {
            val current = preferences.value ?: repository.getUserPreferencesSync() ?: UserPreferences()
            val updated = current.copy(
                weeklyTarget = weeklyTarget,
                voiceCoachEnabled = voiceEnabled,
                notificationsEnabled = notificationsEnabled
            )
            repository.updateUserPreferences(updated)
            val allPlans = repository.getAllDailyPlansSync()
            ReminderScheduler.syncReminders(
                context = getApplication(),
                plans = allPlans,
                notificationsEnabled = notificationsEnabled
            )
        }
    }

    fun completeOnboarding(
        experienceLevel: String,
        weeklyTarget: Int,
        enabledDays: Set<Int>,
        startWorkoutImmediately: Boolean,
        preferredCounterMode: String = UserPreferences.COUNTER_MODE_TAP,
        voiceCoachEnabled: Boolean = false,
        notificationsEnabled: Boolean = false
    ) {
        viewModelScope.launch {
            repository.applyOnboardingSetup(
                experienceLevel = experienceLevel,
                weeklyTarget = weeklyTarget,
                enabledDays = enabledDays,
                voiceCoachEnabled = voiceCoachEnabled,
                notificationsEnabled = notificationsEnabled,
                preferredCounterMode = preferredCounterMode
            )
            milestoneTracker.recordSetupCompleted()
            val allPlans = repository.getAllDailyPlansSync()
            ReminderScheduler.syncReminders(
                context = getApplication(),
                plans = allPlans,
                notificationsEnabled = notificationsEnabled
            )
            if (startWorkoutImmediately) {
                val starter = when (experienceLevel) {
                    "Intermediate" -> TrainingProgram.programs.getOrNull(1)
                    "Advanced" -> TrainingProgram.programs.getOrNull(2)
                    else -> TrainingProgram.programs.firstOrNull()
                }
                if (starter != null) {
                    startProgramWorkout(starter)
                } else {
                    startManualWorkout()
                }
            }
        }
    }

    fun skipOnboardingAndStartWorkout() {
        viewModelScope.launch {
            repository.markOnboardingCompleted()
            milestoneTracker.recordSetupSkipped()
            val starter = TrainingProgram.programs.firstOrNull()
            if (starter != null) {
                startProgramWorkout(starter)
            } else {
                startManualWorkout()
            }
        }
    }

    fun applySuggestedTrainingTime(timeStr: String) {
        viewModelScope.launch {
            repository.applyTimeToEnabledDays(timeStr)
            val allPlans = repository.getAllDailyPlansSync()
            val prefs = repository.getUserPreferencesSync()
            ReminderScheduler.syncReminders(
                context = getApplication(),
                plans = allPlans,
                notificationsEnabled = prefs?.notificationsEnabled == true
            )
        }
    }

    suspend fun buildBackupJson(): String {
        val prefs = repository.getUserPreferencesSync() ?: UserPreferences()
        val allPlans = repository.getAllDailyPlansSync()
        val allSessions = repository.getAllSessionsSync()
        return BackupManager.exportToJson(
            preferences = prefs,
            dailyPlans = allPlans,
            sessions = allSessions,
            challengeState = challengeManager.state.value,
            adaptiveMultipliers = adaptiveProgramManager.getAllMultiplierMap()
        )
    }

    private suspend fun saveSafetyBackupSnapshot(): File? {
        return try {
            val json = buildBackupJson()
            withContext(Dispatchers.IO) {
                val file = File(getApplication<Application>().filesDir, SAFETY_BACKUP_FILENAME)
                file.writeText(json, Charsets.UTF_8)
                file
            }
        } catch (_: Exception) {
            null
        }
    }

    fun restoreBackupFromJson(json: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val payload = BackupManager.parseBackupJson(json)
            if (payload == null) {
                onResult(false, "Invalid backup file format.")
                return@launch
            }
            val safetyFile = saveSafetyBackupSnapshot()
            repository.restoreFromBackup(payload)
            challengeManager.restoreState(payload.challengeState)
            adaptiveProgramManager.restoreMultipliers(payload.adaptiveMultipliers)
            val allPlans = repository.getAllDailyPlansSync()
            val prefs = repository.getUserPreferencesSync()
            ReminderScheduler.syncReminders(
                context = getApplication(),
                plans = allPlans,
                notificationsEnabled = prefs?.notificationsEnabled == true
            )
            WorkoutWidgetProvider.updateAllWidgets(getApplication())
            syncClubsInBackground()
            val safetyNote = if (safetyFile != null) " (previous data safely snapshot locally)" else ""
            onResult(true, "Restored ${payload.sessions.size} workouts & settings$safetyNote.")
        }
    }

    private suspend fun syncClubsInBackground() {
        val currentUser = try {
            Firebase.auth.currentUser
        } catch (_: Exception) {
            null
        } ?: return

        try {
            val allSessions = repository.getAllSessionsSync()
            val allPlans = repository.getAllDailyPlansSync()
            val stats = ClubMemberStats(
                displayName = nicknameStore.resolveDisplayName(currentUser.uid),
                weeklyPushUps = WorkoutRepository.calculateWeeklyPushUps(allSessions).toLong(),
                totalPushUps = allSessions.sumOf { it.totalPushUps }.toLong(),
                streakDays = repository.calculateStreak(allSessions, allPlans).toLong(),
                personalBest = (allSessions.maxOfOrNull { it.totalPushUps } ?: 0).toLong()
            )
            ClubRepository(getApplication()).syncStatsToUserAndClubs(stats)
        } catch (_: Exception) {
            // Offline-first: Clubs remain synced whenever connectivity is available or daily WorkManager runs
        }
    }

    fun startWorkoutFromWidget() {
        viewModelScope.launch {
            val prefs = repository.getUserPreferencesSync()
            if (prefs?.hasCompletedOnboarding == false) {
                repository.markOnboardingCompleted()
            }
            if (_workoutState.value == "IDLE") {
                startManualWorkout()
            }
        }
    }

    fun recordShareInitiated() {
        milestoneTracker.recordShareInitiated()
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager.unregisterListener(this)
        tts?.stop()
        tts?.shutdown()
    }

    companion object {
        const val SAFETY_BACKUP_FILENAME = "pre_restore_safety_backup.json"
    }
}
