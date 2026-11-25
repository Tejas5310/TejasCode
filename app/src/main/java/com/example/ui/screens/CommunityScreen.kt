package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CurriculumRepository
import com.example.model.CommunityProject
import com.example.model.CommunityQuestion
import com.example.model.QuestionReply
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.ui.components.SyntaxCodeCard
import com.example.ui.theme.CoralRose
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import kotlinx.coroutines.flow.Flow

import androidx.compose.material.icons.automirrored.filled.ArrowBack

@Composable
fun CommunityScreen(
    currentUserId: String,
    profile: UserProfile?,
    questionsState: UiState<List<CommunityQuestion>>,
    projectsState: UiState<List<CommunityProject>>,
    observeReplies: (postId: String) -> Flow<List<QuestionReply>>,
    onCreateQuestion: (authorName: String, title: String, body: String, language: String, codeSnippet: String) -> Unit,
    onToggleLikeQuestion: (CommunityQuestion) -> Unit,
    onAddReply: (postId: String, authorName: String, body: String) -> Unit,
    onToggleStarProject: (CommunityProject) -> Unit,
    onToggleFollow: (targetUid: String) -> Unit,
    onToggleBlock: (targetUid: String) -> Unit,
    onReportContent: (targetType: String, targetId: String, reason: String) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showAskDialog by remember { mutableStateOf(false) }
    var activeQuestion by remember { mutableStateOf<CommunityQuestion?>(null) }
    var reportTarget by remember { mutableStateOf<Pair<String, String>?>(null) }

    val blockedIds = profile?.blockedUserIds?.toSet().orEmpty()
    val followingIds = profile?.followingIds?.toSet().orEmpty()
    val authorName = profile?.displayName?.ifBlank { "Student" } ?: "Student"

    if (showAskDialog) {
        AskQuestionDialog(
            onDismiss = { showAskDialog = false },
            onSubmit = { title, body, lang, snippet ->
                onCreateQuestion(authorName, title, body, lang, snippet)
                showAskDialog = false
            }
        )
    }

    activeQuestion?.let { q ->
        val replies by observeReplies(q.id).collectAsStateWithLifecycle(initialValue = emptyList())
        QuestionThreadDialog(
            question = q,
            replies = replies,
            onDismiss = { activeQuestion = null },
            onSendReply = { body -> onAddReply(q.id, authorName, body) }
        )
    }

    reportTarget?.let { (tType, tId) ->
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { reportTarget = null },
            title = { Text("Report Content") },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { if (it.length <= 400) reason = it },
                    label = { Text("Reason (spam, harassment, unsafe code)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReportContent(tType, tId, reason.ifBlank { "Inappropriate content" })
                        reportTarget = null
                    }
                ) { Text("Submit Report") }
            },
            dismissButton = {
                TextButton(onClick = { reportTarget = null }) { Text("Cancel") }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (onBack != null) {
                            IconButton(onClick = onBack, modifier = Modifier.testTag("community_back_button")) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                        Icon(Icons.Default.Forum, contentDescription = null, tint = NeonBlue)
                        Column {
                            Text("Developer Community", style = MaterialTheme.typography.titleLarge)
                            Text("Q&A Discussions & Code Showcase", style = MaterialTheme.typography.bodySmall, color = NeonBlue)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search discussions or languages...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("community_search_input")
                    )
                }
            }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Q&A Forum") },
                    modifier = Modifier.testTag("community_tab_qa")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Code Showcase") },
                    modifier = Modifier.testTag("community_tab_showcase")
                )
            }

            if (selectedTab == 0) {
                when (questionsState) {
                    is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    is UiState.Error -> Box(Modifier.fillMaxSize().padding(16.dp)) { Text(questionsState.message, color = CoralRose) }
                    is UiState.Success -> {
                        val filtered = questionsState.data.filter { q ->
                            q.authorId !in blockedIds && (
                                searchQuery.isBlank() ||
                                    q.title.contains(searchQuery, true) ||
                                    q.language.contains(searchQuery, true) ||
                                    q.body.contains(searchQuery, true)
                                )
                        }
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 84.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (filtered.isEmpty()) {
                                item {
                                    Text(
                                        text = "No discussions yet. Tap + to ask the community a coding question!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                items(filtered, key = { it.id }) { q ->
                                    val liked = currentUserId in q.likedBy
                                    val isFollowing = q.authorId in followingIds
                                    var menuOpen by remember { mutableStateOf(false) }

                                    Card(
                                        shape = MaterialTheme.shapes.large,
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        onClick = { activeQuestion = q }
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(q.title, style = MaterialTheme.typography.titleMedium)
                                                    Text(
                                                        text = "by ${q.authorName} • ${q.language}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = NeonBlue
                                                    )
                                                }
                                                if (q.authorId != currentUserId) {
                                                    Box {
                                                        IconButton(onClick = { menuOpen = true }) {
                                                            Icon(Icons.Default.MoreVert, contentDescription = "Post options")
                                                        }
                                                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                                            DropdownMenuItem(
                                                                text = { Text(if (isFollowing) "Unfollow ${q.authorName}" else "Follow ${q.authorName}") },
                                                                leadingIcon = {
                                                                    Icon(if (isFollowing) Icons.Default.PersonRemove else Icons.Default.PersonAdd, contentDescription = null)
                                                                },
                                                                onClick = {
                                                                    menuOpen = false
                                                                    onToggleFollow(q.authorId)
                                                                }
                                                            )
                                                            DropdownMenuItem(
                                                                text = { Text("Report Question") },
                                                                leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                                                                onClick = {
                                                                    menuOpen = false
                                                                    reportTarget = "community_qa" to q.id
                                                                }
                                                            )
                                                            DropdownMenuItem(
                                                                text = { Text("Block User", color = CoralRose) },
                                                                onClick = {
                                                                    menuOpen = false
                                                                    onToggleBlock(q.authorId)
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            Text(q.body, style = MaterialTheme.typography.bodySmall, maxLines = 4)
                                            if (q.codeSnippet.isNotBlank()) {
                                                SyntaxCodeCard(code = q.codeSnippet, languageLabel = q.language)
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TextButton(onClick = { onToggleLikeQuestion(q) }) {
                                                    Icon(
                                                        imageVector = if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                        contentDescription = "Upvote question",
                                                        tint = if (liked) CoralRose else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("${q.likesCount}")
                                                }
                                                TextButton(onClick = { activeQuestion = q }) {
                                                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = "View replies", modifier = Modifier.size(18.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("${q.replyCount} Replies")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                when (projectsState) {
                    is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    is UiState.Error -> Box(Modifier.fillMaxSize().padding(16.dp)) { Text(projectsState.message, color = CoralRose) }
                    is UiState.Success -> {
                        val filtered = projectsState.data.filter { p ->
                            p.authorId !in blockedIds && (
                                searchQuery.isBlank() ||
                                    p.title.contains(searchQuery, true) ||
                                    p.language.contains(searchQuery, true)
                                )
                        }
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filtered, key = { it.id }) { project ->
                                val starred = currentUserId in project.likedBy
                                Card(
                                    shape = MaterialTheme.shapes.large,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(project.title, style = MaterialTheme.typography.titleMedium)
                                                Text("by ${project.authorName} • ${project.language}", style = MaterialTheme.typography.bodySmall, color = NeonGreen)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(onClick = { onToggleStarProject(project) }) {
                                                    Icon(
                                                        imageVector = if (starred) Icons.Default.Star else Icons.Default.StarBorder,
                                                        contentDescription = "Star project",
                                                        tint = if (starred) NeonAmber else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Text("${project.likesCount}")
                                                if (project.authorId != currentUserId) {
                                                    IconButton(onClick = { reportTarget = "community_projects" to project.id }) {
                                                        Icon(Icons.Default.Flag, contentDescription = "Report project", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            }
                                        }
                                        if (project.description.isNotBlank()) {
                                            Text(project.description, style = MaterialTheme.typography.bodySmall)
                                        }
                                        SyntaxCodeCard(code = project.code, languageLabel = project.language)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedTab == 0) {
            FloatingActionButton(
                onClick = { showAskDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(18.dp)
                    .testTag("ask_question_fab"),
                containerColor = NeonBlue,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ask Question")
            }
        }
    }
}

@Composable
private fun AskQuestionDialog(
    onDismiss: () -> Unit,
    onSubmit: (title: String, body: String, language: String, snippet: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("Python") }
    var snippet by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ask TejasCode Community") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 120) title = it },
                    label = { Text("Question Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("ask_question_title")
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CurriculumRepository.allSupportedLanguages.forEach { lang ->
                        FilterChip(
                            selected = language == lang,
                            onClick = { language = lang },
                            label = { Text(lang) }
                        )
                    }
                }
                OutlinedTextField(
                    value = body,
                    onValueChange = { if (it.length <= 2000) body = it },
                    label = { Text("Describe the bug or concept") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag("ask_question_body")
                )
                OutlinedTextField(
                    value = snippet,
                    onValueChange = { if (it.length <= 5000) snippet = it },
                    label = { Text("Optional Code Snippet") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(title, body, language, snippet) },
                enabled = title.trim().length >= 3 && body.trim().length >= 3,
                modifier = Modifier.testTag("submit_question_button")
            ) {
                Text("Post Question")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun QuestionThreadDialog(
    question: CommunityQuestion,
    replies: List<QuestionReply>,
    onDismiss: () -> Unit,
    onSendReply: (String) -> Unit
) {
    var replyText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(question.title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("by ${question.authorName} • ${question.language}", style = MaterialTheme.typography.labelMedium, color = NeonBlue)
                Text(question.body, style = MaterialTheme.typography.bodySmall)
                if (question.codeSnippet.isNotBlank()) {
                    SyntaxCodeCard(code = question.codeSnippet, languageLabel = question.language)
                }
                Text("Replies (${replies.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (replies.isEmpty()) {
                    Text("No replies yet. Be the first to help!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    replies.forEach { reply ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(reply.authorName, style = MaterialTheme.typography.labelMedium, color = NeonPurple)
                                Text(reply.body, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { if (it.length <= 1000) replyText = it },
                    label = { Text("Write a helpful reply") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (replyText.isNotBlank()) {
                        onSendReply(replyText)
                        replyText = ""
                    }
                },
                enabled = replyText.isNotBlank()
            ) {
                Text("Reply")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
