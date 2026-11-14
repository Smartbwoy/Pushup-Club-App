package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.cloud.Club
import com.example.data.cloud.ClubLeaderboardRow
import com.example.data.cloud.ClubMemberStats
import com.example.data.cloud.GoogleAuthHelper
import com.example.data.cloud.authStateFlow
import com.example.ui.ClubUiState
import com.example.ui.ClubViewModel
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
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun ClubsScreen(
    workoutViewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val authStateFlow = remember { Firebase.auth.authStateFlow() }
    val currentUser by authStateFlow.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    val weeklyPushUps by workoutViewModel.weeklyPushUps.collectAsState()
    val totalPushUps by workoutViewModel.totalPushUps.collectAsState()
    val currentStreak by workoutViewModel.currentStreak.collectAsState()
    val personalBest by workoutViewModel.personalBest.collectAsState()

    var authError by remember { mutableStateOf<String?>(null) }
    var authLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            GoogleAuthHelper.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                scope = scope,
                onAuthSuccess = { authError = null },
                onUnauthenticated = { }
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBlack)
    ) {
        if (currentUser == null) {
            ClubSignInContent(
                isLoading = authLoading,
                errorMessage = authError,
                onSignInClick = {
                    authLoading = true
                    authError = null
                    GoogleAuthHelper.onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        scope = scope,
                        onAuthSuccess = {
                            authLoading = false
                            authError = null
                        },
                        onAuthError = { msg ->
                            authLoading = false
                            authError = msg
                        },
                        onAuthCancelled = {
                            authLoading = false
                        }
                    )
                }
            )
        } else {
            val uid = currentUser!!.uid
            val rawName = currentUser?.displayName?.trim().orEmpty()
            val fallbackEmailName = currentUser?.email?.substringBefore("@")?.trim().orEmpty()
            val displayName = rawName.ifEmpty { fallbackEmailName.ifEmpty { "Athlete" } }.take(30)

            val stats = remember(displayName, weeklyPushUps, totalPushUps, currentStreak, personalBest) {
                ClubMemberStats(
                    displayName = displayName,
                    weeklyPushUps = weeklyPushUps.toLong(),
                    totalPushUps = totalPushUps.toLong(),
                    streakDays = currentStreak.toLong(),
                    personalBest = personalBest.toLong()
                )
            }

            key(uid) {
                val clubViewModel: ClubViewModel = viewModel(
                    key = uid,
                    factory = ClubViewModel.provideFactory(context.applicationContext, uid)
                )
                AuthenticatedClubsContent(
                    uid = uid,
                    displayName = displayName,
                    myStats = stats,
                    clubViewModel = clubViewModel,
                    onSignOut = {
                        GoogleAuthHelper.signOut(credentialManager, scope)
                    }
                )
            }
        }
    }
}

@Composable
private fun ClubSignInContent(
    isLoading: Boolean,
    errorMessage: String?,
    onSignInClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "PUSHUP CLUBS",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = TextWhite,
                letterSpacing = 1.sp
            )
            Text(
                text = "TRAIN TOGETHER WITH SHAREABLE INVITE CODES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextGray
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, NeonGreen.copy(0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(NeonGreen.copy(0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Compete With Friends",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = TextWhite,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Solo workouts always stay private and offline. Sign in with Google only when you want to create a club, join with a 6-character code, and compare weekly reps on a live leaderboard.",
                        fontSize = 13.sp,
                        color = TextGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp
                    )

                    if (!errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = FireOrange.copy(0.14f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, FireOrange.copy(0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage,
                                color = FireOrange,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onSignInClick,
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            contentColor = Color(0xFF003818)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color(0xFF003818),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = "Sign in with Google",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedClubsContent(
    uid: String,
    displayName: String,
    myStats: ClubMemberStats,
    clubViewModel: ClubViewModel,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val clubsState by clubViewModel.clubsState.collectAsStateWithLifecycle()
    val actionBusy by clubViewModel.actionBusy.collectAsStateWithLifecycle()
    val actionMessage by clubViewModel.actionMessage.collectAsStateWithLifecycle()
    val actionError by clubViewModel.actionError.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var clubToLeave by remember { mutableStateOf<Club?>(null) }
    var clubToDelete by remember { mutableStateOf<Club?>(null) }

    // Keep club leaderboard stats fresh whenever weekly/total/streak/best updates while signed in
    LaunchedEffect(myStats) {
        clubViewModel.syncStats(myStats, silent = true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PUSHUP CLUBS",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = TextWhite,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Signed in as $displayName • ${myStats.weeklyPushUps} reps this week",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = { clubViewModel.syncStats(myStats, silent = false) },
                        enabled = !actionBusy,
                        modifier = Modifier
                            .size(48.dp)
                            .background(CardSurfaceElevated, CircleShape)
                            .testTag("sync_club_stats_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync stats to clubs",
                            tint = AquaBlue
                        )
                    }

                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .size(48.dp)
                            .background(CardSurfaceElevated, CircleShape)
                            .testTag("club_sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sign out of Clubs",
                            tint = FireOrange
                        )
                    }
                }
            }
        }

        // Primary Create / Join actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showCreateDialog = true },
                    enabled = !actionBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        contentColor = Color(0xFF003818)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("create_club_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Create Club", fontWeight = FontWeight.Black, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = { showJoinDialog = true },
                    enabled = !actionBusy,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, AquaBlue.copy(0.6f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("join_club_button")
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = AquaBlue, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Join Code", color = AquaBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        if (!actionMessage.isNullOrBlank()) {
            item {
                Surface(
                    color = NeonGreen.copy(0.14f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NeonGreen.copy(0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = actionMessage.orEmpty(),
                            color = NeonGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { clubViewModel.clearFeedback() }) {
                            Text("OK", color = NeonGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (!actionError.isNullOrBlank()) {
            item {
                Surface(
                    color = FireOrange.copy(0.14f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, FireOrange.copy(0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = actionError.orEmpty(),
                            color = FireOrange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { clubViewModel.clearFeedback() }) {
                            Text("Dismiss", color = FireOrange, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        when (val state = clubsState) {
            is ClubUiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonGreen)
                    }
                }
            }

            is ClubUiState.Error -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, FireOrange.copy(0.4f))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Could Not Sync Clubs",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = FireOrange
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = state.message, fontSize = 12.sp, color = TextGray)
                        }
                    }
                }
            }

            is ClubUiState.Success -> {
                if (state.clubs.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color.White.copy(0.08f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Leaderboard,
                                    contentDescription = null,
                                    tint = AquaBlue,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No Clubs Yet",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Create your own Pushup Club to get a 6-character invite code, or enter a friend's code to join their weekly leaderboard.",
                                    fontSize = 12.sp,
                                    color = TextGray,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                } else {
                    items(state.clubs, key = { it.id.ifBlank { it.joinCode } }) { club ->
                        ClubLeaderboardCard(
                            club = club,
                            currentUserId = uid,
                            onShareInvite = {
                                val inviteText = "Join my Pushup Club \"${club.name}\"!\n" +
                                    "Open Pushup Club -> Clubs -> Join Code and enter: ${club.joinCode}"
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_TEXT, inviteText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Club Invite"))
                            },
                            onLeaveClub = { clubToLeave = club },
                            onDeleteClub = { clubToDelete = club }
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showCreateDialog) {
        CreateClubDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                clubViewModel.createClub(name, myStats)
                showCreateDialog = false
            }
        )
    }

    if (showJoinDialog) {
        JoinClubDialog(
            onDismiss = { showJoinDialog = false },
            onJoin = { code ->
                clubViewModel.joinClub(code, myStats)
                showJoinDialog = false
            }
        )
    }

    clubToLeave?.let { club ->
        AlertDialog(
            onDismissRequest = { clubToLeave = null },
            title = { Text("Leave \"${club.name}\"?", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "You can rejoin later using invite code ${club.joinCode}.",
                    color = TextGray,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        clubViewModel.leaveClub(club.id.ifBlank { club.joinCode })
                        clubToLeave = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FireOrange)
                ) {
                    Text("Leave", color = PitchBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { clubToLeave = null }) {
                    Text("Cancel", color = TextWhite)
                }
            },
            containerColor = CardSurfaceElevated
        )
    }

    clubToDelete?.let { club ->
        AlertDialog(
            onDismissRequest = { clubToDelete = null },
            title = { Text("Delete \"${club.name}\"?", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This removes the club and leaderboard for all members.",
                    color = TextGray,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        clubViewModel.deleteClub(club.id.ifBlank { club.joinCode })
                        clubToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FireOrange)
                ) {
                    Text("Delete", color = PitchBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { clubToDelete = null }) {
                    Text("Cancel", color = TextWhite)
                }
            },
            containerColor = CardSurfaceElevated
        )
    }
}

@Composable
private fun ClubLeaderboardCard(
    club: Club,
    currentUserId: String,
    onShareInvite: () -> Unit,
    onLeaveClub: () -> Unit,
    onDeleteClub: () -> Unit
) {
    val rows = remember(club, currentUserId) {
        ClubViewModel.buildLeaderboardRows(club, currentUserId)
    }
    val isOwner = club.ownerId == currentUserId

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color.White.copy(0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = club.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(AquaBlue.copy(0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "CODE: ${club.joinCode}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = AquaBlue,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = "${rows.size} member${if (rows.size == 1) "" else "s"}",
                            fontSize = 11.sp,
                            color = TextGray
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onShareInvite,
                        modifier = Modifier
                            .size(48.dp)
                            .background(CardSurfaceElevated, CircleShape)
                            .testTag("share_club_${club.joinCode}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share club invite code",
                            tint = AquaBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = if (isOwner) onDeleteClub else onLeaveClub,
                        modifier = Modifier
                            .size(48.dp)
                            .background(CardSurfaceElevated, CircleShape)
                            .testTag("club_action_${club.joinCode}")
                    ) {
                        Icon(
                            imageVector = if (isOwner) Icons.Default.DeleteOutline else Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = if (isOwner) "Delete club" else "Leave club",
                            tint = FireOrange,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color.White.copy(0.08f)
            )

            Text(
                text = "WEEKLY LEADERBOARD",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = TextGray,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            rows.forEachIndexed { index, row ->
                ClubLeaderboardRowItem(rank = index + 1, row = row)
                if (index < rows.lastIndex) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun ClubLeaderboardRowItem(
    rank: Int,
    row: ClubLeaderboardRow
) {
    val highlightColor = when {
        rank == 1 -> NeonGreen
        row.isCurrentUser -> AquaBlue
        else -> Color.White.copy(0.06f)
    }
    Surface(
        color = if (row.isCurrentUser) DarkSurface else CardSurfaceElevated.copy(alpha = 0.55f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, highlightColor.copy(alpha = if (row.isCurrentUser || rank == 1) 0.4f else 0.12f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "#$rank",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = if (rank == 1) NeonGreen else TextGray,
                    modifier = Modifier.width(30.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = row.displayName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (row.isCurrentUser) {
                            Box(
                                modifier = Modifier
                                    .background(AquaBlue.copy(0.18f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text("YOU", fontSize = 8.sp, fontWeight = FontWeight.Black, color = AquaBlue)
                            }
                        }
                        if (row.isOwner) {
                            Box(
                                modifier = Modifier
                                    .background(ElectricEmerald.copy(0.18f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text("HOST", fontSize = 8.sp, fontWeight = FontWeight.Black, color = ElectricEmerald)
                            }
                        }
                    }
                    Text(
                        text = "${row.streakDays}d streak • ${row.personalBest} max • ${row.totalPushUps} total",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${row.weeklyPushUps}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = if (rank == 1) NeonGreen else TextWhite
                )
                Text(
                    text = "7d reps",
                    fontSize = 9.sp,
                    color = TextGray
                )
            }
        }
    }
}

@Composable
private fun CreateClubDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var clubName by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create a Pushup Club", color = TextWhite, fontWeight = FontWeight.Black) },
        text = {
            Column {
                Text(
                    text = "Start a weekly leaderboard and invite friends with a 6-character code.",
                    fontSize = 12.sp,
                    color = TextGray
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = clubName,
                    onValueChange = { clubName = it.take(40); errorMsg = "" },
                    label = { Text("Club Name (e.g. Morning Crew)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = Color.White.copy(0.2f),
                        focusedLabelColor = NeonGreen
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_club_name_input")
                )
                if (errorMsg.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = errorMsg, color = FireOrange, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = clubName.trim()
                    if (trimmed.length < 3) {
                        errorMsg = "Enter at least 3 characters."
                    } else {
                        onCreate(trimmed)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                modifier = Modifier.testTag("confirm_create_club")
            ) {
                Text("Create", color = Color(0xFF003818), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextWhite)
            }
        },
        containerColor = CardSurfaceElevated
    )
}

@Composable
private fun JoinClubDialog(
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    var joinCode by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join a Pushup Club", color = TextWhite, fontWeight = FontWeight.Black) },
        text = {
            Column {
                Text(
                    text = "Enter the 6-character invite code shared by your friend.",
                    fontSize = 12.sp,
                    color = TextGray
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = joinCode,
                    onValueChange = {
                        joinCode = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(12)
                        errorMsg = ""
                    },
                    label = { Text("Invite Code (e.g. K7M9P2)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AquaBlue,
                        unfocusedBorderColor = Color.White.copy(0.2f),
                        focusedLabelColor = AquaBlue
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("join_club_code_input")
                )
                if (errorMsg.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = errorMsg, color = FireOrange, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (joinCode.length < 6) {
                        errorMsg = "Enter a valid 6-character code."
                    } else {
                        onJoin(joinCode)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AquaBlue),
                modifier = Modifier.testTag("confirm_join_club")
            ) {
                Text("Join", color = PitchBlack, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextWhite)
            }
        },
        containerColor = CardSurfaceElevated
    )
}
