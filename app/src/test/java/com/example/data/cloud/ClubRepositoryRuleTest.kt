package com.example.data.cloud

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
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

    @Test
    fun leaveClub_ownerWithOtherMembers_transfersOwnershipCleanly() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = ClubRepository(firestore, auth)
        val club = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createClub("Relay Club", ClubMemberStats("Athlete-ALICE", 70, 140, 3, 25)).getOrThrow()
        }

        val bobUid = signInTestUser(BOB_EMAIL)
        val bobRepo = ClubRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.joinClub(club.joinCode, ClubMemberStats("Athlete-BOB", 60, 120, 2, 20)).getOrThrow()
        }

        // Alice (owner) leaves -> ownership transfers transactionally to Bob
        val switchedAliceUid = signInTestUser(ALICE_EMAIL)
        assertEquals(aliceUid, switchedAliceUid)
        auth.currentUser?.getIdToken(true)?.await()
        val leaveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.leaveClub(club.joinCode)
        }
        if (leaveResult.isFailure) {
            throw AssertionError(
                "aliceRepo.leaveClub failed for aliceUid=$aliceUid, bobUid=$bobUid, currentUid=${auth.currentUser?.uid}, club=$club: ${leaveResult.exceptionOrNull()}",
                leaveResult.exceptionOrNull()
            )
        }

        // Bob is now the owner and can observe and delete the club (or leave as sole member to delete)
        signInTestUser(BOB_EMAIL)
        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeUserClubs(bobUid).first { list -> list.any { it.joinCode == club.joinCode } }
        }
        val transferredClub = observed.first { it.joinCode == club.joinCode }
        assertEquals(bobUid, transferredClub.ownerId)
        assertTrue(aliceUid !in transferredClub.memberUids)
        assertTrue(bobUid in transferredClub.memberUids)

        // Sole remaining owner leaving deletes the club cleanly
        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.leaveClub(club.joinCode).getOrThrow()
        }
        val afterLeave = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeUserClubs(bobUid).first { list -> list.none { it.joinCode == club.joinCode } }
        }
        assertTrue(afterLeave.none { it.joinCode == club.joinCode })
    }

    @Test
    fun deleteCloudAccountAndClubData_removesClubsAndDeletesFirebaseUser() = runBlocking {
        val tempEmail = "delete_me_club@test.com"
        val tempUid = signInTestUser(tempEmail)
        val tempRepo = ClubRepository(firestore, auth)
        val club = withTimeout(DEFAULT_TIMEOUT_MS) {
            tempRepo.createClub("Temp Delete Club", ClubMemberStats("Athlete-9999", 20, 40, 1, 15)).getOrThrow()
        }
        assertTrue(tempUid in club.memberUids)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            tempRepo.deleteCloudAccountAndClubData().getOrThrow()
        }
        assertEquals(null, auth.currentUser)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_club@test.com"
        const val BOB_EMAIL = "bob_club@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
