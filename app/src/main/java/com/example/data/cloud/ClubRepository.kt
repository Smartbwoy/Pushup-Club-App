package com.example.data.cloud

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
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
    private val auth: FirebaseAuth = Firebase.auth
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
        val code = generateJoinCode()
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
        return try {
            docRef.set(payload).await()
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
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

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
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                return Result.failure(IllegalArgumentException("No club found for code $code."))
            }
            val club = snap.toObject(Club::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return Result.failure(IllegalStateException("Unable to read club $code."))

            val statsMap = stats.toPayloadMap()
            if (uid in club.memberUids) {
                docRef.update(
                    mapOf(
                        "memberStats.$uid" to statsMap,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                val updated = docRef.get().await()
                    .toObject(Club::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: club
                return Result.success(updated)
            }

            if (club.memberUids.size >= 50) {
                return Result.failure(IllegalStateException("This club already has the maximum 50 members."))
            }

            val updatedMembers = club.memberUids + uid
            docRef.update(
                mapOf(
                    "memberUids" to updatedMembers,
                    "memberStats.$uid" to statsMap,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            val joined = docRef.get().await()
                .toObject(Club::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: club.copy(memberUids = updatedMembers)
            Result.success(joined)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun syncStatsToUserAndClubs(stats: ClubMemberStats): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanStats = stats.toPayloadMap()
        val userDocRef = db.collection(COLLECTION_USERS).document(uid)
        return try {
            val existingProfile = userDocRef.get().await()
            if (!existingProfile.exists()) {
                val createPayload = cleanStats.toMutableMap().apply {
                    put("userId", uid)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                userDocRef.set(createPayload).await()
            } else {
                val updatePayload = cleanStats.toMutableMap().apply {
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                userDocRef.update(updatePayload as Map<String, Any>).await()
            }

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
            handleFirestoreError(e, OperationType.WRITE, userDocRef.path)
            Result.failure(e)
        }
    }

    suspend fun leaveClub(clubId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection(COLLECTION_CLUBS).document(clubId)
        return try {
            val snap = docRef.get().await()
            val club = snap.toObject(Club::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return Result.failure(IllegalArgumentException("Club not found."))
            if (club.ownerId == uid) {
                return Result.failure(IllegalStateException("Club creators cannot leave—delete the club instead."))
            }
            val remainingUids = club.memberUids.filter { it != uid }
            val remainingStats = club.memberStats
                .filterKeys { it != uid }
                .mapValues { (_, v) -> v.toPayloadMap() }

            docRef.update(
                mapOf(
                    "memberUids" to remainingUids,
                    "memberStats" to remainingStats,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
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

    companion object {
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
