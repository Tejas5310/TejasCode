package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CareerTrack
import com.example.model.CodingLesson
import com.example.ui.components.SyntaxCodeCard
import com.example.ui.theme.CoralRose
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.MintEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonPlayerScreen(
    track: CareerTrack,
    lesson: CodingLesson,
    onBack: () -> Unit,
    onAskTutor: (String) -> Unit,
    onLessonCompleted: () -> Unit
) {
    BackHandler { onBack() }

    var stepIndex by remember(lesson.id) { mutableIntStateOf(0) }
    var selectedOption by remember(lesson.id, stepIndex) { mutableStateOf<Int?>(null) }

    val currentStep = lesson.steps[stepIndex]
    val progress by animateFloatAsState(
        targetValue = (stepIndex + 1).toFloat() / lesson.steps.size,
        animationSpec = spring(stiffness = 320f),
        label = "lessonStepProgress"
    )

    val hasQuestion = !currentStep.question.isNullOrBlank()
    val isAnsweredCorrectly = !hasQuestion || selectedOption == currentStep.correctOptionIndex
    val isLastStep = stepIndex == lesson.steps.lastIndex

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = lesson.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        Text(
                            text = "${track.title} • Step ${stepIndex + 1}/${lesson.steps.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("lesson_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to tracks")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(ElectricCyan)
                    )
                }

                Spacer(Modifier.height(18.dp))

                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        (slideInHorizontally { it / 4 } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it / 4 } + fadeOut())
                    },
                    label = "stepTransition"
                ) { step ->
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = step.explanation,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (!step.codeSnippet.isNullOrBlank()) {
                            SyntaxCodeCard(
                                code = step.codeSnippet,
                                languageLabel = step.languageLabel
                            )
                        }

                        if (lesson.resourceUrl.startsWith("http")) {
                            val ctx = androidx.compose.ui.platform.LocalContext.current
                            OutlinedButton(
                                onClick = {
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse(lesson.resourceUrl)
                                    )
                                    runCatching { ctx.startActivity(intent) }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Open Companion Video / Docs Resource")
                            }
                        }

                        if (!step.question.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Quick Check",
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricCyan
                            )
                            Text(
                                text = step.question,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            step.options.forEachIndexed { idx, option ->
                                val isPicked = selectedOption == idx
                                val isCorrect = idx == step.correctOptionIndex
                                val borderColor = when {
                                    isPicked && isCorrect -> MintEmerald
                                    isPicked && !isCorrect -> CoralRose
                                    else -> MaterialTheme.colorScheme.outline
                                }
                                val bgColor = when {
                                    isPicked && isCorrect -> MintEmerald.copy(alpha = 0.14f)
                                    isPicked && !isCorrect -> CoralRose.copy(alpha = 0.14f)
                                    else -> MaterialTheme.colorScheme.surface
                                }

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("quiz_option_$idx")
                                        .clickable { selectedOption = idx },
                                    shape = MaterialTheme.shapes.medium,
                                    color = bgColor,
                                    border = BorderStroke(1.5.dp, borderColor)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = option,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isPicked) {
                                            Icon(
                                                imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                                                contentDescription = null,
                                                tint = if (isCorrect) MintEmerald else CoralRose
                                            )
                                        }
                                    }
                                }
                            }

                            AnimatedVisibility(visible = selectedOption != null) {
                                val correct = selectedOption == step.correctOptionIndex
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (correct) MintEmerald.copy(alpha = 0.14f) else CoralRose.copy(alpha = 0.14f)
                                    ),
                                    border = BorderStroke(1.dp, if (correct) MintEmerald else CoralRose)
                                ) {
                                    Text(
                                        text = if (correct) step.feedbackCorrect else step.feedbackHint,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val prompt = buildString {
                            append("Explain this concept from '${lesson.title}' (${currentStep.title}):\n")
                            append(currentStep.explanation)
                            if (!currentStep.codeSnippet.isNullOrBlank()) {
                                append("\n\nCode:\n```${currentStep.languageLabel}\n${currentStep.codeSnippet}\n```")
                            }
                        }
                        onAskTutor(prompt)
                    },
                    modifier = Modifier.weight(1f).height(50.dp).testTag("lesson_ask_ai_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("AI Hint")
                }

                Button(
                    onClick = {
                        if (isLastStep) {
                            onLessonCompleted()
                            onBack()
                        } else {
                            stepIndex++
                        }
                    },
                    enabled = isAnsweredCorrectly,
                    modifier = Modifier.weight(1.4f).height(50.dp).testTag("lesson_next_button")
                ) {
                    Text(
                        text = if (isLastStep) "Complete (+${lesson.xpReward} XP)" else "Continue",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
