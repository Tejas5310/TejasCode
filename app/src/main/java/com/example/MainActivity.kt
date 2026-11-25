package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.auth.signOut
import com.example.billing.PaymentApiClient
import com.example.billing.PlayBillingManager
import com.example.billing.RazorpayCheckoutCoordinator
import com.example.data.AiTutorRepository
import com.example.data.AppPreferences
import com.example.data.CodeForgeRepository
import com.example.data.CurriculumRepository
import com.example.data.UserPreferencesManager
import com.example.firebase.configureAppCheck
import com.example.model.PlanConfig
import com.example.model.UiState
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AiTutorScreen
import com.example.ui.screens.ArenaScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CodeLabScreen
import com.example.ui.screens.CommunityScreen
import com.example.ui.screens.LearnScreen
import com.example.ui.screens.LessonPlayerScreen
import com.example.ui.screens.PremiumScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsLegalScreen
import com.example.ui.screens.TejasCodeSplashScreen
import com.example.ui.theme.CodeForgeTheme
import com.example.ui.viewmodel.AcademyViewModel
import com.example.ui.viewmodel.CodeLabTutorViewModel
import com.example.util.AppLocaleStrings
import com.example.util.LocalAppLabels
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import kotlinx.coroutines.launch

object Routes {
    const val LEARN = "learn"
    const val CODE_LAB = "codelab"
    const val AI_TUTOR = "tutor"
    const val ARENA = "arena"
    const val PROFILE = "profile"
    const val COMMUNITY = "community"
    const val PREMIUM = "premium"
    const val SETTINGS = "settings"
    const val ADMIN = "admin"
    const val LESSON = "lesson/{trackId}/{lessonId}"

    fun lessonRoute(trackId: String, lessonId: String) = "lesson/$trackId/$lessonId"
}

private data class NavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {

    private lateinit var preferencesManager: UserPreferencesManager
    private lateinit var paymentApiClient: PaymentApiClient
    private lateinit var playBillingManager: PlayBillingManager
    private lateinit var razorpayCoordinator: RazorpayCheckoutCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before UI or ViewModels
        configureAppCheck(this, intent)

        preferencesManager = UserPreferencesManager(applicationContext)
        paymentApiClient = PaymentApiClient()
        playBillingManager = PlayBillingManager(applicationContext, paymentApiClient)
        razorpayCoordinator = RazorpayCheckoutCoordinator(paymentApiClient)

        enableEdgeToEdge()
        setContent {
            val prefs by preferencesManager.preferencesFlow.collectAsStateWithLifecycle(initialValue = AppPreferences())
            val labels = remember(prefs.languageCode) { AppLocaleStrings.forCode(prefs.languageCode) }

            CompositionLocalProvider(LocalAppLabels provides labels) {
                CodeForgeTheme(darkTheme = prefs.darkTheme) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        var showSplash by rememberSaveable { mutableStateOf(true) }
                        if (showSplash) {
                            TejasCodeSplashScreen(onFinished = { showSplash = false })
                        } else {
                            TejasCodeRoot(
                                prefs = prefs,
                                preferencesManager = preferencesManager,
                                paymentApiClient = paymentApiClient,
                                playBillingManager = playBillingManager,
                                razorpayCoordinator = razorpayCoordinator
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        if (::razorpayCoordinator.isInitialized) {
            razorpayCoordinator.onRazorpaySuccess(razorpayPaymentId, paymentData, lifecycleScope)
        }
    }

    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        if (::razorpayCoordinator.isInitialized) {
            razorpayCoordinator.onRazorpayError(code, response)
        }
    }

    override fun onDestroy() {
        if (::playBillingManager.isInitialized) {
            playBillingManager.endConnection()
        }
        super.onDestroy()
    }
}

@Composable
fun TejasCodeRoot(
    prefs: AppPreferences,
    preferencesManager: UserPreferencesManager,
    paymentApiClient: PaymentApiClient,
    playBillingManager: PlayBillingManager,
    razorpayCoordinator: RazorpayCheckoutCoordinator
) {
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    LaunchedEffect(prefs.rememberMe) {
        if (auth.currentUser == null && prefs.rememberMe) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        val uid = user.uid
        key(uid) {
            AuthenticatedTejasCodeApp(
                currentUserId = uid,
                displayName = user.displayName ?: "TejasCode Student",
                email = user.email,
                photoUrl = user.photoUrl?.toString(),
                prefs = prefs,
                preferencesManager = preferencesManager,
                paymentApiClient = paymentApiClient,
                playBillingManager = playBillingManager,
                razorpayCoordinator = razorpayCoordinator,
                onSignOut = {
                    signOut(credentialManager = credentialManager, scope = scope)
                }
            )
        }
    }
}

@Composable
private fun AuthenticatedTejasCodeApp(
    currentUserId: String,
    displayName: String,
    email: String?,
    photoUrl: String?,
    prefs: AppPreferences,
    preferencesManager: UserPreferencesManager,
    paymentApiClient: PaymentApiClient,
    playBillingManager: PlayBillingManager,
    razorpayCoordinator: RazorpayCheckoutCoordinator,
    onSignOut: () -> Unit
) {
    val labels = LocalAppLabels.current
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    val academyViewModel: AcademyViewModel = viewModel(
        key = "academy_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                AcademyViewModel(
                    repository = CodeForgeRepository(db),
                    currentUserId = currentUserId,
                    initialDisplayName = displayName,
                    userEmail = email,
                    initialPhotoUrl = photoUrl,
                    paymentApiClient = paymentApiClient
                )
            }
        }
    )

    val labTutorViewModel: CodeLabTutorViewModel = viewModel(
        key = "lab_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                CodeLabTutorViewModel(
                    repository = CodeForgeRepository(db),
                    aiRepository = AiTutorRepository(),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val topTabs = remember(labels) {
        listOf(
            NavTab(Routes.LEARN, labels.navCourses, Icons.Filled.School, Icons.Outlined.School),
            NavTab(Routes.CODE_LAB, labels.navEditor, Icons.Filled.Code, Icons.Outlined.Code),
            NavTab(Routes.AI_TUTOR, labels.navTutor, Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
            NavTab(Routes.ARENA, labels.navQuizzes, Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents),
            NavTab(Routes.PROFILE, labels.navProfile, Icons.Filled.Person, Icons.Outlined.Person)
        )
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showChrome = currentRoute in topTabs.map { it.route }

    val profileState by academyViewModel.profileState.collectAsStateWithLifecycle()
    val planConfigState by academyViewModel.planConfigState.collectAsStateWithLifecycle()
    val entitlementState by academyViewModel.entitlementState.collectAsStateWithLifecycle()
    val userSubscriptionsState by academyViewModel.userSubscriptionsState.collectAsStateWithLifecycle()
    val userTransactionsState by academyViewModel.userTransactionsState.collectAsStateWithLifecycle()
    val publishedCoursesState by academyViewModel.publishedCoursesState.collectAsStateWithLifecycle()
    val adminCoursesState by academyViewModel.adminCoursesState.collectAsStateWithLifecycle()
    val adminSubscriptionsState by academyViewModel.adminSubscriptionsState.collectAsStateWithLifecycle()
    val adminTransactionsState by academyViewModel.adminTransactionsState.collectAsStateWithLifecycle()
    val adminSettlementsState by academyViewModel.adminSettlementsState.collectAsStateWithLifecycle()
    val moderationReportsState by academyViewModel.moderationReportsState.collectAsStateWithLifecycle()
    val notificationsState by academyViewModel.notificationsState.collectAsStateWithLifecycle()
    val selectedCategory by academyViewModel.selectedCategory.collectAsStateWithLifecycle()
    val unlockedCertTitle by academyViewModel.unlockedCertTitle.collectAsStateWithLifecycle()
    val statusBanner by academyViewModel.statusBanner.collectAsStateWithLifecycle()
    val billingBusy by academyViewModel.billingBusy.collectAsStateWithLifecycle()
    val certificatesState by academyViewModel.certificatesState.collectAsStateWithLifecycle()
    val leaderboardState by academyViewModel.leaderboardState.collectAsStateWithLifecycle()
    val communityState by academyViewModel.communityState.collectAsStateWithLifecycle()
    val communityQuestionsState by academyViewModel.communityQuestionsState.collectAsStateWithLifecycle()
    val battlePhase by academyViewModel.battlePhase.collectAsStateWithLifecycle()

    val editorLanguage by labTutorViewModel.editorLanguage.collectAsStateWithLifecycle()
    val editorTitle by labTutorViewModel.editorTitle.collectAsStateWithLifecycle()
    val editorCode by labTutorViewModel.editorCode.collectAsStateWithLifecycle()
    val executionResult by labTutorViewModel.executionResult.collectAsStateWithLifecycle()
    val aiSandboxState by labTutorViewModel.aiSandboxState.collectAsStateWithLifecycle()
    val snippetsState by labTutorViewModel.snippetsState.collectAsStateWithLifecycle()
    val labNotice by labTutorViewModel.labNotice.collectAsStateWithLifecycle()
    val chatMessages by labTutorViewModel.chatMessages.collectAsStateWithLifecycle()
    val tutorStatus by labTutorViewModel.tutorStatus.collectAsStateWithLifecycle()
    val aiQuotaSummary by labTutorViewModel.aiQuotaSummary.collectAsStateWithLifecycle()

    val playBillingState by playBillingManager.state.collectAsStateWithLifecycle()
    val razorpayState by razorpayCoordinator.state.collectAsStateWithLifecycle()

    val activePlanConfig = (planConfigState as? UiState.Success)?.data ?: PlanConfig()
    LaunchedEffect(
        activePlanConfig.monthlyPlayProductId,
        activePlanConfig.yearlyPlayProductId,
        activePlanConfig.monthlyPlayBasePlanId,
        activePlanConfig.yearlyPlayBasePlanId
    ) {
        playBillingManager.startConnection(activePlanConfig)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusBanner) {
        statusBanner?.let {
            snackbarHostState.showSnackbar(it)
            academyViewModel.dismissBanner()
        }
    }

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (showChrome && !useRail) {
                    NavigationBar {
                        topTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showChrome && useRail) {
                    NavigationRail {
                        topTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_rail_${tab.route}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Routes.LEARN,
                        modifier = Modifier.widthIn(max = 840.dp).fillMaxSize()
                    ) {
                        composable(Routes.LEARN) {
                            LearnScreen(
                                profileState = profileState,
                                entitlementState = entitlementState,
                                planConfigState = planConfigState,
                                publishedCoursesState = publishedCoursesState,
                                notificationsState = notificationsState,
                                selectedCategory = selectedCategory,
                                unlockedCertTitle = unlockedCertTitle,
                                isAdmin = academyViewModel.isAdmin,
                                onSelectCategory = academyViewModel::selectCategory,
                                onOpenLesson = { track, lesson ->
                                    navController.navigate(Routes.lessonRoute(track.id, lesson.id))
                                },
                                onToggleSaveCourse = academyViewModel::toggleSaveCourse,
                                onNavigateToCodeLab = { navigateToTab(Routes.CODE_LAB) },
                                onNavigateToQuizzes = { navigateToTab(Routes.ARENA) },
                                onNavigateToCertificates = { navigateToTab(Routes.PROFILE) },
                                onOpenCommunity = { navController.navigate(Routes.COMMUNITY) },
                                onOpenPremium = { navController.navigate(Routes.PREMIUM) },
                                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                                onOpenAdmin = { navController.navigate(Routes.ADMIN) },
                                onCompleteRegistration = academyViewModel::completeStudentRegistration,
                                onDismissCertDialog = academyViewModel::dismissUnlockedCertificate
                            )
                        }

                        composable(Routes.CODE_LAB) {
                            val activeName = (profileState as? UiState.Success)?.data?.displayName ?: displayName
                            CodeLabScreen(
                                selectedLanguage = editorLanguage,
                                title = editorTitle,
                                code = editorCode,
                                executionResult = executionResult,
                                aiSandboxState = aiSandboxState,
                                snippetsState = snippetsState,
                                labNotice = labNotice,
                                authorName = activeName,
                                quotaSummary = aiQuotaSummary,
                                onSelectLanguage = labTutorViewModel::selectLanguage,
                                onTitleChange = labTutorViewModel::updateTitle,
                                onCodeChange = labTutorViewModel::updateCode,
                                onInsertToken = labTutorViewModel::insertToken,
                                onLoadSample = labTutorViewModel::loadSampleProgram,
                                onRunLocal = labTutorViewModel::runLocalCode,
                                onRunAiSandbox = labTutorViewModel::runAiSandbox,
                                onSaveSnippet = labTutorViewModel::saveSnippet,
                                onLoadSnippet = labTutorViewModel::loadSnippet,
                                onDeleteSnippet = labTutorViewModel::deleteSnippet,
                                onPublishToCommunity = labTutorViewModel::publishToCommunity,
                                onAskTutorAboutCode = {
                                    val prompt = labTutorViewModel.prepareDebugPromptFromLab()
                                    labTutorViewModel.sendTutorMessage(prompt)
                                    navigateToTab(Routes.AI_TUTOR)
                                },
                                onOpenPremium = { navController.navigate(Routes.PREMIUM) },
                                onDismissNotice = labTutorViewModel::dismissLabNotice
                            )
                        }

                        composable(Routes.AI_TUTOR) {
                            AiTutorScreen(
                                messages = chatMessages,
                                tutorStatus = tutorStatus,
                                quotaSummary = aiQuotaSummary,
                                onSendMessage = labTutorViewModel::sendTutorMessage,
                                onReviewLabCode = {
                                    val prompt = labTutorViewModel.prepareDebugPromptFromLab()
                                    labTutorViewModel.sendTutorMessage(prompt)
                                },
                                onOpenPremium = { navController.navigate(Routes.PREMIUM) },
                                onDismissError = labTutorViewModel::clearTutorError
                            )
                        }

                        composable(Routes.ARENA) {
                            ArenaScreen(
                                battlePhase = battlePhase,
                                leaderboardState = leaderboardState,
                                currentUserId = currentUserId,
                                onStartBattle = academyViewModel::startBattle,
                                onSubmitBattleAnswer = academyViewModel::submitBattleAnswer,
                                onNextBattleQuestion = academyViewModel::nextBattleQuestion,
                                onResetBattle = academyViewModel::resetBattle,
                                onOpenChallengeInLab = { challenge ->
                                    labTutorViewModel.loadChallenge(challenge)
                                    navigateToTab(Routes.CODE_LAB)
                                }
                            )
                        }

                        composable(Routes.COMMUNITY) {
                            val profile = (profileState as? UiState.Success)?.data
                            CommunityScreen(
                                currentUserId = currentUserId,
                                profile = profile,
                                questionsState = communityQuestionsState,
                                projectsState = communityState,
                                observeReplies = academyViewModel::observeReplies,
                                onCreateQuestion = academyViewModel::createCommunityQuestion,
                                onToggleLikeQuestion = academyViewModel::toggleLikeQuestion,
                                onAddReply = academyViewModel::addQuestionReply,
                                onToggleStarProject = academyViewModel::toggleStarProject,
                                onToggleFollow = academyViewModel::toggleFollowUser,
                                onToggleBlock = academyViewModel::toggleBlockUser,
                                onReportContent = academyViewModel::reportContent,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Routes.PROFILE) {
                            ProfileScreen(
                                profileState = profileState,
                                entitlementState = entitlementState,
                                certificatesState = certificatesState,
                                communityState = communityState,
                                currentUserId = currentUserId,
                                userEmail = email,
                                isAdmin = academyViewModel.isAdmin,
                                onCompleteRegistration = academyViewModel::completeStudentRegistration,
                                onUpdateExtendedProfile = academyViewModel::updateExtendedProfile,
                                onOpenPremium = { navController.navigate(Routes.PREMIUM) },
                                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                                onOpenAdmin = { navController.navigate(Routes.ADMIN) },
                                onToggleStar = academyViewModel::toggleStarProject,
                                onSignOut = onSignOut
                            )
                        }

                        composable(Routes.PREMIUM) {
                            val profile = (profileState as? UiState.Success)?.data
                            PremiumScreen(
                                planConfigState = planConfigState,
                                entitlementState = entitlementState,
                                subscriptionsState = userSubscriptionsState,
                                transactionsState = userTransactionsState,
                                playBillingState = playBillingState,
                                razorpayState = razorpayState,
                                canUseRazorpay = razorpayCoordinator.canUseRazorpay(activePlanConfig),
                                isBackendConfigured = paymentApiClient.isBackendConfigured(),
                                billingBusy = billingBusy,
                                onBack = { navController.popBackStack() },
                                onPurchasePlay = { interval ->
                                    if (activity != null) {
                                        playBillingManager.launchSubscriptionPurchase(
                                            activity = activity,
                                            userId = currentUserId,
                                            planConfig = activePlanConfig,
                                            billingInterval = interval
                                        )
                                    }
                                },
                                onPurchaseRazorpay = { interval ->
                                    if (activity != null) {
                                        razorpayCoordinator.startSubscriptionCheckout(
                                            activity = activity,
                                            planConfig = activePlanConfig,
                                            billingInterval = interval,
                                            userEmail = email,
                                            userName = profile?.displayName ?: displayName,
                                            scope = scope
                                        )
                                    }
                                },
                                onRestorePurchases = playBillingManager::restorePurchases,
                                onCancelSubscription = academyViewModel::cancelUserSubscription,
                                onDismissBillingMessages = {
                                    playBillingManager.clearMessage()
                                    razorpayCoordinator.clearMessage()
                                }
                            )
                        }

                        composable(Routes.SETTINGS) {
                            val profile = (profileState as? UiState.Success)?.data
                            SettingsLegalScreen(
                                profile = profile,
                                userEmail = email,
                                darkThemeEnabled = prefs.darkTheme,
                                selectedLanguageCode = prefs.languageCode,
                                rememberMeEnabled = prefs.rememberMe,
                                onToggleDarkTheme = { enabled ->
                                    scope.launch { preferencesManager.setDarkTheme(enabled) }
                                },
                                onSelectLanguage = { code ->
                                    scope.launch { preferencesManager.setLanguageCode(code) }
                                    academyViewModel.updateExtendedProfile(profile?.bio.orEmpty(), code)
                                },
                                onToggleRememberMe = { enabled ->
                                    scope.launch { preferencesManager.setRememberMe(enabled) }
                                },
                                onDeleteAccount = {
                                    academyViewModel.deleteMyAccount { onSignOut() }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Routes.ADMIN) {
                            AdminDashboardScreen(
                                isAdmin = academyViewModel.isAdmin,
                                userEmail = email,
                                adminCoursesState = adminCoursesState,
                                planConfigState = planConfigState,
                                adminSubscriptionsState = adminSubscriptionsState,
                                adminTransactionsState = adminTransactionsState,
                                adminSettlementsState = adminSettlementsState,
                                moderationReportsState = moderationReportsState,
                                billingBusy = billingBusy,
                                onBack = { navController.popBackStack() },
                                onSaveCourse = academyViewModel::saveAdminCourse,
                                onDeleteCourse = academyViewModel::deleteAdminCourse,
                                onSavePlanConfig = academyViewModel::savePlanConfig,
                                onReconcileSettlements = academyViewModel::reconcileSettlements,
                                onResolveReport = academyViewModel::resolveReport,
                                onSendAnnouncement = academyViewModel::sendAnnouncement
                            )
                        }

                        composable(
                            route = Routes.LESSON,
                            arguments = listOf(
                                navArgument("trackId") { type = NavType.StringType },
                                navArgument("lessonId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val trackId = backStackEntry.arguments?.getString("trackId")
                            val lessonId = backStackEntry.arguments?.getString("lessonId")
                            val publishedCourses = (publishedCoursesState as? UiState.Success)?.data.orEmpty()
                            val liveTrack = CurriculumRepository.managedCoursesAsTrack(publishedCourses)
                            val allTracks = if (liveTrack != null) listOf(liveTrack) + CurriculumRepository.tracks else CurriculumRepository.tracks

                            val track = allTracks.firstOrNull { it.id == trackId } ?: allTracks.first()
                            val lesson = track.lessons.firstOrNull { it.id == lessonId } ?: track.lessons.first()

                            LessonPlayerScreen(
                                track = track,
                                lesson = lesson,
                                onBack = { navController.popBackStack() },
                                onAskTutor = { prompt ->
                                    labTutorViewModel.sendTutorMessage(prompt)
                                    navController.popBackStack()
                                    navigateToTab(Routes.AI_TUTOR)
                                },
                                onLessonCompleted = {
                                    academyViewModel.completeLesson(lesson, track)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
