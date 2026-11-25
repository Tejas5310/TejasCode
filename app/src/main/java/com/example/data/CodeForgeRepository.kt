package com.example.data

import android.content.Context
import com.example.R
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.Certificate
import com.example.model.CodeSnippet
import com.example.model.CommunityProject
import com.example.model.CommunityQuestion
import com.example.model.LeaderboardEntry
import com.example.model.ManagedCourse
import com.example.model.ModerationReport
import com.example.model.PaymentTransaction
import com.example.model.PlanConfig
import com.example.model.PlatformNotification
import com.example.model.QuestionReply
import com.example.model.SettlementRecord
import com.example.model.SubscriptionRecord
import com.example.model.UserEntitlement
import com.example.model.UserProfile
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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CodeForgeRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun todayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private fun nowIsoString(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
    }

    private fun unwrapAndLogFlowError(
        error: Throwable,
        op: OperationType,
        path: String,
        onlyIfUid: String? = null
    ): Throwable {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        val isAuthorizedContext = if (onlyIfUid != null) auth.currentUser?.uid == onlyIfUid else auth.currentUser != null
        if (exceptionToLog != null && isAuthorizedContext) {
            handleFirestoreError(exceptionToLog, op, path)
        }
        return error
    }

    suspend fun ensureUserProfile(displayName: String, photoUrl: String?): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Unauthenticated"))
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            val safeName = displayName.trim().ifBlank { "Tejas" }.take(60)
            val today = todayDateString()
            val loginStamp = nowIsoString()
            if (!snap.exists()) {
                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "displayName" to safeName,
                    "bio" to "",
                    "learningGoal" to "HTML, CSS & JavaScript",
                    "experienceLevel" to "Beginner",
                    "preferredLanguage" to "en",
                    "registered" to false,
                    "xp" to 50L,
                    "streakDays" to 1L,
                    "lessonsCompleted" to 0L,
                    "totalCodingMinutes" to 0L,
                    "battlesWon" to 0L,
                    "lastActiveDate" to today,
                    "lastLoginAt" to loginStamp,
                    "aiQueriesUsedToday" to 0L,
                    "aiQuotaDate" to today,
                    "completedLessonIds" to emptyList<String>(),
                    "savedCourseIds" to emptyList<String>(),
                    "badges" to listOf("First Steps"),
                    "followingIds" to emptyList<String>(),
                    "blockedUserIds" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!photoUrl.isNullOrBlank()) {
                    payload["photoUrl"] = photoUrl.take(500)
                }
                docRef.set(payload).await()

                val lbPayload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "xp" to 50L,
                    "streakDays" to 1L,
                    "battlesWon" to 0L,
                    "visibility" to "public",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("leaderboard").document(uid).set(lbPayload).await()
            } else {
                docRef.update(
                    mapOf(
                        "lastLoginAt" to loginStamp,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                val lbSnap = db.collection("leaderboard").document(uid).get().await()
                if (!lbSnap.exists()) {
                    val current = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    db.collection("leaderboard").document(uid).set(
                        mapOf(
                            "userId" to uid,
                            "displayName" to (current?.displayName?.ifBlank { safeName } ?: safeName),
                            "xp" to (current?.xp ?: 50L),
                            "streakDays" to (current?.streakDays ?: 1L),
                            "battlesWon" to (current?.battlesWon ?: 0L),
                            "visibility" to "public",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun completeStudentRegistration(
        displayName: String,
        learningGoal: String,
        experienceLevel: String
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            docPath = userRef.path
            val safeName = displayName.trim().ifBlank { "Student" }.take(60)
            val safeGoal = learningGoal.trim().ifBlank { "Full-Stack Web" }.take(60)
            val safeLevel = experienceLevel.trim().ifBlank { "Beginner" }.take(30)

            val snap = userRef.get().await()
            if (snap.exists()) {
                userRef.update(
                    mapOf(
                        "displayName" to safeName,
                        "learningGoal" to safeGoal,
                        "experienceLevel" to safeLevel,
                        "registered" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                userRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to safeName,
                        "learningGoal" to safeGoal,
                        "experienceLevel" to safeLevel,
                        "registered" to true,
                        "xp" to 50L,
                        "streakDays" to 1L,
                        "lessonsCompleted" to 0L,
                        "battlesWon" to 0L,
                        "lastActiveDate" to todayDateString(),
                        "completedLessonIds" to emptyList<String>(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            val current = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            syncLeaderboard(
                uid = uid,
                displayName = safeName,
                xp = current?.xp ?: 50L,
                streakDays = current?.streakDays ?: 1L,
                battlesWon = current?.battlesWon ?: 0L
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfileExtended(bio: String, preferredLanguage: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            docPath = userRef.path
            userRef.update(
                mapOf(
                    "bio" to bio.trim().take(240),
                    "preferredLanguage" to preferredLanguage.trim().ifBlank { "en" }.take(20),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleSaveCourse(courseId: String): Result<Boolean> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            docPath = userRef.path
            val snap = userRef.get().await()
            val current = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return Result.failure(IllegalStateException("Profile not found"))
            val alreadySaved = courseId in current.savedCourseIds
            val updated = if (alreadySaved) {
                current.savedCourseIds.filterNot { it == courseId }
            } else {
                (current.savedCourseIds + courseId).take(100)
            }
            userRef.update(
                mapOf(
                    "savedCourseIds" to updated,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(!alreadySaved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleFollowUser(targetUserId: String): Result<Boolean> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            if (targetUserId.isBlank() || targetUserId == uid) return Result.success(false)
            val userRef = db.collection("users").document(uid)
            docPath = userRef.path
            val snap = userRef.get().await()
            val current = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return Result.failure(IllegalStateException("Profile not found"))
            val following = targetUserId in current.followingIds
            val updated = if (following) {
                current.followingIds.filterNot { it == targetUserId }
            } else {
                (current.followingIds + targetUserId).take(200)
            }
            userRef.update(
                mapOf(
                    "followingIds" to updated,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(!following)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleBlockUser(targetUserId: String): Result<Boolean> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            if (targetUserId.isBlank() || targetUserId == uid) return Result.success(false)
            val userRef = db.collection("users").document(uid)
            docPath = userRef.path
            val snap = userRef.get().await()
            val current = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return Result.failure(IllegalStateException("Profile not found"))
            val blocked = targetUserId in current.blockedUserIds
            val updated = if (blocked) {
                current.blockedUserIds.filterNot { it == targetUserId }
            } else {
                (current.blockedUserIds + targetUserId).take(200)
            }
            userRef.update(
                mapOf(
                    "blockedUserIds" to updated,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(!blocked)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun consumeAiQuota(isProActive: Boolean, freeLimit: Long, proLimit: Long): Result<Pair<Long, Long>> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            docPath = userRef.path
            val snap = userRef.get().await()
            val current = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return Result.success(1L to freeLimit)
            val today = todayDateString()
            val limit = if (isProActive) proLimit.coerceAtLeast(1L) else freeLimit.coerceAtLeast(1L)
            val used = if (current.aiQuotaDate == today) current.aiQueriesUsedToday else 0L
            if (used >= limit) {
                return Result.failure(
                    IllegalStateException(
                        if (isProActive) "Daily AI Tutor limit ($limit) reached. Resets tomorrow."
                        else "Free daily AI limit ($limit/$limit) reached. Upgrade to TejasCode Pro for $proLimit daily AI runs."
                    )
                )
            }
            val nextUsed = used + 1L
            userRef.update(
                mapOf(
                    "aiQueriesUsedToday" to nextUsed,
                    "aiQuotaDate" to today,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(nextUsed to limit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error -> throw unwrapAndLogFlowError(error, OperationType.GET, path, onlyIfUid = userId) }
        )
    }

    fun observePlanConfig(): Flow<PlanConfig> = flow {
        val path = "plan_configs/active"
        emitAll(
            db.collection("plan_configs").document("active")
                .snapshots()
                .map { snap ->
                    snap.toObject(PlanConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: PlanConfig()
                }
                .catch { error -> throw unwrapAndLogFlowError(error, OperationType.GET, path) }
        )
    }

    suspend fun savePlanConfig(config: PlanConfig): Result<Unit> {
        val path = "plan_configs/active"
        return try {
            val uid = requireUserId()
            db.collection("plan_configs").document("active").set(
                mapOf(
                    "monthlyPriceMinor" to config.monthlyPriceMinor.coerceIn(0L, 10000000L),
                    "yearlyPriceMinor" to config.yearlyPriceMinor.coerceIn(0L, 100000000L),
                    "currency" to config.currency.trim().ifBlank { "INR" }.take(5),
                    "currencySymbol" to config.currencySymbol.trim().ifBlank { "₹" }.take(4),
                    "trialDays" to config.trialDays.coerceIn(0L, 90L),
                    "monthlyPlayProductId" to config.monthlyPlayProductId.trim().take(100),
                    "monthlyPlayBasePlanId" to config.monthlyPlayBasePlanId.trim().take(100),
                    "yearlyPlayProductId" to config.yearlyPlayProductId.trim().take(100),
                    "yearlyPlayBasePlanId" to config.yearlyPlayBasePlanId.trim().take(100),
                    "razorpayMonthlyPlanId" to config.razorpayMonthlyPlanId.trim().take(100),
                    "razorpayYearlyPlanId" to config.razorpayYearlyPlanId.trim().take(100),
                    "razorpayEnabled" to config.razorpayEnabled,
                    "freeDailyAiLimit" to config.freeDailyAiLimit.coerceIn(0L, 1000L),
                    "proDailyAiLimit" to config.proDailyAiLimit.coerceIn(0L, 10000L),
                    "billingTermsSummary" to config.billingTermsSummary.trim().take(1000),
                    "updatedBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeUserEntitlement(userId: String): Flow<UserEntitlement> = flow {
        val path = "users/$userId/entitlements/current"
        emitAll(
            db.collection("users").document(userId).collection("entitlements").document("current")
                .snapshots()
                .map { snap ->
                    snap.toObject(UserEntitlement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: UserEntitlement(userId = userId)
                }
                .catch { error -> throw unwrapAndLogFlowError(error, OperationType.GET, path, onlyIfUid = userId) }
        )
    }

    fun observeUserSubscriptions(userId: String): Flow<List<SubscriptionRecord>> = flow {
        val path = "subscriptions"
        emitAll(
            db.collection("subscriptions")
                .whereEqualTo("userId", userId)
                .limit(25)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(SubscriptionRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.currentPeriodEndMs }
                }
                .catch { error -> throw unwrapAndLogFlowError(error, OperationType.LIST, path, onlyIfUid = userId) }
        )
    }

    fun observeAllSubscriptionsForAdmin(): Flow<List<SubscriptionRecord>> = flow {
        val path = "subscriptions"
        emitAll(
            db.collection("subscriptions")
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(SubscriptionRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.currentPeriodEndMs }
                }
                .catch { error -> throw unwrapAndLogFlowError(error, OperationType.LIST, path) }
        )
    }

    fun observeUserTransactions(userId: String): Flow<List<PaymentTransaction>> = flow {
        val path = "payment_transactions"
        emitAll(
            db.collection("payment_transactions")
                .whereEqualTo("userId", userId)
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(PaymentTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAtMs }
                }
                .catch { error -> throw unwrapAndLogFlowError(error, OperationType.LIST, path, onlyIfUid = userId) }
        )
    }

    fun observeAllTransactionsForAdmin(): Flow<List<PaymentTransaction>> = flow {
        val path = "payment_transactions"
        emitAll(
            db.collection("payment_transactions")
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(PaymentTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAtMs }
                }
                .catch { error -> throw unwrapAndLogFlowError(error, OperationType.LIST, path) }
        )
    }

    fun observeSettlementsForAdmin(): Flow<List<SettlementRecord>> = flow {
        val path = "settlements"
        emitAll(
            db.collection("settlements")
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(SettlementRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.settledAtMs ?: 0L }
                }
                .catch { error -> throw unwrapAndLogFlowError(error, OperationType.LIST, path) }
        )
    }

    fun observePublishedCourses(): Flow<List<ManagedCourse>> = flow {
        val path = "courses"
        emitAll(
            db.collection("courses")
                .whereEqualTo("visibility", "public")
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ManagedCourse::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> throw unwrapAndLogFlowError(error, OperationType.LIST, path) }
        )
    }

    fun observeAllCoursesForAdmin(): Flow<List<ManagedCourse>> = flow {
        val path = "courses"
        emitAll(
            db.collection("courses")
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ManagedCourse::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error -> throw unwrapAndLogFlowError(error, OperationType.LIST, path) }
        )
    }

    suspend fun saveManagedCourse(
        courseId: String?,
        title: String,
        subtitle: String,
        language: String,
        category: String,
        difficulty: String,
        durationMinutes: Long,
        xpReward: Long,
        starterCode: String,
        lessonSummary: String,
        quizQuestion: String,
        quizOptions: List<String>,
        correctOptionIndex: Long,
        visibility: String,
        isPremium: Boolean = false,
        resourceUrl: String = ""
    ): Result<String> {
        var docPath = "courses"
        return try {
            val uid = requireUserId()
            val safeTitle = title.trim().ifBlank { "New Coding Course" }.take(80)
            val safeSubtitle = subtitle.trim().ifBlank { "Interactive TejasCode module" }.take(160)
            val safeOptions = quizOptions.map { it.trim() }.filter { it.isNotBlank() }
                .ifEmpty { listOf("True", "False") }
                .take(6)
            val safeCorrectIdx = correctOptionIndex.coerceIn(0L, (safeOptions.size - 1).toLong())
            val safeVis = if (visibility == "draft") "draft" else "public"
            val safeUrl = resourceUrl.trim().take(500)

            val col = db.collection("courses")
            val docRef = if (courseId.isNullOrBlank()) col.document() else col.document(courseId)
            docPath = docRef.path

            if (courseId.isNullOrBlank()) {
                val payload = mutableMapOf<String, Any>(
                    "title" to safeTitle,
                    "subtitle" to safeSubtitle,
                    "language" to language.trim().ifBlank { "HTML" }.take(30),
                    "category" to category.trim().ifBlank { "WEB" }.take(30),
                    "difficulty" to difficulty.trim().ifBlank { "Beginner" }.take(30),
                    "durationMinutes" to durationMinutes.coerceIn(1L, 240L),
                    "xpReward" to xpReward.coerceIn(10L, 2000L),
                    "isPremium" to isPremium,
                    "starterCode" to starterCode.take(5000),
                    "lessonSummary" to lessonSummary.take(1000),
                    "quizQuestion" to quizQuestion.take(300),
                    "quizOptions" to safeOptions,
                    "correctOptionIndex" to safeCorrectIdx,
                    "visibility" to safeVis,
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (safeUrl.isNotBlank()) payload["resourceUrl"] = safeUrl
                docRef.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "title" to safeTitle,
                    "subtitle" to safeSubtitle,
                    "language" to language.trim().ifBlank { "HTML" }.take(30),
                    "category" to category.trim().ifBlank { "WEB" }.take(30),
                    "difficulty" to difficulty.trim().ifBlank { "Beginner" }.take(30),
                    "durationMinutes" to durationMinutes.coerceIn(1L, 240L),
                    "xpReward" to xpReward.coerceIn(10L, 2000L),
                    "isPremium" to isPremium,
                    "resourceUrl" to safeUrl,
                    "starterCode" to starterCode.take(5000),
                    "lessonSummary" to lessonSummary.take(1000),
                    "quizQuestion" to quizQuestion.take(300),
                    "quizOptions" to safeOptions,
                    "correctOptionIndex" to safeCorrectIdx,
                    "visibility" to safeVis,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteManagedCourse(courseId: String): Result<Unit> {
        val docPath = "courses/$courseId"
        return try {
            requireUserId()
            require(courseId.isNotBlank()) { "Course ID cannot be blank" }
            db.collection("courses").document(courseId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    fun observeSnippets(userId: String): Flow<List<CodeSnippet>> = flow {
        val path = "users/$userId/snippets"
        emitAll(
            db.collection("users").document(userId).collection("snippets")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(CodeSnippet::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error -> throw unwrapAndLogFlowError(error, OperationType.LIST, path, onlyIfUid = userId) }
        )
    }

    fun observeCertificates(userId: String): Flow<List<Certificate>> = flow {
        val path = "users/$userId/certificates"
        emitAll(
            db.collection("users").document(userId).collection("certificates")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Certificate::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.earnedAt?.seconds ?: 0L }
                }
                .catch { error -> throw unwrapAndLogFlowError(error, OperationType.LIST, path, onlyIfUid = userId) }
        )
    }

    fun observeLeaderboard(): Flow<List<LeaderboardEntry>> = flow {
        val path = "leaderboard"
        emitAll(
            db.collection("leaderboard")
                .whereEqualTo("visibility", "public")
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(LeaderboardEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.xp }
                }
                .catch { error -> throw unwrapAndLogFlowError(error, OperationType.LIST, path) }
        )
    }

    fun observeCommunityProjects(): Flow<List<CommunityProject>> = flow {
        val path = "community_projects"
        emitAll(
            db.collection("community_projects")
                .whereEqualTo("visibility", "public")
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(CommunityProject::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> throw unwrapAndLogFlowError(error, OperationType.LIST, path) }
        )
    }

    fun observeCommunityQuestions(): Flow<List<CommunityQuestion>> = flow {
        val path = "community_qa"
        emitAll(
            db.collection("community_qa")
                .whereEqualTo("visibility", "public")
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(CommunityQuestion::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> throw unwrapAndLogFlowError(error, OperationType.LIST, path) }
        )
    }

    suspend fun createCommunityQuestion(
        authorName: String,
        title: String,
        body: String,
        language: String,
        codeSnippet: String
    ): Result<Unit> {
        var path = "community_qa"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("community_qa").document()
            path = docRef.path
            val safeTitle = title.trim().ifBlank { "Coding Question" }.take(120).let {
                if (it.length < 3) "$it ?" else it
            }
            val safeBody = body.trim().ifBlank { "Need help debugging." }.take(2000).let {
                if (it.length < 3) "$it ..." else it
            }
            docRef.set(
                mapOf(
                    "authorId" to uid,
                    "authorName" to authorName.trim().ifBlank { "Student" }.take(60),
                    "title" to safeTitle,
                    "body" to safeBody,
                    "language" to language.trim().ifBlank { "Python" }.take(30),
                    "codeSnippet" to codeSnippet.trim().take(5000),
                    "likesCount" to 0L,
                    "likedBy" to emptyList<String>(),
                    "replyCount" to 0L,
                    "visibility" to "public",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleLikeQuestion(question: CommunityQuestion): Result<Unit> {
        val path = "community_qa/${question.id}"
        return try {
            val uid = requireUserId()
            val updated = if (uid in question.likedBy) {
                question.likedBy.filterNot { it == uid }
            } else {
                (question.likedBy + uid).take(500)
            }
            db.collection("community_qa").document(question.id).update(
                mapOf(
                    "likedBy" to updated,
                    "likesCount" to updated.size.toLong(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeQuestionReplies(postId: String): Flow<List<QuestionReply>> = flow {
        val path = "community_qa/$postId/replies"
        emitAll(
            db.collection("community_qa").document(postId).collection("replies")
                .limit(50)
                .snapshots()
                .map { snap ->
                    snap.toObjects(QuestionReply::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> throw unwrapAndLogFlowError(error, OperationType.LIST, path) }
        )
    }

    suspend fun addQuestionReply(postId: String, authorName: String, body: String): Result<Unit> {
        val path = "community_qa/$postId/replies"
        return try {
            val uid = requireUserId()
            val cleanBody = body.trim().take(1000)
            require(cleanBody.isNotEmpty()) { "Reply cannot be empty" }
            val postRef = db.collection("community_qa").document(postId)
            postRef.collection("replies").document().set(
                mapOf(
                    "authorId" to uid,
                    "authorName" to authorName.trim().ifBlank { "Student" }.take(60),
                    "body" to cleanBody,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            postRef.update(
                mapOf(
                    "replyCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun submitModerationReport(targetType: String, targetId: String, reason: String): Result<Unit> {
        var path = "moderation_reports"
        return try {
            val uid = requireUserId()
            val safeReason = reason.trim().ifBlank { "Inappropriate content" }.take(400).let {
                if (it.length < 3) "$it ..." else it
            }
            val docRef = db.collection("moderation_reports").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "reporterId" to uid,
                    "targetType" to targetType.trim().take(30),
                    "targetId" to targetId.trim().take(128),
                    "reason" to safeReason,
                    "status" to "open",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeModerationReports(): Flow<List<ModerationReport>> = flow {
        val path = "moderation_reports"
        emitAll(
            db.collection("moderation_reports")
                .limit(50)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ModerationReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> throw unwrapAndLogFlowError(error, OperationType.LIST, path) }
        )
    }

    suspend fun resolveModerationReport(reportId: String, newStatus: String, hideTargetQuestionId: String? = null): Result<Unit> {
        val path = "moderation_reports/$reportId"
        return try {
            val uid = requireUserId()
            db.collection("moderation_reports").document(reportId).update(
                mapOf(
                    "status" to newStatus,
                    "resolvedBy" to uid
                )
            ).await()
            if (!hideTargetQuestionId.isNullOrBlank()) {
                db.collection("community_qa").document(hideTargetQuestionId).update(
                    mapOf(
                        "visibility" to "hidden",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observePlatformNotifications(): Flow<List<PlatformNotification>> = flow {
        val path = "platform_notifications"
        emitAll(
            db.collection("platform_notifications")
                .limit(30)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PlatformNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> throw unwrapAndLogFlowError(error, OperationType.LIST, path) }
        )
    }

    suspend fun sendPlatformNotification(title: String, message: String, targetTier: String = "all"): Result<Unit> {
        var path = "platform_notifications"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("platform_notifications").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "title" to title.trim().ifBlank { "TejasCode Update" }.take(100),
                    "message" to message.trim().take(500),
                    "targetTier" to targetTier,
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordLessonCompletion(
        lessonId: String,
        trackId: String,
        trackTitle: String,
        xpEarned: Int,
        durationMinutes: Int = 5,
        trackLessonIds: List<String>
    ): Result<Boolean> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            docPath = userRef.path
            val snap = userRef.get().await()
            val current = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: UserProfile(userId = uid, displayName = auth.currentUser?.displayName ?: "Student")

            val alreadyCompleted = lessonId in current.completedLessonIds
            val updatedLessonIds = if (alreadyCompleted) current.completedLessonIds
            else (current.completedLessonIds + lessonId).take(200)

            val today = todayDateString()
            val newStreak = if (current.lastActiveDate != today && current.lastActiveDate.isNotBlank()) {
                current.streakDays + 1L
            } else {
                maxOf(1L, current.streakDays)
            }

            val bonusXp = if (alreadyCompleted) 20L else xpEarned.toLong()
            val newXp = current.xp + bonusXp
            val newLessonsCount = updatedLessonIds.size.toLong()
            val newMinutes = current.totalCodingMinutes + durationMinutes.toLong().coerceAtLeast(1L)

            val newBadges = current.badges.toMutableSet()
            if (newLessonsCount >= 1L) newBadges.add("Code Starter")
            if (newLessonsCount >= 5L) newBadges.add("Track Explorer")
            if (newXp >= 500L) newBadges.add("500 XP Club")
            if (newStreak >= 3L) newBadges.add("3-Day Streak")

            if (snap.exists()) {
                userRef.update(
                    mapOf(
                        "xp" to newXp,
                        "streakDays" to newStreak,
                        "lessonsCompleted" to newLessonsCount,
                        "totalCodingMinutes" to newMinutes,
                        "badges" to newBadges.take(50),
                        "lastActiveDate" to today,
                        "completedLessonIds" to updatedLessonIds,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                userRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to current.displayName.take(60),
                        "learningGoal" to current.learningGoal.take(60),
                        "experienceLevel" to current.experienceLevel.take(30),
                        "registered" to current.registered,
                        "xp" to newXp,
                        "streakDays" to newStreak,
                        "lessonsCompleted" to newLessonsCount,
                        "totalCodingMinutes" to newMinutes,
                        "badges" to newBadges.take(50),
                        "battlesWon" to current.battlesWon,
                        "lastActiveDate" to today,
                        "completedLessonIds" to updatedLessonIds,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            syncLeaderboard(uid, current.displayName, newXp, newStreak, current.battlesWon)

            var unlockedCert = false
            if (trackLessonIds.isNotEmpty() && trackLessonIds.all { it in updatedLessonIds }) {
                val certId = "cert_$trackId"
                val certRef = userRef.collection("certificates").document(certId)
                if (!certRef.get().await().exists()) {
                    val code = "TC-${trackId.take(4).uppercase(Locale.US)}-${uid.take(6).uppercase(Locale.US)}"
                    certRef.set(
                        mapOf(
                            "userId" to uid,
                            "trackId" to trackId.take(64),
                            "trackTitle" to trackTitle.take(100),
                            "recipientName" to current.displayName.ifBlank { "TejasCode Student" }.take(60),
                            "verificationCode" to code.take(40),
                            "earnedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    unlockedCert = true
                }
            }
            Result.success(unlockedCert)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun recordBattleResult(won: Boolean, xpEarned: Int): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            docPath = userRef.path
            val snap = userRef.get().await()
            val current = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return Result.failure(IllegalStateException("Profile not initialized"))
            val newXp = current.xp + xpEarned.toLong().coerceAtLeast(0L)
            val newWins = if (won) current.battlesWon + 1L else current.battlesWon
            val today = todayDateString()
            val newBadges = current.badges.toMutableSet()
            if (newWins >= 1L) newBadges.add("Arena Victor")
            userRef.update(
                mapOf(
                    "xp" to newXp,
                    "battlesWon" to newWins,
                    "badges" to newBadges.take(50),
                    "lastActiveDate" to today,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            syncLeaderboard(uid, current.displayName, newXp, current.streakDays, newWins)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    private suspend fun syncLeaderboard(
        uid: String,
        displayName: String,
        xp: Long,
        streakDays: Long,
        battlesWon: Long
    ) {
        val lbRef = db.collection("leaderboard").document(uid)
        val safeName = displayName.trim().ifBlank { "Student" }.take(60)
        val snap = lbRef.get().await()
        if (snap.exists()) {
            lbRef.update(
                mapOf(
                    "displayName" to safeName,
                    "xp" to xp,
                    "streakDays" to streakDays,
                    "battlesWon" to battlesWon,
                    "visibility" to "public",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            lbRef.set(
                mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "xp" to xp,
                    "streakDays" to streakDays,
                    "battlesWon" to battlesWon,
                    "visibility" to "public",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun saveSnippet(
        snippetId: String?,
        title: String,
        language: String,
        code: String,
        lastOutput: String?
    ): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/snippets"
        return try {
            val uid = requireUserId()
            val safeTitle = title.trim().ifBlank { "Untitled Snippet" }.take(80)
            val safeLang = language.trim().ifBlank { "Python" }.take(30)
            val safeCode = code.take(20000)
            val safeOut = lastOutput?.take(5000)

            val snippetsCol = db.collection("users").document(uid).collection("snippets")
            val docRef = if (snippetId.isNullOrBlank()) snippetsCol.document() else snippetsCol.document(snippetId)
            docPath = docRef.path

            if (snippetId.isNullOrBlank()) {
                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "title" to safeTitle,
                    "language" to safeLang,
                    "code" to safeCode,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (safeOut != null) payload["lastOutput"] = safeOut
                docRef.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "title" to safeTitle,
                    "language" to safeLang,
                    "code" to safeCode,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (safeOut != null) updates["lastOutput"] = safeOut
                docRef.update(updates).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSnippet(snippetId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/snippets/$snippetId"
        return try {
            val uid = requireUserId()
            require(snippetId.isNotBlank()) { "Snippet ID cannot be blank" }
            val docRef = db.collection("users").document(uid).collection("snippets").document(snippetId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun publishCommunityProject(
        authorName: String,
        title: String,
        language: String,
        code: String,
        description: String
    ): Result<Unit> {
        var docPath = "community_projects"
        return try {
            val uid = requireUserId()
            val safeTitle = title.trim().ifBlank { "Shared Snippet" }.take(80)
            val safeCode = code.trim().ifBlank { "// empty" }.take(10000)
            val docRef = db.collection("community_projects").document()
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "authorId" to uid,
                    "authorName" to authorName.trim().ifBlank { "Student" }.take(60),
                    "title" to safeTitle,
                    "language" to language.trim().ifBlank { "Python" }.take(30),
                    "code" to safeCode,
                    "description" to description.trim().take(300),
                    "likesCount" to 0L,
                    "likedBy" to emptyList<String>(),
                    "visibility" to "public",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleStarProject(project: CommunityProject): Result<Unit> {
        val docPath = "community_projects/${project.id}"
        return try {
            val uid = requireUserId()
            require(project.id.isNotBlank()) { "Project ID cannot be blank" }
            val updatedLikedBy = if (uid in project.likedBy) {
                project.likedBy.filterNot { it == uid }
            } else {
                (project.likedBy + uid).take(500)
            }
            db.collection("community_projects").document(project.id).update(
                mapOf(
                    "likedBy" to updatedLikedBy,
                    "likesCount" to updatedLikedBy.size.toLong(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }
}
