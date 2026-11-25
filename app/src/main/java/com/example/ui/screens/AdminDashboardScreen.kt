package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CurriculumRepository
import com.example.model.CareerCategory
import com.example.model.ManagedCourse
import com.example.model.ModerationReport
import com.example.model.PaymentTransaction
import com.example.model.PlanConfig
import com.example.model.SettlementRecord
import com.example.model.SubscriptionRecord
import com.example.model.SubscriptionStatus
import com.example.model.UiState
import com.example.ui.theme.CoralRose
import com.example.ui.theme.NavyBg
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.viewmodel.AcademyViewModel
import com.example.util.CertificateExportHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminDashboardScreen(
    isAdmin: Boolean,
    userEmail: String?,
    adminCoursesState: UiState<List<ManagedCourse>>,
    planConfigState: UiState<PlanConfig>,
    adminSubscriptionsState: UiState<List<SubscriptionRecord>>,
    adminTransactionsState: UiState<List<PaymentTransaction>>,
    adminSettlementsState: UiState<List<SettlementRecord>>,
    moderationReportsState: UiState<List<ModerationReport>>,
    billingBusy: Boolean,
    onBack: () -> Unit,
    onSaveCourse: (
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
        isPremium: Boolean,
        resourceUrl: String
    ) -> Unit,
    onDeleteCourse: (String) -> Unit,
    onSavePlanConfig: (PlanConfig) -> Unit,
    onReconcileSettlements: () -> Unit,
    onResolveReport: (reportId: String, status: String, hidePostId: String?) -> Unit,
    onSendAnnouncement: (title: String, message: String, targetTier: String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var selectedTab by remember { mutableIntStateOf(0) }
    var editingCourse by remember { mutableStateOf<ManagedCourse?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    if (showCreateDialog || editingCourse != null) {
        CourseEditorDialog(
            initial = editingCourse,
            onDismiss = {
                showCreateDialog = false
                editingCourse = null
            },
            onSave = { title, subtitle, language, category, difficulty, duration, xp, code, summary, quizQ, quizOpts, correctIdx, vis, isPremium, resourceUrl ->
                onSaveCourse(
                    editingCourse?.id,
                    title,
                    subtitle,
                    language,
                    category,
                    difficulty,
                    duration,
                    xp,
                    code,
                    summary,
                    quizQ,
                    quizOpts,
                    correctIdx,
                    vis,
                    isPremium,
                    resourceUrl
                )
                showCreateDialog = false
                editingCourse = null
            }
        )
    }

    if (pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Delete Course?") },
            text = { Text("This permanently removes the course from the TejasCode live catalog.") },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDeleteId?.let(onDeleteCourse)
                        pendingDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRose)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("admin_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Dashboard"
                        )
                    }
                    Column {
                        Text(
                            text = "Admin Control Console",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Courses • Pricing • Settlements • Moderation",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonBlue
                        )
                    }
                }
                if (isAdmin && selectedTab == 0) {
                    Button(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("admin_new_course_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonBlue,
                            contentColor = Color(0xFF031526)
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Course", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (!isAdmin) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, CoralRose.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Admin Restricted",
                            tint = CoralRose,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "Admin Privileges Required",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Signed in as ${userEmail ?: "Student"}. Only the verified TejasCode administrator (${AcademyViewModel.ADMIN_EMAIL}) can publish courses, configure pricing plans, view settlements, or moderate content.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(onClick = onBack, modifier = Modifier.height(48.dp)) {
                            Text("Return to Student Dashboard")
                        }
                    }
                }
            }
        } else {
            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 12.dp) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Courses") },
                    modifier = Modifier.testTag("admin_tab_courses")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Pricing & Plans") },
                    modifier = Modifier.testTag("admin_tab_pricing")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Payments & Settlements") },
                    modifier = Modifier.testTag("admin_tab_payments")
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Moderation & Broadcasts") },
                    modifier = Modifier.testTag("admin_tab_moderation")
                )
            }

            when (selectedTab) {
                0 -> CoursesAdminTab(
                    adminCoursesState = adminCoursesState,
                    onEditCourse = { editingCourse = it },
                    onDeleteCourse = { pendingDeleteId = it },
                    onQuickPublishSample = {
                        onSaveCourse(
                            null,
                            "DOM & Interactive JS",
                            "Select elements, handle click events, and update the live DOM",
                            "JavaScript",
                            CareerCategory.WEB.name,
                            "Beginner",
                            6L,
                            120L,
                            "const btn = document.querySelector('#start');\nbtn.addEventListener('click', () => console.log('Clicked!'));",
                            "Use document.querySelector() and addEventListener() to respond cleanly to user interactions.",
                            "Which DOM method attaches an event handler to a button?",
                            listOf("addEventListener()", "attachScript()", "bindStyle()", "queryStyle()"),
                            0L,
                            "public",
                            false,
                            ""
                        )
                    }
                )
                1 -> PricingPlansAdminTab(
                    planConfigState = planConfigState,
                    onSavePlanConfig = onSavePlanConfig
                )
                2 -> PaymentsSettlementsAdminTab(
                    planConfigState = planConfigState,
                    adminSubscriptionsState = adminSubscriptionsState,
                    adminTransactionsState = adminTransactionsState,
                    adminSettlementsState = adminSettlementsState,
                    billingBusy = billingBusy,
                    onReconcileSettlements = onReconcileSettlements
                )
                3 -> ModerationBroadcastsAdminTab(
                    moderationReportsState = moderationReportsState,
                    onResolveReport = onResolveReport,
                    onSendAnnouncement = onSendAnnouncement
                )
            }
        }
    }
}

@Composable
private fun CoursesAdminTab(
    adminCoursesState: UiState<List<ManagedCourse>>,
    onEditCourse: (ManagedCourse) -> Unit,
    onDeleteCourse: (String) -> Unit,
    onQuickPublishSample: () -> Unit
) {
    val courses = (adminCoursesState as? UiState.Success)?.data.orEmpty()
    val publishedCount = courses.count { it.visibility == "public" }
    val draftCount = courses.count { it.visibility != "public" }
    val proCount = courses.count { it.isPremium }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(Color(0xFF191247), Color(0xFF0A244D))))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = NeonBlue)
                        Text(
                            text = "Instructor Catalog Controls",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AdminStatChip("Total", "${courses.size}", NeonBlue, Modifier.weight(1f))
                        AdminStatChip("Live", "$publishedCount", NeonGreen, Modifier.weight(1f))
                        AdminStatChip("Pro", "$proCount", NeonPurple, Modifier.weight(1f))
                        AdminStatChip("Drafts", "$draftCount", NeonAmber, Modifier.weight(1f))
                    }
                    OutlinedButton(
                        onClick = onQuickPublishSample,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("admin_seed_course_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Quick-Publish Sample JS Course")
                    }
                }
            }
        }

        when (adminCoursesState) {
            is UiState.Loading -> item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> item { Text(adminCoursesState.message, color = CoralRose) }
            is UiState.Success -> {
                if (courses.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("No custom courses published yet", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Tap 'Add Course' or 'Quick-Publish Sample JS Course' to add live courses across all 15 languages.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(courses, key = { it.id }) { course ->
                        val isPublic = course.visibility == "public"
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(course.title, style = MaterialTheme.typography.titleMedium)
                                        Text(course.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (course.isPremium) {
                                            Surface(
                                                shape = RoundedCornerShape(999.dp),
                                                color = NeonPurple.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "PRO",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = NeonPurple,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = (if (isPublic) NeonGreen else NeonAmber).copy(alpha = 0.16f)
                                        ) {
                                            Text(
                                                text = if (isPublic) "LIVE" else "DRAFT",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isPublic) NeonGreen else NeonAmber,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${course.language} • ${course.difficulty} • ${course.durationMinutes} min • +${course.xpReward} XP",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NeonBlue
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = { onEditCourse(course) },
                                        modifier = Modifier.testTag("admin_edit_${course.id}")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit course", modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Edit")
                                    }
                                    TextButton(
                                        onClick = { onDeleteCourse(course.id) },
                                        modifier = Modifier.testTag("admin_delete_${course.id}")
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete course", tint = CoralRose, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Delete", color = CoralRose)
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
private fun PricingPlansAdminTab(
    planConfigState: UiState<PlanConfig>,
    onSavePlanConfig: (PlanConfig) -> Unit
) {
    val current = (planConfigState as? UiState.Success)?.data ?: PlanConfig()

    var monthlyMajor by remember(current.monthlyPriceMinor) {
        mutableStateOf(if (current.monthlyPriceMinor > 0L) (current.monthlyPriceMinor / 100L).toString() else "")
    }
    var yearlyMajor by remember(current.yearlyPriceMinor) {
        mutableStateOf(if (current.yearlyPriceMinor > 0L) (current.yearlyPriceMinor / 100L).toString() else "")
    }
    var currency by remember(current.currency) { mutableStateOf(current.currency.ifBlank { "INR" }) }
    var symbol by remember(current.currencySymbol) { mutableStateOf(current.currencySymbol.ifBlank { "₹" }) }
    var trialDays by remember(current.trialDays) { mutableStateOf(current.trialDays.toString()) }
    var playMonthlyId by remember(current.monthlyPlayProductId) { mutableStateOf(current.monthlyPlayProductId) }
    var playMonthlyBase by remember(current.monthlyPlayBasePlanId) { mutableStateOf(current.monthlyPlayBasePlanId) }
    var playYearlyId by remember(current.yearlyPlayProductId) { mutableStateOf(current.yearlyPlayProductId) }
    var playYearlyBase by remember(current.yearlyPlayBasePlanId) { mutableStateOf(current.yearlyPlayBasePlanId) }
    var rzpMonthlyPlan by remember(current.razorpayMonthlyPlanId) { mutableStateOf(current.razorpayMonthlyPlanId) }
    var rzpYearlyPlan by remember(current.razorpayYearlyPlanId) { mutableStateOf(current.razorpayYearlyPlanId) }
    var razorpayEnabled by remember(current.razorpayEnabled) { mutableStateOf(current.razorpayEnabled) }
    var freeAiLimit by remember(current.freeDailyAiLimit) { mutableStateOf(current.freeDailyAiLimit.toString()) }
    var proAiLimit by remember(current.proDailyAiLimit) { mutableStateOf(current.proDailyAiLimit.toString()) }
    var termsSummary by remember(current.billingTermsSummary) { mutableStateOf(current.billingTermsSummary) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Live Subscription Pricing & Billing Config", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Configure Monthly & Annual prices in major currency units (stored in Firestore as minor paise/cents). Student checkout remains disabled until valid >0 prices are saved.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = monthlyMajor,
                            onValueChange = { monthlyMajor = it.filter(Char::isDigit).take(6) },
                            label = { Text("Monthly Price ($symbol)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("admin_monthly_price_input")
                        )
                        OutlinedTextField(
                            value = yearlyMajor,
                            onValueChange = { yearlyMajor = it.filter(Char::isDigit).take(6) },
                            label = { Text("Yearly Price ($symbol)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("admin_yearly_price_input")
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = currency,
                            onValueChange = { currency = it.uppercase().take(3) },
                            label = { Text("Currency (INR/USD)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = symbol,
                            onValueChange = { symbol = it.take(3) },
                            label = { Text("Symbol (₹/$)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = trialDays,
                            onValueChange = { trialDays = it.filter(Char::isDigit).take(2) },
                            label = { Text("Trial Days") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text("Google Play Billing Product IDs", style = MaterialTheme.typography.labelLarge, color = NeonBlue)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = playMonthlyId,
                            onValueChange = { playMonthlyId = it.trim() },
                            label = { Text("Monthly Product ID") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = playMonthlyBase,
                            onValueChange = { playMonthlyBase = it.trim() },
                            label = { Text("Monthly Base Plan") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = playYearlyId,
                            onValueChange = { playYearlyId = it.trim() },
                            label = { Text("Yearly Product ID") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = playYearlyBase,
                            onValueChange = { playYearlyBase = it.trim() },
                            label = { Text("Yearly Base Plan") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text("Razorpay Merchant Subscription Plan IDs", style = MaterialTheme.typography.labelLarge, color = NeonPurple)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = rzpMonthlyPlan,
                            onValueChange = { rzpMonthlyPlan = it.trim() },
                            label = { Text("Razorpay Monthly plan_xxx") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = rzpYearlyPlan,
                            onValueChange = { rzpYearlyPlan = it.trim() },
                            label = { Text("Razorpay Yearly plan_xxx") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Razorpay Gateway (UPI/Card)", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = razorpayEnabled, onCheckedChange = { razorpayEnabled = it })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = freeAiLimit,
                            onValueChange = { freeAiLimit = it.filter(Char::isDigit).take(4) },
                            label = { Text("Free Daily AI Limit") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = proAiLimit,
                            onValueChange = { proAiLimit = it.filter(Char::isDigit).take(4) },
                            label = { Text("Pro Daily AI Limit") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = termsSummary,
                        onValueChange = { if (it.length <= 500) termsSummary = it },
                        label = { Text("Subscription Disclosure Summary") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val mMinor = (monthlyMajor.toLongOrNull() ?: 0L) * 100L
                            val yMinor = (yearlyMajor.toLongOrNull() ?: 0L) * 100L
                            onSavePlanConfig(
                                current.copy(
                                    monthlyPriceMinor = mMinor,
                                    yearlyPriceMinor = yMinor,
                                    currency = currency.ifBlank { "INR" },
                                    currencySymbol = symbol.ifBlank { "₹" },
                                    trialDays = trialDays.toLongOrNull() ?: 0L,
                                    monthlyPlayProductId = playMonthlyId,
                                    monthlyPlayBasePlanId = playMonthlyBase,
                                    yearlyPlayProductId = playYearlyId,
                                    yearlyPlayBasePlanId = playYearlyBase,
                                    razorpayMonthlyPlanId = rzpMonthlyPlan,
                                    razorpayYearlyPlanId = rzpYearlyPlan,
                                    razorpayEnabled = razorpayEnabled,
                                    freeDailyAiLimit = (freeAiLimit.toLongOrNull() ?: 5L).coerceAtLeast(1L),
                                    proDailyAiLimit = (proAiLimit.toLongOrNull() ?: 100L).coerceAtLeast(1L),
                                    billingTermsSummary = termsSummary
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_plan_config_button")
                    ) {
                        Text("Publish Pricing & Plan Config", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentsSettlementsAdminTab(
    planConfigState: UiState<PlanConfig>,
    adminSubscriptionsState: UiState<List<SubscriptionRecord>>,
    adminTransactionsState: UiState<List<PaymentTransaction>>,
    adminSettlementsState: UiState<List<SettlementRecord>>,
    billingBusy: Boolean,
    onReconcileSettlements: () -> Unit
) {
    val context = LocalContext.current
    val planConfig = (planConfigState as? UiState.Success)?.data ?: PlanConfig()
    val subs = (adminSubscriptionsState as? UiState.Success)?.data.orEmpty()
    val transactions = (adminTransactionsState as? UiState.Success)?.data.orEmpty()
    val settlements = (adminSettlementsState as? UiState.Success)?.data.orEmpty()

    var filterQuery by remember { mutableStateOf("") }

    val activeSubs = subs.count { it.subscriptionStatus == SubscriptionStatus.Active || it.subscriptionStatus == SubscriptionStatus.Trialing }
    val cancelledSubs = subs.count { it.subscriptionStatus == SubscriptionStatus.Cancelled }
    val failedOrRefunded = subs.count { it.subscriptionStatus == SubscriptionStatus.Refunded || it.subscriptionStatus == SubscriptionStatus.Revoked }
    val monthlyCount = subs.count { it.billingInterval == "monthly" }
    val yearlyCount = subs.count { it.billingInterval == "yearly" }

    val totalGrossMinor = transactions.filter { it.status == "captured" || it.status == "settled" }.sumOf { it.grossAmountMinor }
    val totalFeesMinor = transactions.filter { it.status == "captured" || it.status == "settled" }.sumOf { it.gatewayFeeMinor + it.taxMinor }
    val netSettledMinor = settlements.filter { it.status == "processed" || it.status == "settled" }.sumOf { it.netPayoutMinor }
    val pendingPayoutMinor = settlements.filter { it.status == "pending" || it.status == "created" }.sumOf { it.netPayoutMinor }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(Color(0xFF062C22), Color(0xFF0A244D))))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Payments, contentDescription = null, tint = NeonGreen)
                        Text("Revenue & Bank Settlement Analytics", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AdminStatChip("Gross", PlanConfig.formatMinorAmount(totalGrossMinor, planConfig.currencySymbol), NeonGreen, Modifier.weight(1f))
                        AdminStatChip("Fees+Tax", PlanConfig.formatMinorAmount(totalFeesMinor, planConfig.currencySymbol), NeonAmber, Modifier.weight(1f))
                        AdminStatChip("Settled", PlanConfig.formatMinorAmount(netSettledMinor, planConfig.currencySymbol), NeonBlue, Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AdminStatChip("Active Pro", "$activeSubs", NeonGreen, Modifier.weight(1f))
                        AdminStatChip("M / Y", "$monthlyCount / $yearlyCount", NeonPurple, Modifier.weight(1f))
                        AdminStatChip("Canc / Rev", "$cancelledSubs / $failedOrRefunded", CoralRose, Modifier.weight(1f))
                    }
                    Text(
                        text = "Pending Merchant Bank Payout: ${PlanConfig.formatMinorAmount(pendingPayoutMinor, planConfig.currencySymbol)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onReconcileSettlements,
                            enabled = !billingBusy,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("admin_reconcile_button")
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Sync Bank Settlements", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                CertificateExportHelper.exportPaymentsCsvAndShare(context, transactions, settlements)
                                    .onFailure {
                                        Toast.makeText(context, "Export failed: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                            },
                            modifier = Modifier.weight(0.8f).height(48.dp).testTag("admin_export_csv_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Export CSV", maxLines = 1)
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = filterQuery,
                onValueChange = { filterQuery = it },
                label = { Text("Search user ID, status, transaction, or UTR...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item { Text("Merchant Bank Settlements (${settlements.size})", style = MaterialTheme.typography.titleMedium) }
        if (settlements.isEmpty()) {
            item {
                Text(
                    text = "No Razorpay / Play bank settlements synced yet. Tap 'Sync Bank Settlements' once your backend is live.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val filteredSettlements = settlements.filter {
                filterQuery.isBlank() ||
                    it.settlementId.contains(filterQuery, true) ||
                    it.utrNumber.orEmpty().contains(filterQuery, true) ||
                    it.status.contains(filterQuery, true)
            }
            items(filteredSettlements, key = { it.id }) { st ->
                val dateTxt = st.settledAtMs?.let { SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(it)) } ?: "Pending"
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = NeonBlue)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${st.settlementId} • ${st.status.uppercase()}", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Bank: ${st.bankAccountMasked.ifBlank { "Verified Account" }} • UTR: ${st.utrNumber ?: "—"} • $dateTxt",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = PlanConfig.formatMinorAmount(st.netPayoutMinor, planConfig.currencySymbol),
                            style = MaterialTheme.typography.titleMedium,
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item { Text("Recent Payment Transactions (${transactions.size})", style = MaterialTheme.typography.titleMedium) }
        val filteredTx = transactions.filter {
            filterQuery.isBlank() ||
                it.userId.contains(filterQuery, true) ||
                it.transactionId.contains(filterQuery, true) ||
                it.status.contains(filterQuery, true) ||
                it.provider.contains(filterQuery, true)
        }
        items(filteredTx, key = { it.id }) { tx ->
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${tx.transactionId} (${tx.provider})", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "User: ${tx.userId.take(10)}... • ${tx.billingInterval} • ${tx.status}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = PlanConfig.formatMinorAmount(tx.grossAmountMinor, planConfig.currencySymbol),
                        style = MaterialTheme.typography.titleSmall,
                        color = NeonAmber,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ModerationBroadcastsAdminTab(
    moderationReportsState: UiState<List<ModerationReport>>,
    onResolveReport: (reportId: String, status: String, hidePostId: String?) -> Unit,
    onSendAnnouncement: (title: String, message: String, targetTier: String) -> Unit
) {
    val reports = (moderationReportsState as? UiState.Success)?.data.orEmpty()
    var announceTitle by remember { mutableStateOf("") }
    var announceBody by remember { mutableStateOf("") }
    var targetTier by remember { mutableStateOf("all") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = NeonPurple)
                        Text("Broadcast Platform Notification", style = MaterialTheme.typography.titleMedium)
                    }
                    OutlinedTextField(
                        value = announceTitle,
                        onValueChange = { if (it.length <= 100) announceTitle = it },
                        label = { Text("Announcement Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = announceBody,
                        onValueChange = { if (it.length <= 400) announceBody = it },
                        label = { Text("Message to Students") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("all", "free", "pro").forEach { tier ->
                            FilterChip(
                                selected = targetTier == tier,
                                onClick = { targetTier = tier },
                                label = { Text(tier.uppercase()) }
                            )
                        }
                    }
                    Button(
                        onClick = {
                            if (announceTitle.isNotBlank() && announceBody.isNotBlank()) {
                                onSendAnnouncement(announceTitle.trim(), announceBody.trim(), targetTier)
                                announceTitle = ""
                                announceBody = ""
                            }
                        },
                        enabled = announceTitle.isNotBlank() && announceBody.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Send Announcement")
                    }
                }
            }
        }

        item {
            Text("Community Moderation Queue (${reports.size})", style = MaterialTheme.typography.titleMedium)
        }
        if (reports.isEmpty()) {
            item {
                Text(
                    text = "No user reports submitted yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(reports, key = { it.id }) { report ->
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${report.targetType} • ${report.status.uppercase()}", style = MaterialTheme.typography.titleSmall, color = NeonAmber)
                        Text("Target ID: ${report.targetId}", style = MaterialTheme.typography.labelSmall)
                        Text("Reason: ${report.reason}", style = MaterialTheme.typography.bodySmall)
                        if (report.status == "open") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onResolveReport(report.id, "dismissed", null) }) {
                                    Text("Dismiss")
                                }
                                Button(
                                    onClick = {
                                        val postId = if (report.targetType == "community_qa") report.targetId else null
                                        onResolveReport(report.id, "resolved", postId)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CoralRose)
                                ) {
                                    Text("Hide Content & Resolve")
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
private fun AdminStatChip(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = NavyBg.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f), maxLines = 1)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CourseEditorDialog(
    initial: ManagedCourse?,
    onDismiss: () -> Unit,
    onSave: (
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
        isPremium: Boolean,
        resourceUrl: String
    ) -> Unit
) {
    var title by remember(initial) { mutableStateOf(initial?.title ?: "") }
    var subtitle by remember(initial) { mutableStateOf(initial?.subtitle ?: "") }
    var language by remember(initial) { mutableStateOf(initial?.language ?: "HTML") }
    var category by remember(initial) { mutableStateOf(initial?.category ?: CareerCategory.WEB.name) }
    var difficulty by remember(initial) { mutableStateOf(initial?.difficulty ?: "Beginner") }
    var durationText by remember(initial) { mutableStateOf((initial?.durationMinutes ?: 5L).toString()) }
    var xpText by remember(initial) { mutableStateOf((initial?.xpReward ?: 100L).toString()) }
    var lessonSummary by remember(initial) { mutableStateOf(initial?.lessonSummary ?: "") }
    var starterCode by remember(initial) { mutableStateOf(initial?.starterCode ?: "<h1>Hello TejasCode</h1>") }
    var quizQuestion by remember(initial) { mutableStateOf(initial?.quizQuestion ?: "Which tag defines the largest heading?") }
    var optionsRaw by remember(initial) {
        mutableStateOf(
            initial?.quizOptions?.joinToString(" | ") ?: "<h1> | <p> | <div> | <span>"
        )
    }
    var correctIdx by remember(initial) { mutableIntStateOf((initial?.correctOptionIndex ?: 0L).toInt()) }
    var isPublic by remember(initial) { mutableStateOf((initial?.visibility ?: "public") == "public") }
    var isPremium by remember(initial) { mutableStateOf(initial?.isPremium ?: false) }
    var resourceUrl by remember(initial) { mutableStateOf(initial?.resourceUrl ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Create TejasCode Course" else "Edit Course") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 80) title = it },
                    label = { Text("Course Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("course_title_input")
                )
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { if (it.length <= 160) subtitle = it },
                    label = { Text("Subtitle / Track Summary") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Language", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CurriculumRepository.allSupportedLanguages.forEach { lang ->
                        FilterChip(
                            selected = language == lang,
                            onClick = {
                                language = lang
                                category = when (lang) {
                                    "Python", "SQL" -> CareerCategory.DATA_AI.name
                                    "Go", "Rust" -> CareerCategory.ESSENTIALS.name
                                    "Java", "Kotlin", "Swift", "C", "C++", "C#" -> CareerCategory.SOFTWARE_MOBILE.name
                                    else -> CareerCategory.WEB.name
                                }
                            },
                            label = { Text(lang) }
                        )
                    }
                }
                Text("Difficulty", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Beginner", "Intermediate", "Advanced").forEach { diff ->
                        FilterChip(
                            selected = difficulty == diff,
                            onClick = { difficulty = diff },
                            label = { Text(diff) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it.filter(Char::isDigit).take(3) },
                        label = { Text("Minutes") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = xpText,
                        onValueChange = { xpText = it.filter(Char::isDigit).take(4) },
                        label = { Text("XP") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = resourceUrl,
                    onValueChange = { if (it.length <= 300) resourceUrl = it },
                    label = { Text("Optional Video / PDF Resource URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = lessonSummary,
                    onValueChange = { if (it.length <= 1000) lessonSummary = it },
                    label = { Text("Lesson Explanation") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = starterCode,
                    onValueChange = { if (it.length <= 2000) starterCode = it },
                    label = { Text("Code Example") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = quizQuestion,
                    onValueChange = { if (it.length <= 300) quizQuestion = it },
                    label = { Text("Quiz Question") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = optionsRaw,
                    onValueChange = { optionsRaw = it },
                    label = { Text("Options (separated by | )") },
                    modifier = Modifier.fillMaxWidth()
                )
                val parsedOpts = optionsRaw.split("|").map { it.trim() }.filter { it.isNotBlank() }
                    .ifEmpty { listOf("Option A", "Option B") }
                Text("Correct Option", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    parsedOpts.forEachIndexed { idx, opt ->
                        FilterChip(
                            selected = correctIdx == idx,
                            onClick = { correctIdx = idx },
                            label = { Text("${idx + 1}. ${opt.take(16)}") }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pro-Only Premium Course", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isPremium, onCheckedChange = { isPremium = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Publish Live to Students", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val opts = optionsRaw.split("|").map { it.trim() }.filter { it.isNotBlank() }
                        .ifEmpty { listOf("Option A", "Option B") }
                    onSave(
                        title.ifBlank { "$language Essentials" },
                        subtitle.ifBlank { "Interactive $language module on TejasCode" },
                        language,
                        category,
                        difficulty,
                        durationText.toLongOrNull() ?: 5L,
                        xpText.toLongOrNull() ?: 100L,
                        starterCode,
                        lessonSummary,
                        quizQuestion,
                        opts,
                        correctIdx.toLong().coerceIn(0L, (opts.size - 1).toLong()),
                        if (isPublic) "public" else "draft",
                        isPremium,
                        resourceUrl
                    )
                },
                modifier = Modifier.testTag("save_course_confirm_button")
            ) {
                Text("Save Course")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
