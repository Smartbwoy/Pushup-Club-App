package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.cloud.Club
import com.example.data.cloud.ClubLeaderboardRow
import com.example.data.cloud.ClubMemberStats
import com.example.data.cloud.ClubRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ClubUiState {
    data object Loading : ClubUiState
    data class Success(val clubs: List<Club>) : ClubUiState
    data class Error(val message: String) : ClubUiState
}

class ClubViewModel(
    private val repository: ClubRepository,
    private val currentUserId: String
) : ViewModel() {

    val clubsState: StateFlow<ClubUiState> = repository.observeUserClubs(currentUserId)
        .map<List<Club>, ClubUiState> { ClubUiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            emit(ClubUiState.Error(rootError.localizedMessage ?: "Unable to load your clubs right now."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = ClubUiState.Loading
        )

    private val _actionBusy = MutableStateFlow(false)
    val actionBusy: StateFlow<Boolean> = _actionBusy.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    fun clearFeedback() {
        _actionMessage.value = null
        _actionError.value = null
    }

    fun createClub(clubName: String, stats: ClubMemberStats) {
        if (_actionBusy.value) return
        viewModelScope.launch {
            _actionBusy.value = true
            clearFeedback()
            repository.createClub(clubName, stats)
                .onSuccess { created ->
                    _actionMessage.value = "Created \"${created.name}\" • Invite Code: ${created.joinCode}"
                }
                .onFailure { err ->
                    _actionError.value = err.localizedMessage ?: "Could not create club."
                }
            _actionBusy.value = false
        }
    }

    fun joinClub(joinCode: String, stats: ClubMemberStats) {
        if (_actionBusy.value) return
        viewModelScope.launch {
            _actionBusy.value = true
            clearFeedback()
            repository.joinClub(joinCode, stats)
                .onSuccess { club ->
                    _actionMessage.value = "Joined \"${club.name}\"!"
                }
                .onFailure { err ->
                    _actionError.value = err.localizedMessage ?: "Could not join club."
                }
            _actionBusy.value = false
        }
    }

    fun syncStats(stats: ClubMemberStats, silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) {
                _actionBusy.value = true
                clearFeedback()
            }
            repository.syncStatsToUserAndClubs(stats)
                .onSuccess {
                    if (!silent) {
                        _actionMessage.value = "Synced weekly progress to your clubs."
                    }
                }
                .onFailure { err ->
                    if (!silent) {
                        _actionError.value = err.localizedMessage ?: "Failed to sync club stats."
                    }
                }
            if (!silent) {
                _actionBusy.value = false
            }
        }
    }

    fun leaveClub(clubId: String) {
        if (_actionBusy.value) return
        viewModelScope.launch {
            _actionBusy.value = true
            clearFeedback()
            repository.leaveClub(clubId)
                .onSuccess {
                    _actionMessage.value = "Left club cleanly."
                }
                .onFailure { err ->
                    _actionError.value = err.localizedMessage ?: "Could not leave club."
                }
            _actionBusy.value = false
        }
    }

    fun deleteClub(clubId: String) {
        if (_actionBusy.value) return
        viewModelScope.launch {
            _actionBusy.value = true
            clearFeedback()
            repository.deleteClub(clubId)
                .onSuccess {
                    _actionMessage.value = "Club deleted."
                }
                .onFailure { err ->
                    _actionError.value = err.localizedMessage ?: "Could not delete club."
                }
            _actionBusy.value = false
        }
    }

    fun deleteCloudAccount(onSuccess: () -> Unit) {
        if (_actionBusy.value) return
        viewModelScope.launch {
            _actionBusy.value = true
            clearFeedback()
            repository.deleteCloudAccountAndClubData()
                .onSuccess {
                    _actionBusy.value = false
                    onSuccess()
                }
                .onFailure { err ->
                    _actionError.value = err.localizedMessage ?: "Could not delete cloud account."
                    _actionBusy.value = false
                }
        }
    }

    companion object {
        fun provideFactory(
            appContext: Context,
            userId: String
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ClubViewModel(
                    repository = ClubRepository(appContext),
                    currentUserId = userId
                )
            }
        }

        fun buildLeaderboardRows(club: Club, currentUserId: String): List<ClubLeaderboardRow> {
            return club.memberUids.distinct().map { uid ->
                val stats = club.memberStats[uid] ?: ClubMemberStats(displayName = "Athlete")
                ClubLeaderboardRow(
                    uid = uid,
                    displayName = stats.displayName.ifBlank { "Athlete" },
                    weeklyPushUps = stats.weeklyPushUps.toInt(),
                    totalPushUps = stats.totalPushUps.toInt(),
                    streakDays = stats.streakDays.toInt(),
                    personalBest = stats.personalBest.toInt(),
                    isCurrentUser = (uid == currentUserId),
                    isOwner = (uid == club.ownerId)
                )
            }.sortedWith(
                compareByDescending<ClubLeaderboardRow> { it.weeklyPushUps }
                    .thenByDescending { it.totalPushUps }
                    .thenBy { it.displayName.lowercase() }
            )
        }
    }
}
