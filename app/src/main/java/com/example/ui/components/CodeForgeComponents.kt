package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.WarmAmber

private val KEYWORDS = setOf(
    "fun", "val", "var", "def", "class", "return", "if", "elif", "else", "for", "while",
    "in", "import", "from", "const", "let", "async", "await", "function", "SELECT", "FROM",
    "WHERE", "GROUP", "BY", "HAVING", "ORDER", "DESC", "ASC", "fn", "use", "int", "using",
    "namespace", "package", "main", "true", "false", "null", "None", "print", "println"
)

@Composable
fun rememberSyntaxHighlightedCode(code: String): AnnotatedString {
    return remember(code) {
        buildAnnotatedString {
            append(code)
            // Highlight strings
            Regex("""("[^"\n]*"|'[^'\n]*')""").findAll(code).forEach { match ->
                addStyle(SpanStyle(color = MintEmerald), match.range.first, match.range.last + 1)
            }
            // Highlight numbers
            Regex("""\b\d+(\.\d+)?\b""").findAll(code).forEach { match ->
                addStyle(SpanStyle(color = WarmAmber), match.range.first, match.range.last + 1)
            }
            // Highlight keywords
            Regex("""\b[A-Za-z_]\w*\b""").findAll(code).forEach { match ->
                if (match.value in KEYWORDS) {
                    addStyle(
                        SpanStyle(color = ElectricViolet, fontWeight = FontWeight.SemiBold),
                        match.range.first,
                        match.range.last + 1
                    )
                }
            }
            // Highlight comments
            Regex("""(//[^\n]*|#[^\n]*|--[^\n]*)""").findAll(code).forEach { match ->
                addStyle(SpanStyle(color = Color(0xFF64748B)), match.range.first, match.range.last + 1)
            }
        }
    }
}

@Composable
fun SyntaxCodeCard(
    code: String,
    languageLabel: String,
    modifier: Modifier = Modifier
) {
    val highlighted = rememberSyntaxHighlightedCode(code)
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = ObsidianBg,
        border = BorderStroke(1.dp, ObsidianOutline)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurfaceElevated)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFB7185)))
                    Box(Modifier.size(8.dp).clip(CircleShape).background(WarmAmber))
                    Box(Modifier.size(8.dp).clip(CircleShape).background(MintEmerald))
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }
                if (languageLabel.isNotBlank()) {
                    Text(
                        text = languageLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(12.dp)
            ) {
                val lines = code.lines()
                Column(modifier = Modifier.padding(end = 12.dp)) {
                    lines.indices.forEach { idx ->
                        Text(
                            text = "${idx + 1}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            lineHeight = 19.sp,
                            color = Color(0xFF475569)
                        )
                    }
                }
                Text(
                    text = highlighted,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = Color(0xFFE2E8F0)
                )
            }
        }
    }
}

@Composable
fun AnimatedStreakXpBar(
    streakDays: Long,
    xp: Long,
    lessonsCompleted: Long,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "streakPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flameScale"
    )
    val animatedLevelProgress by animateFloatAsState(
        targetValue = ((xp % 500L) / 500f).coerceIn(0.05f, 1f),
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 280f),
        label = "xpBar"
    )
    val level = (xp / 500L) + 1L

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = WarmAmber.copy(alpha = 0.16f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Daily streak",
                            tint = WarmAmber,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(22.dp)
                                .scale(pulseScale)
                        )
                    }
                    Column {
                        Text(
                            text = "$streakDays-Day Streak",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$lessonsCompleted modules completed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = ElectricCyan.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "$xp XP • Lv.$level",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedLevelProgress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(ElectricCyan, MintEmerald)
                            )
                        )
                )
            }
        }
    }
}

@Composable
fun AnimatedDifficultyPill(label: String, accent: Color) {
    val bg by animateColorAsState(accent.copy(alpha = 0.15f), label = "pillBg")
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = bg,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
