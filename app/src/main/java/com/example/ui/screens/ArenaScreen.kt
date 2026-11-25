package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CurriculumRepository
import com.example.model.CodingChallenge
import com.example.model.LeaderboardEntry
import com.example.model.UiState
import com.example.ui.components.AnimatedDifficultyPill
import com.example.ui.components.SyntaxCodeCard
import com.example.ui.theme.CoralRose
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.WarmAmber
import com.example.ui.viewmodel.BattlePhase

@Composable
fun ArenaScreen(
    battlePhase: BattlePhase,
    leaderboardState: UiState<List<LeaderboardEntry>>,
    currentUserId: String,
    onStartBattle: () -> Unit,
    onSubmitBattleAnswer: (Int) -> Unit,
    onNextBattleQuestion: () -> Unit,
    onResetBattle: () -> Unit,
    onOpenChallengeInLab: (CodingChallenge) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Battle", "Challenges", "Leaderboard")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Compete & Practice",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { idx, label ->
                SegmentedButton(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    shape = SegmentedButtonDefaults.itemShape(index = idx, count = tabs.size),
                    modifier = Modifier.testTag("arena_tab_$idx")
                ) {
                    Text(label)
                }
            }
        }

        AnimatedContent(targetState = selectedTab, label = "arenaTab") { tab ->
            when (tab) {
                0 -> BattleTab(
                    phase = battlePhase,
                    onStartBattle = onStartBattle,
                    onSubmitAnswer = onSubmitBattleAnswer,
                    onNextQuestion = onNextBattleQuestion,
                    onReset = onResetBattle
                )
                1 -> ChallengesTab(onOpenChallengeInLab = onOpenChallengeInLab)
                else -> LeaderboardTab(
                    leaderboardState = leaderboardState,
                    currentUserId = currentUserId
                )
            }
        }
    }
}

@Composable
private fun BattleTab(
    phase: BattlePhase,
    onStartBattle: () -> Unit,
    onSubmitAnswer: (Int) -> Unit,
    onNextQuestion: () -> Unit,
    onReset: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        when (phase) {
            is BattlePhase.Lobby -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = ElectricViolet.copy(alpha = 0.2f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = null,
                                tint = ElectricViolet,
                                modifier = Modifier.padding(12.dp).size(30.dp)
                            )
                        }
                        Text(
                            text = "Head-to-Head Code Duel",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Race through 5 rapid-fire coding questions across Python, JS, Kotlin, SQL, and Big-O. Answer faster for time bonuses and combo multipliers!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onStartBattle,
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("start_battle_button")
                        ) {
                            Text("Start 5-Round Battle", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            is BattlePhase.InProgress -> {
                val q = phase.questions[phase.currentIndex]
                val totalScore = (phase.userScore + phase.rivalScore).coerceAtLeast(1)
                val userRatio by animateFloatAsState(
                    targetValue = (phase.userScore.toFloat() / totalScore).coerceIn(0.1f, 0.9f),
                    animationSpec = spring(),
                    label = "battleRace"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "You: ${phase.userScore}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MintEmerald
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (phase.secondsLeft <= 5) CoralRose.copy(alpha = 0.2f) else ElectricCyan.copy(alpha = 0.16f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                tint = if (phase.secondsLeft <= 5) CoralRose else ElectricCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${phase.secondsLeft}s",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (phase.secondsLeft <= 5) CoralRose else ElectricCyan
                            )
                        }
                    }
                    Text(
                        text = "Rival: ${phase.rivalScore}",
                        style = MaterialTheme.typography.titleMedium,
                        color = CoralRose
                    )
                }

                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(CoralRose.copy(alpha = 0.4f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(userRatio)
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(MintEmerald)
                    )
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AnimatedDifficultyPill("${q.language} • Round ${phase.currentIndex + 1}/${phase.questions.size}", ElectricCyan)
                    if (phase.combo > 1) {
                        AnimatedDifficultyPill("${phase.combo}x Combo!", WarmAmber)
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(q.prompt, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                SyntaxCodeCard(code = q.codeSnippet, languageLabel = q.language)
                Spacer(Modifier.height(12.dp))

                q.options.forEachIndexed { idx, opt ->
                    val picked = phase.selectedOption
                    val isSelected = picked == idx
                    val isCorrect = idx == q.correctIndex
                    val border = when {
                        picked != null && isCorrect -> MintEmerald
                        picked != null && isSelected -> CoralRose
                        else -> MaterialTheme.colorScheme.outline
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("battle_option_$idx")
                            .clickable(enabled = picked == null) { onSubmitAnswer(idx) },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.5.dp, border)
                    ) {
                        Text(
                            text = opt,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                if (phase.selectedOption != null) {
                    Spacer(Modifier.height(10.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = q.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = onNextQuestion,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("battle_next_button")
                    ) {
                        Text(if (phase.currentIndex == phase.questions.lastIndex) "See Results" else "Next Round")
                    }
                }
            }

            is BattlePhase.Finished -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, if (phase.won) MintEmerald else WarmAmber)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = if (phase.won) WarmAmber else ElectricCyan,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (phase.won) "Victory!" else "Close Match!",
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (phase.won) MintEmerald else WarmAmber
                        )
                        Text(
                            text = "You ${phase.userScore} — ${phase.rivalScore} Rival",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${phase.correctCount}/${phase.totalCount} correct • +${phase.xpEarned} XP synced",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ElectricCyan
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) {
                                Text("Lobby")
                            }
                            Button(onClick = onStartBattle, modifier = Modifier.weight(1f)) {
                                Text("Rematch")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChallengesTab(
    onOpenChallengeInLab: (CodingChallenge) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(CurriculumRepository.challenges, key = { _, ch -> ch.id }) { _, ch ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(ch.title, style = MaterialTheme.typography.titleLarge)
                        AnimatedDifficultyPill(ch.difficulty, WarmAmber)
                    }
                    Text(ch.prompt, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${ch.language.displayName} • +${ch.xpReward} XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan
                    )
                    Button(
                        onClick = { onOpenChallengeInLab(ch) },
                        modifier = Modifier.fillMaxWidth().testTag("solve_challenge_${ch.id}")
                    ) {
                        Text("Solve in Code Lab")
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardTab(
    leaderboardState: UiState<List<LeaderboardEntry>>,
    currentUserId: String
) {
    when (leaderboardState) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is UiState.Error -> Text(leaderboardState.message, color = CoralRose)
        is UiState.Success -> {
            if (leaderboardState.data.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Leaderboard, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("No rankings yet. Complete a lesson or battle to claim #1!")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(leaderboardState.data, key = { _, entry -> entry.userId }) { idx, entry ->
                        val isMe = entry.userId == currentUserId
                        val rankColor = when (idx) {
                            0 -> WarmAmber
                            1 -> Color(0xFFCBD5E1)
                            2 -> Color(0xFFD97706)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = if (isMe) ElectricCyan.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (isMe) ElectricCyan else MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "#${idx + 1}",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = rankColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Column {
                                        Text(
                                            text = if (isMe) "${entry.displayName} (You)" else entry.displayName,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "${entry.streakDays}d streak • ${entry.battlesWon} battle wins",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(2.dp))
                                    Text(
                                        text = "${entry.xp} XP",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = ElectricCyan
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
