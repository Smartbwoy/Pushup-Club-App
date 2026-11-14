package com.example.data.cloud

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClubRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndJoinClub_authenticatedUsers_succeedsAndStreamsRealtimeUpdates() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = ClubRepository(firestore, auth)
        val aliceStats = ClubMemberStats(
            displayName = "Alice",
            weeklyPushUps = 80,
            totalPushUps = 240,
            streakDays = 4,
            personalBest = 30
        )

        val createdClub = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createClub("Morning Club", aliceStats).getOrThrow()
        }
        assertTrue(createdClub.joinCode.length == 6)
        assertTrue(aliceUid in createdClub.memberUids)

        // Observe via Flow
        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeUserClubs(aliceUid).first { list -> list.any { it.joinCode == createdClub.joinCode } }
        }
        assertTrue(observed.any { it.name == "Morning Club" })

        // Bob joins by joinCode
        val bobUid = signInTestUser(BOB_EMAIL)
        val bobRepo = ClubRepository(firestore, auth)
        val bobStats = ClubMemberStats(
            displayName = "Bob",
            weeklyPushUps = 65,
            totalPushUps = 150,
            streakDays = 2,
            personalBest = 22
        )
        val joined = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.joinClub(createdClub.joinCode, bobStats).getOrThrow()
        }
        assertEquals(2, joined.memberUids.size)
        assertTrue(bobUid in joined.memberUids)

        // Sync stats for Bob
        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.syncStatsToUserAndClubs(bobStats.copy(weeklyPushUps = 95)).getOrThrow()
        }

        // Bob leaves cleanly
        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.leaveClub(createdClub.joinCode).getOrThrow()
        }

        // Alice deletes club
        signInTestUser(ALICE_EMAIL)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.deleteClub(createdClub.joinCode).getOrThrow()
        }
    }

    @Test
    fun observeUserClubs_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = ClubRepository(firestore, auth)
        var caught: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeUserClubs("someone_else").first()
            }
        } catch (e: Exception) {
            caught = e
        }
        assertNotNull(caught)
        val firestoreException = caught!!.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caught", firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException!!.code)
    }

    @Test
    fun deleteClub_nonOwnerMember_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = ClubRepository(firestore, auth)
        val club = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createClub("Pushup Elite", ClubMemberStats("Alice", 50, 100, 1, 20)).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = ClubRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.joinClub(club.joinCode, ClubMemberStats("Bob", 40, 90, 1, 18)).getOrThrow()
        }

        val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.deleteClub(club.joinCode)
        }
        assertTrue(deleteResult.isFailure)
        val fsEx = deleteResult.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)

        // Cleanup as Alice
        signInTestUser(ALICE_EMAIL)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.deleteClub(club.joinCode).getOrThrow()
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_club@test.com"
        const val BOB_EMAIL = "bob_club@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
