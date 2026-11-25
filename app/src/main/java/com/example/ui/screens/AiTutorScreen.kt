package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.UiState
import com.example.ui.components.SyntaxCodeCard
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonGreen
import com.example.ui.viewmodel.AiQuotaSummary
import com.example.ui.viewmodel.TutorMessage

@Composable
fun AiTutorScreen(
    messages: List<TutorMessage>,
    tutorStatus: UiState<Unit>,
    quotaSummary: AiQuotaSummary,
    onSendMessage: (String) -> Unit,
    onReviewLabCode: () -> Unit,
    onOpenPremium: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val isLoading = tutorStatus is UiState.Loading

    LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ElectricViolet.copy(alpha = 0.16f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Tutor",
                        tint = ElectricViolet,
                        modifier = Modifier.padding(8.dp).size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AI Code Tutor",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Quota: ${quotaSummary.usedToday}/${quotaSummary.dailyLimit} today (${if (quotaSummary.isPro) "PRO" else "FREE"})",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (quotaSummary.isPro) NeonGreen else ElectricCyan
                    )
                }
                if (!quotaSummary.isPro) {
                    TextButton(onClick = onOpenPremium, modifier = Modifier.testTag("tutor_upgrade_pro_button")) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(16.dp))
                        Text("Pro", color = NeonAmber)
                    }
                }
            }
        }

        if (tutorStatus is UiState.Error) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                onClick = onDismissError
            ) {
                Text(
                    text = "${tutorStatus.message} (Tap to dismiss)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                TutorChatBubble(message = message)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = { if (!isLoading) onReviewLabCode() },
                label = { Text("Debug my Code Lab") },
                modifier = Modifier.testTag("chip_debug_lab")
            )
            listOf(
                "Explain Binary Search Big-O",
                "Give me a Python interview question",
                "Compare Kotlin Coroutines vs Threads"
            ).forEach { prompt ->
                AssistChip(
                    onClick = { if (!isLoading) onSendMessage(prompt) },
                    label = { Text(prompt) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Ask about any bug, algorithm, or syntax...") },
                modifier = Modifier.weight(1f).testTag("tutor_input_field"),
                maxLines = 3
            )
            IconButton(
                onClick = {
                    val msg = input
                    input = ""
                    onSendMessage(msg)
                },
                enabled = !isLoading && input.isNotBlank(),
                modifier = Modifier.size(50.dp).testTag("tutor_send_button"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send prompt")
                }
            }
        }
    }
}

@Composable
private fun TutorChatBubble(message: TutorMessage) {
    val align = if (message.isFromUser) Alignment.End else Alignment.Start
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = if (message.isFromUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            border = BorderStroke(
                1.dp,
                if (message.isFromUser) ElectricCyan.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val segments = remember(message.text) { parseCodeSegments(message.text) }
                if (segments.isEmpty() && message.isStreaming) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Thinking...", style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    segments.forEach { seg ->
                        if (seg.isCode) {
                            SyntaxCodeCard(code = seg.content, languageLabel = seg.language)
                        } else if (seg.content.isNotBlank()) {
                            Text(
                                text = seg.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (message.isFromUser) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
                if (message.isStreaming && message.text.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Streaming...",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricViolet
                    )
                }
            }
        }
    }
}

private data class MessageSegment(val isCode: Boolean, val language: String, val content: String)

private fun parseCodeSegments(raw: String): List<MessageSegment> {
    if (raw.isBlank()) return emptyList()
    val regex = Regex("""```([a-zA-Z0-9+#_-]*)\n([\s\S]*?)```""")
    val out = mutableListOf<MessageSegment>()
    var lastEnd = 0
    for (match in regex.findAll(raw)) {
        if (match.range.first > lastEnd) {
            out.add(MessageSegment(false, "", raw.substring(lastEnd, match.range.first).trim()))
        }
        out.add(
            MessageSegment(
                isCode = true,
                language = match.groupValues[1].ifBlank { "Code" },
                content = match.groupValues[2].trimEnd()
            )
        )
        lastEnd = match.range.last + 1
    }
    if (lastEnd < raw.length) {
        out.add(MessageSegment(false, "", raw.substring(lastEnd).trim()))
    }
    return out
}
