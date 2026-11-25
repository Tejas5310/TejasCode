package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ExecutionResult
import com.example.model.CodeSnippet
import com.example.model.ProgrammingLanguage
import com.example.model.UiState
import com.example.ui.theme.CoralRose
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.viewmodel.AiQuotaSummary

@Composable
fun CodeLabScreen(
    selectedLanguage: ProgrammingLanguage,
    title: String,
    code: String,
    executionResult: ExecutionResult?,
    aiSandboxState: UiState<String>?,
    snippetsState: UiState<List<CodeSnippet>>,
    labNotice: String?,
    authorName: String,
    quotaSummary: AiQuotaSummary,
    onSelectLanguage: (ProgrammingLanguage) -> Unit,
    onTitleChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onInsertToken: (String) -> Unit,
    onLoadSample: () -> Unit,
    onRunLocal: () -> Unit,
    onRunAiSandbox: () -> Unit,
    onSaveSnippet: () -> Unit,
    onLoadSnippet: (CodeSnippet) -> Unit,
    onDeleteSnippet: (String) -> Unit,
    onPublishToCommunity: (String, String) -> Unit,
    onAskTutorAboutCode: () -> Unit,
    onOpenPremium: () -> Unit,
    onDismissNotice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    var showShareDialog by remember { mutableStateOf(false) }
    var shareDescription by remember { mutableStateOf("") }

    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("Share to Community") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Publish \"$title\" (${selectedLanguage.displayName}) for fellow learners to inspect and star.")
                    OutlinedTextField(
                        value = shareDescription,
                        onValueChange = { if (it.length <= 300) shareDescription = it },
                        label = { Text("Short description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onPublishToCommunity(authorName, shareDescription)
                    showShareDialog = false
                    shareDescription = ""
                }) {
                    Text("Publish")
                }
            },
            dismissButton = {
                TextButton(onClick = { showShareDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Mobile Code Lab",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "15 Languages • Editing ${selectedLanguage.fileExt}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ElectricCyan
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = onLoadSample, modifier = Modifier.testTag("load_sample_button")) {
                    Icon(Icons.Default.Restore, contentDescription = "Reset to sample program", tint = NeonAmber)
                }
                IconButton(
                    onClick = { clipboard.setText(AnnotatedString(code)) },
                    modifier = Modifier.testTag("copy_code_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy code", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onSaveSnippet, modifier = Modifier.testTag("save_snippet_button")) {
                    Icon(Icons.Default.Save, contentDescription = "Save snippet", tint = MintEmerald)
                }
                IconButton(onClick = { showShareDialog = true }, modifier = Modifier.testTag("share_project_button")) {
                    Icon(Icons.Default.CloudUpload, contentDescription = "Share to community", tint = ElectricCyan)
                }
            }
        }

        // Daily AI Sandbox Quota Banner
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth().clickable { if (!quotaSummary.isPro) onOpenPremium() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI Compiles Today: ${quotaSummary.usedToday} / ${quotaSummary.dailyLimit} (${if (quotaSummary.isPro) "PRO" else "FREE"})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!quotaSummary.isPro) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(16.dp))
                        Text("Upgrade", style = MaterialTheme.typography.labelMedium, color = NeonAmber, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        AnimatedVisibility(visible = labNotice != null) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MintEmerald.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, MintEmerald.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().clickable { onDismissNotice() }
            ) {
                Text(
                    text = labNotice.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MintEmerald,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProgrammingLanguage.entries.forEach { lang ->
                FilterChip(
                    selected = lang == selectedLanguage,
                    onClick = { onSelectLanguage(lang) },
                    label = { Text(lang.displayName) },
                    modifier = Modifier.testTag("lang_chip_${lang.name}")
                )
            }
        }

        OutlinedTextField(
            value = title,
            onValueChange = { if (it.length <= 80) onTitleChange(it) },
            label = { Text("Project / Snippet Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("snippet_title_input")
        )

        // Editor Surface
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = ObsidianBg,
            border = BorderStroke(1.dp, ObsidianOutline)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ObsidianSurfaceElevated)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("    ", "()", "{}", "[]", " = ", "->", "\": \"", ";").forEach { sym ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.clickable { onInsertToken(sym) }
                        ) {
                            Text(
                                text = if (sym == "    ") "TAB" else sym.trim(),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.sp,
                                color = ElectricCyan,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    val lineCount = code.lines().size.coerceAtLeast(1)
                    Column(modifier = Modifier.padding(end = 10.dp)) {
                        for (i in 1..lineCount) {
                            Text(
                                text = "$i",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = Color(0xFF475569)
                            )
                        }
                    }

                    BasicTextField(
                        value = code,
                        onValueChange = onCodeChange,
                        textStyle = TextStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = Color(0xFFF1F5F9)
                        ),
                        cursorBrush = SolidColor(ElectricCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .testTag("code_editor_input")
                    )
                }
            }
        }

        // Execution buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onRunLocal,
                modifier = Modifier.weight(1f).height(48.dp).testTag("run_code_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MintEmerald, contentColor = Color(0xFF042219))
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Run", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onRunAiSandbox,
                modifier = Modifier.weight(1.2f).height(48.dp).testTag("ai_sandbox_button"),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet, contentColor = Color(0xFF1A0E3A))
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("AI Compile", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onAskTutorAboutCode,
                modifier = Modifier.weight(1f).height(48.dp).testTag("ask_tutor_from_lab_button")
            ) {
                Text("Debug")
            }
        }

        if (executionResult != null) {
            TerminalOutputCard(
                title = "${executionResult.engineLabel} • ${executionResult.durationMs}ms",
                output = executionResult.output,
                isError = executionResult.isError
            )
        }

        when (aiSandboxState) {
            is UiState.Loading -> {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = ObsidianBg,
                    border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ElectricViolet, strokeWidth = 2.dp)
                        Text("Running in Gemini AI Sandbox...", style = MaterialTheme.typography.bodySmall, color = ElectricViolet)
                    }
                }
            }
            is UiState.Success -> {
                TerminalOutputCard(
                    title = "AI Sandbox Output & Insights",
                    output = aiSandboxState.data,
                    isError = false
                )
            }
            is UiState.Error -> {
                TerminalOutputCard(
                    title = "AI Sandbox Error",
                    output = aiSandboxState.message,
                    isError = true
                )
            }
            null -> Unit
        }

        Text(
            text = "Saved Cloud Snippets",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        when (snippetsState) {
            is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.size(22.dp))
            is UiState.Error -> Text(snippetsState.message, color = CoralRose, style = MaterialTheme.typography.bodySmall)
            is UiState.Success -> {
                if (snippetsState.data.isEmpty()) {
                    Text(
                        text = "No saved snippets yet. Tap the save icon above to store code in your cloud workspace.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(snippetsState.data, key = { it.id }) { snippet ->
                            Card(
                                modifier = Modifier
                                    .width(210.dp)
                                    .clickable { onLoadSnippet(snippet) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(snippet.title, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                                        Text(snippet.language, style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                                    }
                                    IconButton(onClick = { onDeleteSnippet(snippet.id) }) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Delete snippet",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TerminalOutputCard(
    title: String,
    output: String,
    isError: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = ObsidianBg,
        border = BorderStroke(1.dp, if (isError) CoralRose.copy(alpha = 0.6f) else MintEmerald.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = if (isError) CoralRose else MintEmerald,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isError) CoralRose else MintEmerald
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = output,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = if (isError) CoralRose else Color(0xFFE2E8F0)
            )
        }
    }
}
