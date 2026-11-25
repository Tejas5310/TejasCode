package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.billing.PaymentApiClient
import com.example.data.CodeForgeRepository
import com.example.data.CurriculumRepository
import com.example.model.CareerCategory
import com.example.model.CareerTrack
import com.example.model.Certificate
import com.example.model.CodingBattleQuestion
import com.example.model.CodingLesson
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
import com.example.model.UiState
import com.example.model.UserEntitlement
import com.example.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface BattlePhase {
    data object Lobby : BattlePhase
    data class InProgress(
        val questions: List<CodingBattleQuestion>,
        val currentIndex: Int,
        val secondsLeft: Int,
        val userScore: Int,
        val rivalScore: Int,
        val combo: Int,
        val selectedOption: Int? = null
    ) : BattlePhase

    data class Finished(
        val userScore: Int,
        val rivalScore: Int,
        val correctCount: Int,
        val totalCount: Int,
        val xpEarned: Int,
        val won: Boolean
    ) : BattlePhase
}

class AcademyViewModel(
    private val repository: CodeForgeRepository,
    private val currentUserId: String,
    initialDisplayName: String,
    userEmail: String?,
    initialPhotoUrl: String?,
    private val paymentApiClient: PaymentApiClient = PaymentApiClient()
) : ViewModel() {

    val isAdmin: Boolean = userEmail.equals(ADMIN_EMAIL, ignoreCase = true)

    private val _selectedCategory = MutableStateFlow(CareerCategory.ALL)
    val selectedCategory: StateFlow<CareerCategory> = _selectedCategory.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _unlockedCertTitle = MutableStateFlow<String?>(null)
    val unlockedCertTitle: StateFlow<String?> = _unlockedCertTitle.asStateFlow()

    private val _battlePhase = MutableStateFlow<BattlePhase>(BattlePhase.Lobby)
    val battlePhase: StateFlow<BattlePhase> = _battlePhase.asStateFlow()

    private val _billingBusy = MutableStateFlow(false)
    val billingBusy: StateFlow<Boolean> = _billingBusy.asStateFlow()

    private var timerJob: Job? = null
    private var battleCorrectCount = 0

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(initialDisplayName, initialPhotoUrl)
                .onFailure { err ->
                    Log.w(TAG, "Profile sync warning: ${err.message}")
                }
        }
    }

    val profileState: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error -> emit(unwrapFirestoreError("profile", error)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val planConfigState: StateFlow<UiState<PlanConfig>> = repository.observePlanConfig()
        .map<PlanConfig, UiState<PlanConfig>> { UiState.Success(it) }
        .catch { error -> emit(unwrapFirestoreError("plan_config", error)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val entitlementState: StateFlow<UiState<UserEntitlement>> = repository.observeUserEntitlement(currentUserId)
        .map<UserEntitlement, UiState<UserEntitlement>> { UiState.Success(it) }
        .catch { error -> emit(unwrapFirestoreError("entitlement", error)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val userSubscriptionsState: StateFlow<UiState<List<SubscriptionRecord>>> = repository.observeUserSubscriptions(currentUserId)
        .map<List<SubscriptionRecord>, UiState<List<SubscriptionRecord>>> { UiState.Success(it) }
        .catch { error -> emit(unwrapFirestoreError("user_subscriptions", error)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val userTransactionsState: StateFlow<UiState<List<PaymentTransaction>>> = repository.observeUserTransactions(currentUserId)
        .map<List<PaymentTransaction>, UiState<List<PaymentTransaction>>> { UiState.Success(it) }
        .catch { error -> emit(unwrapFirestoreError("user_transactions", error)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val publishedCoursesState: StateFlow<UiState<List<ManagedCourse>>> = repository.observePublishedCourses()
        .map<List<ManagedCourse>, UiState<List<ManagedCourse>>> { UiState.Success(it) }
        .catch { error -> emit(unwrapFirestoreError("courses", error)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val adminCoursesState: StateFlow<UiState<List<ManagedCourse>>> =
        if (isAdmin) {
            repository.observeAllCoursesForAdmin()
                .map<List<ManagedCourse>, UiState<List<ManagedCourse>>> { UiState.Success(it) }
                .catch { error -> emit(unwrapFirestoreError("admin_courses", error)) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)
        } else {
            flowOf<UiState<List<ManagedCourse>>>(UiState.Success(emptyList()))
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Success(emptyList()))
        }

    val adminSubscriptionsState: StateFlow<UiState<List<SubscriptionRecord>>> =
        if (isAdmin) {
            repository.observeAllSubscriptionsForAdmin()
                .map<List<SubscriptionRecord>, UiState<List<SubscriptionRecord>>> { UiState.Success(it) }
                .catch { error -> emit(unwrapFirestoreError("admin_subscriptions", error)) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)
        } else {
            flowOf<UiState<List<SubscriptionRecord>>>(UiState.Success(emptyList()))
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Success(emptyList()))
        }

    val adminTransactionsState: StateFlow<UiState<List<PaymentTransaction>>> =
        if (isAdmin) {
            repository.observeAllTransactionsForAdmin()
                .map<List<PaymentTransaction>, UiState<List<PaymentTransaction>>> { UiState.Success(it) }
                .catch { error -> emit(unwrapFirestoreError("admin_transactions", error)) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)
        } else {
            flowOf<UiState<List<PaymentTransaction>>>(UiState.Success(emptyList()))
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Success(emptyList()))
        }

    val adminSettlementsState: StateFlow<UiState<List<SettlementRecord>>> =
        if (isAdmin) {
            repository.observeSettlementsForAdmin()
                .map<List<SettlementRecord>, UiState<List<SettlementRecord>>> { UiState.Success(it) }
                .catch { error -> emit(unwrapFirestoreError("admin_settlements", error)) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)
        } else {
            flowOf<UiState<List<SettlementRecord>>>(UiState.Success(emptyList()))
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Success(emptyList()))
        }

    val moderationReportsState: StateFlow<UiState<List<ModerationReport>>> =
        if (isAdmin) {
            repository.observeModerationReports()
                .map<List<ModerationReport>, UiState<List<ModerationReport>>> { UiState.Success(it) }
                .catch { error -> emit(unwrapFirestoreError("moderation_reports", error)) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)
        } else {
            flowOf<UiState<List<ModerationReport>>>(UiState.Success(emptyList()))
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Success(emptyList()))
        }

    val notificationsState: StateFlow<UiState<List<PlatformNotification>>> = repository.observePlatformNotifications()
        .map<List<PlatformNotification>, UiState<List<PlatformNotification>>> { UiState.Success(it) }
        .catch { error -> emit(unwrapFirestoreError("notifications", error)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val certificatesState: StateFlow<UiState<List<Certificate>>> = repository.observeCertificates(currentUserId)
        .map<List<Certificate>, UiState<List<Certificate>>> { UiState.Success(it) }
        .catch { error -> emit(unwrapFirestoreError("certificates", error)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val leaderboardState: StateFlow<UiState<List<LeaderboardEntry>>> = repository.observeLeaderboard()
        .map<List<LeaderboardEntry>, UiState<List<LeaderboardEntry>>> { UiState.Success(it) }
        .catch { error -> emit(unwrapFirestoreError("leaderboard", error)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val communityState: StateFlow<UiState<List<CommunityProject>>> = repository.observeCommunityProjects()
        .map<List<CommunityProject>, UiState<List<CommunityProject>>> { UiState.Success(it) }
        .catch { error -> emit(unwrapFirestoreError("community", error)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val communityQuestionsState: StateFlow<UiState<List<CommunityQuestion>>> = repository.observeCommunityQuestions()
        .map<List<CommunityQuestion>, UiState<List<CommunityQuestion>>> { UiState.Success(it) }
        .catch { error -> emit(unwrapFirestoreError("community_qa", error)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private fun <T> unwrapFirestoreError(label: String, error: Throwable): UiState<T> {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        Log.w(TAG, "Error observing $label", root)
        return UiState.Error(root.message ?: "Failed to load $label.")
    }

    fun selectCategory(category: CareerCategory) {
        _selectedCategory.value = category
    }

    fun dismissBanner() {
        _statusBanner.value = null
    }

    fun dismissUnlockedCertificate() {
        _unlockedCertTitle.value = null
    }

    fun showBanner(message: String) {
        _statusBanner.value = message
    }

    fun completeStudentRegistration(
        displayName: String,
        learningGoal: String,
        experienceLevel: String
    ) {
        viewModelScope.launch {
            repository.completeStudentRegistration(displayName, learningGoal, experienceLevel)
                .onSuccess {
                    _statusBanner.value = "Welcome to TejasCode, $displayName! Profile saved."
                }
                .onFailure { err ->
                    _statusBanner.value = err.localizedMessage ?: "Failed to update registration."
                }
        }
    }

    fun updateExtendedProfile(bio: String, preferredLanguage: String) {
        viewModelScope.launch {
            repository.updateUserProfileExtended(bio, preferredLanguage)
                .onSuccess { _statusBanner.value = "Profile bio & language saved." }
                .onFailure { err -> _statusBanner.value = err.localizedMessage ?: "Failed to update profile." }
        }
    }

    fun toggleSaveCourse(courseId: String) {
        viewModelScope.launch {
            repository.toggleSaveCourse(courseId)
                .onSuccess { saved ->
                    _statusBanner.value = if (saved) "Saved course to bookmarks." else "Removed from saved courses."
                }
                .onFailure { err ->
                    _statusBanner.value = err.localizedMessage ?: "Could not update bookmark."
                }
        }
    }

    fun toggleFollowUser(targetUserId: String) {
        viewModelScope.launch {
            repository.toggleFollowUser(targetUserId)
                .onSuccess { nowFollowing ->
                    _statusBanner.value = if (nowFollowing) "Following developer." else "Unfollowed developer."
                }
        }
    }

    fun toggleBlockUser(targetUserId: String) {
        viewModelScope.launch {
            repository.toggleBlockUser(targetUserId)
                .onSuccess { blocked ->
                    _statusBanner.value = if (blocked) "User blocked. Their posts are now hidden." else "User unblocked."
                }
        }
    }

    fun savePlanConfig(config: PlanConfig) {
        viewModelScope.launch {
            repository.savePlanConfig(config)
                .onSuccess { _statusBanner.value = "Pricing & Plan Configuration updated live!" }
                .onFailure { err -> _statusBanner.value = err.localizedMessage ?: "Failed to save plan config." }
        }
    }

    fun cancelUserSubscription(subscriptionId: String, reason: String?) {
        viewModelScope.launch {
            _billingBusy.value = true
            paymentApiClient.cancelSubscription(subscriptionId, reason)
                .onSuccess { msg -> _statusBanner.value = msg }
                .onFailure { err -> _statusBanner.value = err.localizedMessage ?: "Cancellation failed." }
            _billingBusy.value = false
        }
    }

    fun reconcileSettlements() {
        viewModelScope.launch {
            _billingBusy.value = true
            paymentApiClient.reconcileSettlements()
                .onSuccess { msg -> _statusBanner.value = msg }
                .onFailure { err -> _statusBanner.value = err.localizedMessage ?: "Settlement sync failed." }
            _billingBusy.value = false
        }
    }

    fun deleteMyAccount(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _billingBusy.value = true
            paymentApiClient.deleteAccount()
                .onSuccess { msg ->
                    _statusBanner.value = msg
                    onDeleted()
                }
                .onFailure { err ->
                    _statusBanner.value = err.localizedMessage ?: "Account deletion requires backend URL configuration."
                }
            _billingBusy.value = false
        }
    }

    fun saveAdminCourse(
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
    ) {
        viewModelScope.launch {
            repository.saveManagedCourse(
                courseId = courseId,
                title = title,
                subtitle = subtitle,
                language = language,
                category = category,
                difficulty = difficulty,
                durationMinutes = durationMinutes,
                xpReward = xpReward,
                starterCode = starterCode,
                lessonSummary = lessonSummary,
                quizQuestion = quizQuestion,
                quizOptions = quizOptions,
                correctOptionIndex = correctOptionIndex,
                visibility = visibility,
                isPremium = isPremium,
                resourceUrl = resourceUrl
            ).onSuccess {
                _statusBanner.value = if (courseId == null) "Course published to TejasCode!" else "Course updated."
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Failed to save course."
            }
        }
    }

    fun deleteAdminCourse(courseId: String) {
        viewModelScope.launch {
            repository.deleteManagedCourse(courseId)
                .onSuccess { _statusBanner.value = "Course deleted." }
                .onFailure { err -> _statusBanner.value = err.localizedMessage ?: "Failed to delete course." }
        }
    }

    fun completeLesson(lesson: CodingLesson, track: CareerTrack) {
        viewModelScope.launch {
            val result = repository.recordLessonCompletion(
                lessonId = lesson.id,
                trackId = track.id,
                trackTitle = track.title,
                xpEarned = lesson.xpReward,
                durationMinutes = lesson.durationMinutes,
                trackLessonIds = track.lessons.map { it.id }
            )
            result.onSuccess { unlockedCert ->
                if (unlockedCert) {
                    _unlockedCertTitle.value = track.title
                } else {
                    _statusBanner.value = "+${lesson.xpReward} XP earned for ${lesson.title}!"
                }
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Could not save lesson progress."
            }
        }
    }

    fun toggleStarProject(project: CommunityProject) {
        viewModelScope.launch {
            repository.toggleStarProject(project).onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Failed to update project star."
            }
        }
    }

    fun createCommunityQuestion(
        authorName: String,
        title: String,
        body: String,
        language: String,
        codeSnippet: String
    ) {
        viewModelScope.launch {
            repository.createCommunityQuestion(authorName, title, body, language, codeSnippet)
                .onSuccess { _statusBanner.value = "Question posted to TejasCode Q&A!" }
                .onFailure { err -> _statusBanner.value = err.localizedMessage ?: "Failed to post question." }
        }
    }

    fun toggleLikeQuestion(question: CommunityQuestion) {
        viewModelScope.launch {
            repository.toggleLikeQuestion(question).onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Failed to update upvote."
            }
        }
    }

    fun observeReplies(postId: String): Flow<List<QuestionReply>> = repository.observeQuestionReplies(postId)

    fun addQuestionReply(postId: String, authorName: String, body: String) {
        viewModelScope.launch {
            repository.addQuestionReply(postId, authorName, body)
                .onSuccess { _statusBanner.value = "Reply posted." }
                .onFailure { err -> _statusBanner.value = err.localizedMessage ?: "Failed to post reply." }
        }
    }

    fun reportContent(targetType: String, targetId: String, reason: String) {
        viewModelScope.launch {
            repository.submitModerationReport(targetType, targetId, reason)
                .onSuccess { _statusBanner.value = "Report submitted to moderation team." }
                .onFailure { err -> _statusBanner.value = err.localizedMessage ?: "Failed to submit report." }
        }
    }

    fun resolveReport(reportId: String, status: String, hidePostId: String? = null) {
        viewModelScope.launch {
            repository.resolveModerationReport(reportId, status, hidePostId)
                .onSuccess { _statusBanner.value = "Moderation report marked $status." }
                .onFailure { err -> _statusBanner.value = err.localizedMessage ?: "Failed to resolve report." }
        }
    }

    fun sendAnnouncement(title: String, message: String, targetTier: String = "all") {
        viewModelScope.launch {
            repository.sendPlatformNotification(title, message, targetTier)
                .onSuccess { _statusBanner.value = "Platform announcement published!" }
                .onFailure { err -> _statusBanner.value = err.localizedMessage ?: "Failed to publish announcement." }
        }
    }

    fun startBattle() {
        timerJob?.cancel()
        battleCorrectCount = 0
        val questions = CurriculumRepository.battleQuestions.shuffled()
        _battlePhase.value = BattlePhase.InProgress(
            questions = questions,
            currentIndex = 0,
            secondsLeft = 18,
            userScore = 0,
            rivalScore = 0,
            combo = 0,
            selectedOption = null
        )
        launchQuestionTimer()
    }

    private fun launchQuestionTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val phase = _battlePhase.value as? BattlePhase.InProgress ?: break
                if (phase.selectedOption != null) break
                val nextSec = phase.secondsLeft - 1
                if (nextSec <= 0) {
                    _battlePhase.value = phase.copy(
                        secondsLeft = 0,
                        selectedOption = -1,
                        combo = 0,
                        rivalScore = phase.rivalScore + 95
                    )
                    break
                } else {
                    _battlePhase.value = phase.copy(secondsLeft = nextSec)
                }
            }
        }
    }

    fun submitBattleAnswer(optionIndex: Int) {
        val phase = _battlePhase.value as? BattlePhase.InProgress ?: return
        if (phase.selectedOption != null) return
        timerJob?.cancel()

        val q = phase.questions[phase.currentIndex]
        val isCorrect = optionIndex == q.correctIndex
        val newCombo = if (isCorrect) phase.combo + 1 else 0
        if (isCorrect) battleCorrectCount++

        val gained = if (isCorrect) 100 + (phase.secondsLeft * 5) + (newCombo * 15) else 0
        val rivalGain = if ((phase.currentIndex + phase.secondsLeft) % 3 != 0) 95 else 40

        _battlePhase.value = phase.copy(
            selectedOption = optionIndex,
            userScore = phase.userScore + gained,
            rivalScore = phase.rivalScore + rivalGain,
            combo = newCombo
        )
    }

    fun nextBattleQuestion() {
        val phase = _battlePhase.value as? BattlePhase.InProgress ?: return
        val nextIdx = phase.currentIndex + 1
        if (nextIdx >= phase.questions.size) {
            timerJob?.cancel()
            val won = phase.userScore >= phase.rivalScore
            val xp = (phase.userScore / 5).coerceAtLeast(30) + if (won) 60 else 0
            _battlePhase.value = BattlePhase.Finished(
                userScore = phase.userScore,
                rivalScore = phase.rivalScore,
                correctCount = battleCorrectCount,
                totalCount = phase.questions.size,
                xpEarned = xp,
                won = won
            )
            viewModelScope.launch {
                repository.recordBattleResult(won = won, xpEarned = xp)
            }
        } else {
            _battlePhase.value = phase.copy(
                currentIndex = nextIdx,
                secondsLeft = 18,
                selectedOption = null
            )
            launchQuestionTimer()
        }
    }

    fun resetBattle() {
        timerJob?.cancel()
        _battlePhase.value = BattlePhase.Lobby
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    companion object {
        private const val TAG = "AcademyVM"
        const val ADMIN_EMAIL = "tejashpatel02010@gmail.com"
    }
}
