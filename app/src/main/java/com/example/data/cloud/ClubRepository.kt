package com.example.data.cloud

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

class ClubRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
    private val joinCodeGenerator: () -> String = { generateJoinCode() }
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Please sign in with Google before accessing Pushup Clubs.")
    }

    fun observeUserClubs(userId: String): Flow<List<Club>> {
        val path = COLLECTION_CLUBS
        return db.collection(COLLECTION_CLUBS)
            .whereArrayContains("memberUids", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Club::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.name.lowercase() }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun createClub(
        clubName: String,
        stats: ClubMemberStats
    ): Result<Club> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanName = clubName.trim().take(40)
        if (cleanName.length < 3) {
            return Result.failure(IllegalArgumentException("Club name must be at least 3 characters."))
        }

        val cleanStats = stats.toPayloadMap()
        repeat(MAX_CODE_ATTEMPTS) {
            val code = joinCodeGenerator()
            val docRef = db.collection(COLLECTION_CLUBS).document(code)
            val payload = mapOf(
                "name" to cleanName,
                "joinCode" to code,
                "ownerId" to uid,
                "memberUids" to listOf(uid),
                "memberStats" to mapOf(uid to cleanStats),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                val reserved = db.runTransaction { transaction ->
                    if (transaction.get(docRef).exists()) {
                        false
                    } else {
                        transaction.set(docRef, payload)
                        true
                    }
                }.await()
                if (!reserved) return@repeat
                val created = docRef.get().await()
                    .toObject(Club::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: Club(
                        id = code,
                        name = cleanName,
                        joinCode = code,
                        ownerId = uid,
                        memberUids = listOf(uid),
                        memberStats = mapOf(uid to stats)
                    )
                return Result.success(created)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, docRef.path)
                return Result.failure(e)
            }
        }
        return Result.failure(IllegalStateException("Unable to reserve a club code. Please try again."))
    }

    /**
     * Atomically joins a club using a Firestore transaction so concurrent joins from multiple
     * members never overwrite each other's memberUids or trip security rule size checks.
     */
    suspend fun joinClub(
        rawJoinCode: String,
        stats: ClubMemberStats
    ): Result<Club> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val code = rawJoinCode.trim().uppercase().filter { it.isLetterOrDigit() }
        if (code.length !in 6..12) {
            return Result.failure(IllegalArgumentException("Enter a valid 6-character club code."))
        }

        val docRef = db.collection(COLLECTION_CLUBS).document(code)
        val statsMap = stats.toPayloadMap()
        return try {
            val joined = db.runTransaction { transaction ->
                val snap = transaction.get(docRef)
                if (!snap.exists()) {
                    throw IllegalArgumentException("No club found for code $code.")
                }
                val club = snap.toObject(Club::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("Unable to read club $code.")

                if (uid in club.memberUids) {
                    transaction.update(
                        docRef,
                        mapOf(
                            "memberStats.$uid" to statsMap,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    club.copy(
                        memberStats = club.memberStats + (uid to stats)
                    )
                } else {
                    if (club.memberUids.size >= 50) {
                        throw IllegalStateException("This club already has the maximum 50 members.")
                    }
                    val updatedMembers = club.memberUids + uid
                    transaction.update(
                        docRef,
                        mapOf(
                            "memberUids" to updatedMembers,
                            "memberStats.$uid" to statsMap,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    club.copy(
                        memberUids = updatedMembers,
                        memberStats = club.memberStats + (uid to stats)
                    )
                }
            }.await()
            Result.success(joined)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (e !is IllegalArgumentException && e !is IllegalStateException) {
                handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            }
            Result.failure(e)
        }
    }

    /**
     * Syncs the user's leaderboard metrics across all joined clubs.
     * Legacy /users/{uid} documents are only updated if already present so no redundant profile docs are created.
     */
    suspend fun syncStatsToUserAndClubs(stats: ClubMemberStats): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanStats = stats.toPayloadMap()
        return try {
            val joinedClubs = db.collection(COLLECTION_CLUBS)
                .whereArrayContains("memberUids", uid)
                .get()
                .await()

            joinedClubs.documents.forEach { clubDoc ->
                db.collection(COLLECTION_CLUBS).document(clubDoc.id).update(
                    mapOf(
                        "memberStats.$uid" to cleanStats,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "$COLLECTION_CLUBS/$uid")
            Result.failure(e)
        }
    }

    /**
     * Transactionally removes the caller from a club. If the caller is the owner:
     * - transfers ownership to another member when others remain, OR
     * - deletes the club if the caller is the sole remaining member.
     */
    suspend fun leaveClub(clubId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection(COLLECTION_CLUBS).document(clubId)
        return try {
            db.runTransaction { transaction ->
                val snap = transaction.get(docRef)
                val club = snap.toObject(Club::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalArgumentException("Club not found.")

                val remainingUids = club.memberUids.filter { it != uid }
                if (club.ownerId == uid) {
                    if (remainingUids.isEmpty()) {
                        transaction.delete(docRef)
                    } else {
                        val nextOwnerId = remainingUids.first()
                        val remainingStats = club.memberStats
                            .filterKeys { it != uid }
                            .mapValues { (_, v) -> v.toPayloadMap() }
                        transaction.update(
                            docRef,
                            mapOf(
                                "ownerId" to nextOwnerId,
                                "memberUids" to remainingUids,
                                "memberStats" to remainingStats,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                } else {
                    val remainingStats = club.memberStats
                        .filterKeys { it != uid }
                        .mapValues { (_, v) -> v.toPayloadMap() }
                    transaction.update(
                        docRef,
                        mapOf(
                            "memberUids" to remainingUids,
                            "memberStats" to remainingStats,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (e !is IllegalArgumentException) {
                handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            }
            Result.failure(e)
        }
    }

    suspend fun deleteClub(clubId: String): Result<Unit> {
        try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection(COLLECTION_CLUBS).document(clubId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Deletes all cloud data owned by or referencing the signed-in user (leaves/transfers/deletes
     * all clubs, removes any /users/{uid} document, and deletes the Firebase Auth account).
     * Local workout history on-device remains intact.
     */
    suspend fun deleteCloudAccountAndClubData(): Result<Unit> {
        val currentUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("Sign in first to delete your cloud club account."))
        val uid = currentUser.uid

        return try {
            val joinedClubs = db.collection(COLLECTION_CLUBS)
                .whereArrayContains("memberUids", uid)
                .get()
                .await()

            for (clubDoc in joinedClubs.documents) {
                leaveClub(clubDoc.id).getOrThrow()
            }

            // Clean up any legacy private user doc if one exists
            db.collection(COLLECTION_USERS).document(uid).delete().await()

            // Permanently remove Firebase Authentication identity
            currentUser.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            Result.failure(
                IllegalStateException(
                    "For security, please Sign Out and Sign In with Google once, then tap Delete Account again."
                )
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "$COLLECTION_USERS/$uid")
            Result.failure(e)
        }
    }

    companion object {
        private const val MAX_CODE_ATTEMPTS = 5
        private const val COLLECTION_CLUBS = "clubs"
        private const val COLLECTION_USERS = "users"
        private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        private val random = SecureRandom()

        fun generateJoinCode(length: Int = 6): String {
            return buildString(length) {
                repeat(length) {
                    append(CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)])
                }
            }
        }
    }
}
