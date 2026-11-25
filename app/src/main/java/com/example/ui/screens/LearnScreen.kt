package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CurriculumRepository
import com.example.model.CareerCategory
import com.example.model.CareerTrack
import com.example.model.CodingLesson
import com.example.model.ManagedCourse
import com.example.model.PlanConfig
import com.example.model.PlatformNotification
import com.example.model.UiState
import com.example.model.UserEntitlement
import com.example.model.UserProfile
import com.example.ui.components.AnimatedDifficultyPill
import com.example.ui.components.AnimatedStreakXpBar
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.VividViolet
import com.example.util.LocalAppLabels
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen(
    profileState: UiState<UserProfile?>,
    entitlementState: UiState<UserEntitlement>,
    planConfigState: UiState<PlanConfig>,
    publishedCoursesState: UiState<List<ManagedCourse>>,
    notificationsState: UiState<List<PlatformNotification>>,
    selectedCategory: CareerCategory,
    unlockedCertTitle: String?,
    isAdmin: Boolean,
    onSelectCategory: (CareerCategory) -> Unit,
    onOpenLesson: (CareerTrack, CodingLesson) -> Unit,
    onToggleSaveCourse: (String) -> Unit,
    onNavigateToCodeLab: () -> Unit,
    onNavigateToQuizzes: () -> Unit,
    onNavigateToCertificates: () -> Unit,
    onOpenCommunity: () -> Unit,
    onOpenPremium: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAdmin: () -> Unit,
    onCompleteRegistration: (displayName: String, learningGoal: String, experienceLevel: String) -> Unit,
    onDismissCertDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val labels = LocalAppLabels.current
    val profile = (profileState as? UiState.Success)?.data
    val entitlement = (entitlementState as? UiState.Success)?.data ?: UserEntitlement()
    val planConfig = (planConfigState as? UiState.Success)?.data ?: PlanConfig()
    val notifications = (notificationsState as? UiState.Success)?.data.orEmpty()

    val completedIds = profile?.completedLessonIds?.toSet().orEmpty()
    val savedIds = profile?.savedCourseIds?.toSet().orEmpty()
    val isProActive = entitlement.isProActive

    val publishedCourses = (publishedCoursesState as? UiState.Success)?.data.orEmpty()
    val liveTrack = remember(publishedCourses) { CurriculumRepository.managedCoursesAsTrack(publishedCourses) }
    val allTracks = remember(liveTrack) {
        if (liveTrack != null) listOf(liveTrack) + CurriculumRepository.tracks else CurriculumRepository.tracks
    }

    var savedOnlyFilter by remember { mutableStateOf(false) }

    val filteredTracks = allTracks.filter { track ->
        val matchesCat = selectedCategory == CareerCategory.ALL ||
            track.category == selectedCategory ||
            track.id == "tejascode_live_catalog"
        val matchesSaved = !savedOnlyFilter || track.id in savedIds
        matchesCat && matchesSaved
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var showMenuSheet by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showRegistrationDialog by remember { mutableStateOf(false) }

    val needsOnboardingRegistration = profile != null && !profile.registered
    if (needsOnboardingRegistration || showRegistrationDialog) {
        StudentRegistrationDialog(
            initialName = profile?.displayName ?: "",
            initialGoal = profile?.learningGoal ?: "HTML, CSS & JavaScript",
            initialLevel = profile?.experienceLevel ?: "Beginner",
            dismissible = !needsOnboardingRegistration,
            onDismiss = { showRegistrationDialog = false },
            onSubmit = { name, goal, level ->
                onCompleteRegistration(name, goal, level)
                showRegistrationDialog = false
            }
        )
    }

    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = { Text("TejasCode Activity & Alerts") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Daily Streak: ${profile?.streakDays ?: 1} day(s) active")
                    Text("Current XP: ${profile?.xp ?: 0} XP • Tier: ${if (isProActive) "TejasCode Pro" else "Free"}")
                    Text("Modules Completed: ${profile?.lessonsCompleted ?: 0}")
                    if (notifications.isNotEmpty()) {
                        Text("Announcements:", fontWeight = FontWeight.Bold, color = NeonBlue)
                        notifications.take(5).forEach { alert ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(alert.title, style = MaterialTheme.typography.titleSmall)
                                    Text(alert.message, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showNotificationsDialog = false }) { Text("Got it") }
            }
        )
    }

    if (showMenuSheet) {
        ModalBottomSheet(onDismissRequest = { showMenuSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("TejasCode Menu", style = MaterialTheme.typography.titleLarge)
                MenuSheetItem(
                    icon = Icons.Default.WorkspacePremium,
                    title = if (isProActive) "TejasCode Pro Active" else labels.upgradePro,
                    subtitle = if (isProActive) "Manage subscription & invoices" else "Unlock all 15 languages & Pro tracks",
                    accent = NeonAmber,
                    testTag = "menu_premium"
                ) {
                    showMenuSheet = false
                    onOpenPremium()
                }
                MenuSheetItem(
                    icon = Icons.Default.PersonAdd,
                    title = "Student Registration & Goal",
                    subtitle = "${profile?.learningGoal ?: "Full-Stack Web"} • ${profile?.experienceLevel ?: "Beginner"}",
                    accent = NeonBlue,
                    testTag = "menu_student_registration"
                ) {
                    showMenuSheet = false
                    showRegistrationDialog = true
                }
                MenuSheetItem(
                    icon = Icons.Default.Forum,
                    title = "Developer Community Q&A",
                    subtitle = "Ask questions & star shared code snippets",
                    accent = NeonGreen,
                    testTag = "menu_community"
                ) {
                    showMenuSheet = false
                    onOpenCommunity()
                }
                MenuSheetItem(
                    icon = Icons.Default.Settings,
                    title = labels.settingsTitle,
                    subtitle = "Dark/Light Theme, 10 Languages & Privacy",
                    accent = NeonBlue,
                    testTag = "menu_settings"
                ) {
                    showMenuSheet = false
                    onOpenSettings()
                }
                MenuSheetItem(
                    icon = Icons.Default.AdminPanelSettings,
                    title = "Admin Control Console",
                    subtitle = if (isAdmin) "Courses, Pricing, Settlements & Moderation" else "Instructor & admin console",
                    accent = NeonPurple,
                    testTag = "menu_admin_dashboard"
                ) {
                    showMenuSheet = false
                    onOpenAdmin()
                }
                MenuSheetItem(
                    icon = Icons.Default.Code,
                    title = "Online Code Editor",
                    subtitle = "Run code across all 15 languages",
                    accent = NeonGreen,
                    testTag = "menu_code_editor"
                ) {
                    showMenuSheet = false
                    onNavigateToCodeLab()
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (unlockedCertTitle != null) {
        AlertDialog(
            onDismissRequest = onDismissCertDialog,
            icon = {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = "Certificate Unlocked",
                    tint = NeonAmber,
                    modifier = Modifier.size(44.dp)
                )
            },
            title = { Text("Verified Certificate Earned!") },
            text = {
                Text(
                    "Congratulations! You completed all modules in $unlockedCertTitle. View, download PDF, and share your verified QR credential in Profile."
                )
            },
            confirmButton = {
                Button(onClick = onDismissCertDialog) {
                    Text("Awesome!")
                }
            }
        )
    }

    val firstName = profile?.displayName?.trim()?.substringBefore(" ")?.ifBlank { "Student" } ?: "Student"

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top App Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { showMenuSheet = true },
                        modifier = Modifier.size(48.dp).testTag("dashboard_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Menu",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Image(
                        painter = painterResource(id = R.drawable.ic_tejascode_logo),
                        contentDescription = "TejasCode Logo",
                        modifier = Modifier.size(34.dp),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)) {
                                append("Tejas")
                            }
                            withStyle(
                                SpanStyle(
                                    brush = Brush.horizontalGradient(listOf(NeonBlue, NeonPurple)),
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append("Code")
                            }
                        },
                        fontSize = 22.sp,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onOpenCommunity,
                        modifier = Modifier.size(48.dp).testTag("dashboard_community_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forum,
                            contentDescription = "Community Q&A",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(
                        onClick = { showNotificationsDialog = true },
                        modifier = Modifier.size(48.dp).testTag("dashboard_notifications_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Image(
                        painter = painterResource(id = R.drawable.img_tejas_coder),
                        contentDescription = "Student Profile",
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .clickable { onNavigateToCertificates() }
                            .testTag("dashboard_avatar_button"),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        // Welcome Hero Banner
        item {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = Color.Transparent,
                border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.28f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(Color(0xFF0B1438), Color(0xFF171345))))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = labels.welcomeBack,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = "$firstName!",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = labels.keepCoding,
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeonBlue
                        )
                    }
                    Image(
                        painter = painterResource(id = R.drawable.img_tejas_coder),
                        contentDescription = "TejasCode Student Mascot",
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        // Pro Status / Upgrade Banner
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("banner_upgrade_pro")
                    .clickable(onClick = onOpenPremium),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, (if (isProActive) NeonGreen else NeonAmber).copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = "TejasCode Pro",
                            tint = if (isProActive) NeonGreen else NeonAmber,
                            modifier = Modifier.size(26.dp)
                        )
                        Column {
                            Text(
                                text = if (isProActive) "TejasCode Pro Active" else labels.upgradePro,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isProActive) {
                                    "All 15 languages & Pro tracks unlocked"
                                } else if (planConfig.isConfigured) {
                                    "From ${planConfig.formattedMonthlyPrice()}/mo or ${planConfig.formattedYearlyPrice()}/yr"
                                } else {
                                    "Unlock Advanced Tracks, 100 Daily AI Compiles & Certificates"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }
        }

        // Continue Learning Card
        item {
            val nextTrack = allTracks.firstOrNull { track ->
                track.lessons.any { it.id !in completedIds }
            } ?: allTracks.first()
            val nextLesson = nextTrack.lessons.firstOrNull { it.id !in completedIds } ?: nextTrack.lessons.first()
            val trackDone = nextTrack.lessons.count { it.id in completedIds }
            val pct = if (nextTrack.lessons.isEmpty()) 0 else ((trackDone * 100) / nextTrack.lessons.size)
            val displayPct = if (pct == 0) 20 else pct
            val animatedPct by animateFloatAsState(
                targetValue = (displayPct / 100f).coerceIn(0.08f, 1f),
                animationSpec = spring(stiffness = 260f),
                label = "continueProgress"
            )
            val lockedNext = (nextTrack.isPremium || nextLesson.isPremium) && !isProActive

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("continue_learning_card")
                    .clickable {
                        if (lockedNext) onOpenPremium() else onOpenLesson(nextTrack, nextLesson)
                    },
                shape = RoundedCornerShape(22.dp),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(VividViolet, Color(0xFF4F46E5), Color(0xFF0284C7))))
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.16f),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = nextTrack.languages.firstOrNull()?.take(3)?.uppercase() ?: "DEV",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = labels.continueLearning,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.88f)
                        )
                        Text(
                            text = nextLesson.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "$pct% Completed • ${nextTrack.title}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 1
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.Black.copy(alpha = 0.25f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedPct)
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Brush.horizontalGradient(listOf(NeonBlue, Color.White)))
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (lockedNext) Icons.Default.Lock else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Resume Lesson",
                                tint = VividViolet
                            )
                        }
                    }
                }
            }
        }

        // 2x2 Quick Action Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionTile(
                        title = labels.navCourses,
                        subtitle = "15 Languages",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        gradientColors = listOf(RoyalBlue, Color(0xFF1D4ED8)),
                        testTag = "tile_courses",
                        modifier = Modifier.weight(1f)
                    ) {
                        onSelectCategory(CareerCategory.ALL)
                        scope.launch { listState.animateScrollToItem(6) }
                    }
                    QuickActionTile(
                        title = labels.navEditor,
                        subtitle = "Practice Online",
                        icon = Icons.Default.Code,
                        gradientColors = listOf(NeonGreen, Color(0xFF047857)),
                        testTag = "tile_code_editor",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToCodeLab
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionTile(
                        title = labels.navQuizzes,
                        subtitle = "Test Your Knowledge",
                        icon = Icons.Default.EmojiEvents,
                        gradientColors = listOf(NeonPurple, Color(0xFF6D28D9)),
                        testTag = "tile_quizzes",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToQuizzes
                    )
                    QuickActionTile(
                        title = "Certificates",
                        subtitle = "Get Recognized",
                        icon = Icons.Default.WorkspacePremium,
                        gradientColors = listOf(NeonAmber, Color(0xFFD97706)),
                        testTag = "tile_certificates",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToCertificates
                    )
                }
            }
        }

        // Learning Progress Dashboard Card
        item {
            when (profileState) {
                is UiState.Loading -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(92.dp),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        }
                    }
                }
                is UiState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = profileState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
                is UiState.Success -> {
                    val codingHours = ((profile?.totalCodingMinutes ?: 0L) / 60.0)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AnimatedStreakXpBar(
                            streakDays = profile?.streakDays ?: 1L,
                            xp = profile?.xp ?: 0L,
                            lessonsCompleted = profile?.lessonsCompleted ?: 0L
                        )
                        LanguageMasteryCard(
                            tracks = CurriculumRepository.tracks,
                            completedIds = completedIds,
                            learningGoal = profile?.learningGoal ?: "HTML, CSS & JavaScript",
                            codingHoursLabel = String.format(Locale.US, "%.1f hrs coded", codingHours)
                        )
                    }
                }
            }
        }

        // Popular Courses Header & Category Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = labels.popularCourses,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = {
                        savedOnlyFilter = false
                        onSelectCategory(CareerCategory.ALL)
                    }) {
                        Text("View All", color = NeonBlue)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = savedOnlyFilter,
                        onClick = { savedOnlyFilter = !savedOnlyFilter },
                        label = { Text("Saved (${savedIds.size})") },
                        modifier = Modifier.testTag("filter_saved_courses")
                    )
                    CareerCategory.entries.forEach { category ->
                        val selected = category == selectedCategory
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectCategory(category) },
                            label = { Text(category.label) },
                            modifier = Modifier.testTag("category_chip_${category.name}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }

        itemsIndexed(filteredTracks, key = { _, track -> track.id }) { idx, track ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically { (idx + 1) * 16 }
            ) {
                TrackCard(
                    track = track,
                    completedIds = completedIds,
                    isSaved = track.id in savedIds,
                    isProActive = isProActive,
                    onToggleSave = { onToggleSaveCourse(track.id) },
                    onOpenLesson = { lesson ->
                        val locked = (track.isPremium || lesson.isPremium) && !isProActive
                        if (locked) onOpenPremium() else onOpenLesson(track, lesson)
                    }
                )
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(92.dp)
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradientColors))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.2f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.padding(8.dp).size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.88f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun LanguageMasteryCard(
    tracks: List<CareerTrack>,
    completedIds: Set<String>,
    learningGoal: String,
    codingHoursLabel: String
) {
    val totalLessons = tracks.sumOf { it.lessons.size }.coerceAtLeast(1)
    val totalDone = tracks.sumOf { track -> track.lessons.count { it.id in completedIds } }
    val overallPct = (totalDone * 100) / totalLessons

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                Column {
                    Text("Learning Progress Dashboard", style = MaterialTheme.typography.titleMedium)
                    Text("Goal: $learningGoal • $codingHoursLabel", style = MaterialTheme.typography.bodySmall, color = NeonBlue)
                }
                Text(
                    text = "$overallPct%",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonGreen,
                    fontWeight = FontWeight.Bold
                )
            }
            tracks.take(3).forEach { track ->
                val done = track.lessons.count { it.id in completedIds }
                val total = track.lessons.size.coerceAtLeast(1)
                val ratio = done.toFloat() / total
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = track.languages.firstOrNull() ?: track.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(74.dp),
                        maxLines = 1
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ratio.coerceIn(0.04f, 1f))
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(track.accentHex))
                        )
                    }
                    Text(
                        text = "$done/$total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuSheetItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = title, tint = accent)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudentRegistrationDialog(
    initialName: String,
    initialGoal: String,
    initialLevel: String,
    dismissible: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (displayName: String, learningGoal: String, experienceLevel: String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var goal by remember(initialGoal) { mutableStateOf(initialGoal) }
    var level by remember(initialLevel) { mutableStateOf(initialLevel) }

    AlertDialog(
        onDismissRequest = { if (dismissible) onDismiss() },
        title = { Text("Student Registration") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Personalize your TejasCode learning track and certificate name.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 60) name = it },
                    label = { Text("Full Name (for Certificates)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("registration_name_input")
                )
                Text("Learning Goal", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("HTML, CSS & JavaScript", "Python & AI", "Full-Stack Web", "Mobile & Kotlin", "Systems (Go/Rust)", "Interview Prep").forEach { item ->
                        FilterChip(
                            selected = goal == item,
                            onClick = { goal = item },
                            label = { Text(item) }
                        )
                    }
                }
                Text("Experience Level", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Beginner", "Intermediate", "Advanced").forEach { item ->
                        FilterChip(
                            selected = level == item,
                            onClick = { level = item },
                            label = { Text(item) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(name.ifBlank { "Student" }, goal, level) },
                modifier = Modifier.testTag("complete_registration_button")
            ) {
                Text("Save Profile")
            }
        },
        dismissButton = if (dismissible) {
            {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        } else null
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrackCard(
    track: CareerTrack,
    completedIds: Set<String>,
    isSaved: Boolean,
    isProActive: Boolean,
    onToggleSave: () -> Unit,
    onOpenLesson: (CodingLesson) -> Unit
) {
    val accent = Color(track.accentHex)
    val doneCount = track.lessons.count { it.id in completedIds }
    val progress by animateFloatAsState(
        targetValue = if (track.lessons.isEmpty()) 0f else doneCount.toFloat() / track.lessons.size,
        animationSpec = spring(stiffness = 300f),
        label = "trackProgress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (track.isPremium) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = NeonAmber.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = "PRO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonAmber,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = track.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onToggleSave) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = if (isSaved) "Remove saved course" else "Save course",
                        tint = if (isSaved) NeonBlue else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AnimatedDifficultyPill(track.difficulty, accent)
            }

            Spacer(Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                track.languages.forEach { lang ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = lang,
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$doneCount / ${track.lessons.size} modules",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (doneCount == track.lessons.size && track.lessons.isNotEmpty()) {
                    Text(
                        text = "Certificate Earned",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(accent)
                )
            }

            Spacer(Modifier.height(12.dp))

            track.lessons.forEach { lesson ->
                val isDone = lesson.id in completedIds
                val isLocked = (track.isPremium || lesson.isPremium) && !isProActive
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("lesson_item_${lesson.id}")
                        .clickable { onOpenLesson(lesson) },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    border = BorderStroke(
                        1.dp,
                        when {
                            isDone -> NeonGreen.copy(alpha = 0.4f)
                            isLocked -> NeonAmber.copy(alpha = 0.35f)
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = when {
                                isDone -> NeonGreen.copy(alpha = 0.2f)
                                isLocked -> NeonAmber.copy(alpha = 0.16f)
                                else -> accent.copy(alpha = 0.16f)
                            }
                        ) {
                            Icon(
                                imageVector = when {
                                    isDone -> Icons.Default.CheckCircle
                                    isLocked -> Icons.Default.Lock
                                    else -> Icons.Default.PlayArrow
                                },
                                contentDescription = if (isLocked) "Pro lesson locked" else if (isDone) "Completed" else "Start lesson",
                                tint = when {
                                    isDone -> NeonGreen
                                    isLocked -> NeonAmber
                                    else -> accent
                                },
                                modifier = Modifier.padding(8.dp).size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = lesson.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (lesson.isPremium && !track.isPremium) {
                                    Text("PRO", style = MaterialTheme.typography.labelSmall, color = NeonAmber)
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "${lesson.durationMinutes} min • +${lesson.xpReward} XP",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
