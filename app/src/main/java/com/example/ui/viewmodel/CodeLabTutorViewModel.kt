package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiTutorRepository
import com.example.data.CodeForgeRepository
import com.example.engine.ExecutionResult
import com.example.engine.LocalCodeRunner
import com.example.model.CodeSnippet
import com.example.model.CodingChallenge
import com.example.model.PlanConfig
import com.example.model.ProgrammingLanguage
import com.example.model.UiState
import com.example.model.UserEntitlement
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class TutorMessage(
    val id: String = UUID.randomUUID().toString(),
    val isFromUser: Boolean,
    val text: String,
    val isStreaming: Boolean = false
)

data class AiQuotaSummary(
    val usedToday: Long = 0L,
    val dailyLimit: Long = 5L,
    val freeLimit: Long = 5L,
    val proLimit: Long = 100L,
    val isPro: Boolean = false
)

class CodeLabTutorViewModel(
    private val repository: CodeForgeRepository,
    private val aiRepository: AiTutorRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _editorLanguage = MutableStateFlow(ProgrammingLanguage.PYTHON)
    val editorLanguage: StateFlow<ProgrammingLanguage> = _editorLanguage.asStateFlow()

    private val _editorTitle = MutableStateFlow("My Python Sandbox")
    val editorTitle: StateFlow<String> = _editorTitle.asStateFlow()

    private val _editorCode = MutableStateFlow(ProgrammingLanguage.PYTHON.starterCode)
    val editorCode: StateFlow<String> = _editorCode.asStateFlow()

    private val _activeSnippetId = MutableStateFlow<String?>(null)
    val activeSnippetId: StateFlow<String?> = _activeSnippetId.asStateFlow()

    private val _executionResult = MutableStateFlow<ExecutionResult?>(null)
    val executionResult: StateFlow<ExecutionResult?> = _executionResult.asStateFlow()

    private val _aiSandboxState = MutableStateFlow<UiState<String>?>(null)
    val aiSandboxState: StateFlow<UiState<String>?> = _aiSandboxState.asStateFlow()

    private val _labNotice = MutableStateFlow<String?>(null)
    val labNotice: StateFlow<String?> = _labNotice.asStateFlow()

    private val _chatMessages = MutableStateFlow(
        listOf(
            TutorMessage(
                isFromUser = false,
                text = "Welcome to TejasCode AI Tutor! Ask me to debug any of our 15 programming languages, explain Big-O complexity, or prep for coding interviews."
            )
        )
    )
    val chatMessages: StateFlow<List<TutorMessage>> = _chatMessages.asStateFlow()

    private val _tutorStatus = MutableStateFlow<UiState<Unit>>(UiState.Success(Unit))
    val tutorStatus: StateFlow<UiState<Unit>> = _tutorStatus.asStateFlow()

    private val entitlementFlow = repository.observeUserEntitlement(currentUserId)
        .catch { emit(UserEntitlement(userId = currentUserId)) }

    private val planConfigFlow = repository.observePlanConfig()
        .catch { emit(PlanConfig()) }

    private val profileFlow = repository.observeUserProfile(currentUserId)
        .catch { emit(null) }

    val aiQuotaSummary: StateFlow<AiQuotaSummary> = combine(
        profileFlow,
        entitlementFlow,
        planConfigFlow
    ) { profile, entitlement, planConfig ->
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val isPro = entitlement.isProActive
        val freeLim = planConfig.freeDailyAiLimit.coerceAtLeast(1L)
        val proLim = planConfig.proDailyAiLimit.coerceAtLeast(1L)
        val limit = if (isPro) proLim else freeLim
        val used = if (profile?.aiQuotaDate == today) profile.aiQueriesUsedToday else 0L
        AiQuotaSummary(usedToday = used, dailyLimit = limit, freeLimit = freeLim, proLimit = proLim, isPro = isPro)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AiQuotaSummary())

    val snippetsState: StateFlow<UiState<List<CodeSnippet>>> = repository.observeSnippets(currentUserId)
        .map<List<CodeSnippet>, UiState<List<CodeSnippet>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing snippets", root)
            emit(UiState.Error(root.message ?: "Failed to load saved snippets."))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    fun selectLanguage(language: ProgrammingLanguage) {
        _editorLanguage.value = language
        _activeSnippetId.value = null
        _editorTitle.value = "${language.displayName} Scratchpad"
        _editorCode.value = language.starterCode
        _executionResult.value = null
        _aiSandboxState.value = null
    }

    fun updateCode(code: String) {
        _editorCode.value = code
    }

    fun updateTitle(title: String) {
        _editorTitle.value = title
    }

    fun insertToken(token: String) {
        _editorCode.value = _editorCode.value + token
    }

    fun loadSampleProgram() {
        _editorCode.value = _editorLanguage.value.starterCode
        _executionResult.value = null
        _aiSandboxState.value = null
        _labNotice.value = "Loaded ${_editorLanguage.value.displayName} starter sample."
    }

    fun dismissLabNotice() {
        _labNotice.value = null
    }

    fun runLocalCode() {
        val result = LocalCodeRunner.run(_editorLanguage.value, _editorCode.value)
        _executionResult.value = result
    }

    fun runAiSandbox() {
        val lang = _editorLanguage.value.displayName
        val code = _editorCode.value
        if (code.isBlank()) {
            _aiSandboxState.value = UiState.Error("Editor is empty. Write some code first.")
            return
        }
        viewModelScope.launch {
            _aiSandboxState.value = UiState.Loading
            val currentSummary = aiQuotaSummary.value
            val quotaResult = repository.consumeAiQuota(
                isProActive = currentSummary.isPro,
                freeLimit = currentSummary.freeLimit,
                proLimit = currentSummary.proLimit
            )
            quotaResult.onFailure { err ->
                _aiSandboxState.value = UiState.Error(err.localizedMessage ?: "Daily AI quota exceeded.")
                return@launch
            }
            try {
                val output = withTimeout(60_000L) {
                    aiRepository.evaluateCodeInSandbox(lang, code)
                }
                _aiSandboxState.value = UiState.Success(output)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI Sandbox timed out", e)
                _aiSandboxState.value = UiState.Error("AI Sandbox timed out after 60s. Please retry.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Sandbox failed", e)
                _aiSandboxState.value = UiState.Error(e.localizedMessage ?: "AI Sandbox failed.")
            }
        }
    }

    fun saveSnippet() {
        viewModelScope.launch {
            val result = repository.saveSnippet(
                snippetId = _activeSnippetId.value,
                title = _editorTitle.value,
                language = _editorLanguage.value.displayName,
                code = _editorCode.value,
                lastOutput = _executionResult.value?.output
            )
            result.onSuccess { id ->
                _activeSnippetId.value = id
                _labNotice.value = "Snippet saved to cloud workspace."
            }.onFailure { err ->
                _labNotice.value = err.localizedMessage ?: "Failed to save snippet."
            }
        }
    }

    fun publishToCommunity(authorName: String, description: String) {
        viewModelScope.launch {
            repository.publishCommunityProject(
                authorName = authorName,
                title = _editorTitle.value,
                language = _editorLanguage.value.displayName,
                code = _editorCode.value,
                description = description
            ).onSuccess {
                _labNotice.value = "Published to Community Showcase!"
            }.onFailure { err ->
                _labNotice.value = err.localizedMessage ?: "Failed to share project."
            }
        }
    }

    fun loadSnippet(snippet: CodeSnippet) {
        val lang = ProgrammingLanguage.entries.firstOrNull {
            it.displayName.equals(snippet.language, ignoreCase = true)
        } ?: ProgrammingLanguage.PYTHON
        _editorLanguage.value = lang
        _editorTitle.value = snippet.title
        _editorCode.value = snippet.code
        _activeSnippetId.value = snippet.id
        _executionResult.value = snippet.lastOutput?.let {
            ExecutionResult(output = it, isError = false, durationMs = 1L, engineLabel = "Saved Output")
        }
        _aiSandboxState.value = null
    }

    fun loadChallenge(challenge: CodingChallenge) {
        _editorLanguage.value = challenge.language
        _editorTitle.value = challenge.title
        _editorCode.value = challenge.starterCode
        _activeSnippetId.value = null
        _executionResult.value = null
        _aiSandboxState.value = null
    }

    fun deleteSnippet(snippetId: String) {
        viewModelScope.launch {
            repository.deleteSnippet(snippetId).onSuccess {
                if (_activeSnippetId.value == snippetId) {
                    _activeSnippetId.value = null
                }
                _labNotice.value = "Snippet deleted."
            }.onFailure { err ->
                _labNotice.value = err.localizedMessage ?: "Failed to delete snippet."
            }
        }
    }

    fun sendTutorMessage(userPrompt: String) {
        val trimmed = userPrompt.trim()
        if (trimmed.isBlank()) return

        val recentContext = _chatMessages.value
            .takeLast(6)
            .joinToString("\n") { msg ->
                val role = if (msg.isFromUser) "Learner" else "Tutor"
                "$role: ${msg.text}"
            }

        val userMsg = TutorMessage(isFromUser = true, text = trimmed)
        val assistantMsgId = UUID.randomUUID().toString()
        val placeholderMsg = TutorMessage(
            id = assistantMsgId,
            isFromUser = false,
            text = "",
            isStreaming = true
        )

        _chatMessages.value = _chatMessages.value + userMsg + placeholderMsg
        _tutorStatus.value = UiState.Loading

        viewModelScope.launch {
            val currentSummary = aiQuotaSummary.value
            val quotaResult = repository.consumeAiQuota(
                isProActive = currentSummary.isPro,
                freeLimit = currentSummary.freeLimit,
                proLimit = currentSummary.proLimit
            )
            quotaResult.onFailure { err ->
                _chatMessages.value = _chatMessages.value.filterNot { it.id == assistantMsgId }
                _tutorStatus.value = UiState.Error(err.localizedMessage ?: "Daily AI limit reached.")
                return@launch
            }

            val buffer = StringBuilder()
            try {
                withTimeout(60_000L) {
                    aiRepository.streamTutorResponse(trimmed, recentContext).collect { chunk ->
                        buffer.append(chunk)
                        val partial = buffer.toString()
                        _chatMessages.value = _chatMessages.value.map {
                            if (it.id == assistantMsgId) it.copy(text = partial, isStreaming = true) else it
                        }
                    }
                }
                val finalText = buffer.toString().trim()
                if (finalText.isBlank()) {
                    _chatMessages.value = _chatMessages.value.filterNot { it.id == assistantMsgId }
                    _tutorStatus.value = UiState.Error("AI Tutor returned an empty response.")
                } else {
                    _chatMessages.value = _chatMessages.value.map {
                        if (it.id == assistantMsgId) it.copy(text = finalText, isStreaming = false) else it
                    }
                    _tutorStatus.value = UiState.Success(Unit)
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI Tutor timed out", e)
                _chatMessages.value = _chatMessages.value.filterNot { it.id == assistantMsgId && it.text.isBlank() }
                    .map { if (it.id == assistantMsgId) it.copy(isStreaming = false) else it }
                _tutorStatus.value = UiState.Error("AI Tutor timed out after 60s. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Tutor failed", e)
                _chatMessages.value = _chatMessages.value.filterNot { it.id == assistantMsgId && it.text.isBlank() }
                    .map { if (it.id == assistantMsgId) it.copy(isStreaming = false) else it }
                _tutorStatus.value = UiState.Error(e.localizedMessage ?: "AI Tutor request failed.")
            }
        }
    }

    fun prepareDebugPromptFromLab(): String {
        val lang = _editorLanguage.value.displayName
        val code = _editorCode.value
        val output = _executionResult.value?.output.orEmpty()
        return buildString {
            append("Please review and help me improve or debug this $lang code:\n\n```$lang\n$code\n```")
            if (output.isNotBlank()) {
                append("\n\nLatest output:\n$output")
            }
        }
    }

    fun clearTutorError() {
        _tutorStatus.value = UiState.Success(Unit)
    }

    private companion object {
        const val TAG = "CodeLabTutorVM"
    }
}
