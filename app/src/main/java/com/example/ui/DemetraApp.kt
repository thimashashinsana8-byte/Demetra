package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.UserSettings
import com.example.i18n.LocalizedCopy
import com.example.i18n.stringsFor
import com.example.model.RedistributionUiState
import com.example.model.UserRole
import com.example.ui.components.DemetraTopHeader
import com.example.ui.components.FloatingPipFab
import com.example.ui.components.PipDonationAssistant
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.DemetraViewModel

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val ROLE_SELECT = "role_select"
    const val AUTH = "auth"
    const val AUTH_LOGIN = "auth_login"
    const val VOLUNTEER_REGISTRATION = "volunteer_registration"
    const val MAIN_HOME = "main_home"
    const val CREATE_DONATION = "create_donation"
    const val DONATION_DETAIL = "donation_detail/{id}"
    const val VOLUNTEER_TASK = "volunteer_task/{id}"
    const val RECIPIENT_FOOD = "recipient_food/{id}"
    const val NOTIFICATIONS = "notifications"

    fun donationDetail(id: String) = "donation_detail/$id"
    fun volunteerTask(id: String) = "volunteer_task/$id"
    fun recipientFood(id: String) = "recipient_food/$id"
}

private data class MainTabItem(
    val key: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun DemetraApp(viewModel: DemetraViewModel) {
    val navController = rememberNavController()
    val role by viewModel.currentRole.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var activeTabKey by remember(role) { mutableStateOf("home") }
    val copy = remember(settings.language) { stringsFor(settings.language) }

    val onExploreGarden = remember(navController) {
        {
            activeTabKey = "garden"
            navController.popBackStack(Routes.MAIN_HOME, inclusive = false)
            Unit
        }
    }
    val onOpenCreateDonation = remember(navController) {
        { navController.navigate(Routes.CREATE_DONATION) }
    }
    val onOpenVolunteerRegistration = remember(navController, viewModel) {
        {
            viewModel.resetVolunteerRegistrationState()
            navController.navigate(Routes.VOLUNTEER_REGISTRATION)
        }
    }
    val onOpenDonationDetail: (String) -> Unit = remember(navController) {
        { id -> navController.navigate(Routes.donationDetail(id)) }
    }
    val onOpenVolunteerTask: (String) -> Unit = remember(navController) {
        { id -> navController.navigate(Routes.volunteerTask(id)) }
    }
    val onOpenRecipientFood: (String) -> Unit = remember(navController) {
        { id -> navController.navigate(Routes.recipientFood(id)) }
    }
    val onOpenNotifications = remember(navController) {
        { navController.navigate(Routes.NOTIFICATIONS) }
    }
    val onSelectTab: (String) -> Unit = remember {
        { tab -> activeTabKey = tab }
    }

    FlowerCelebrationHost(
        viewModel = viewModel,
        reducedMotion = settings.reducedMotion,
        onExploreGarden = onExploreGarden
    )

    PipAssistantHost(
        viewModel = viewModel,
        role = role,
        askHint = copy.askPipHint,
        reducedMotion = settings.reducedMotion,
        onQuickActionNavigate = { actionRoute ->
            when (actionRoute) {
                "create_donation" -> onOpenCreateDonation()
                "harvest_garden" -> onExploreGarden()
                "volunteer_explore", "recipient_browse" -> {
                    activeTabKey = "explore"
                    navController.popBackStack(Routes.MAIN_HOME, inclusive = false)
                }
            }
        }
    )

    Box(modifier = Modifier.fillMaxSize().background(WarmCream)) {
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            enterTransition = {
                if (settings.reducedMotion) EnterTransition.None
                else fadeIn(tween(180)) + slideInHorizontally(tween(180)) { it / 18 }
            },
            exitTransition = {
                if (settings.reducedMotion) ExitTransition.None
                else fadeOut(tween(150))
            },
            popEnterTransition = {
                if (settings.reducedMotion) EnterTransition.None
                else fadeIn(tween(180)) + slideInHorizontally(tween(180)) { -it / 18 }
            },
            popExitTransition = {
                if (settings.reducedMotion) ExitTransition.None
                else fadeOut(tween(150)) + slideOutHorizontally(tween(150)) { it / 18 }
            },
            modifier = Modifier.fillMaxSize().background(WarmCream)
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(
                    language = settings.language,
                    onContinue = {
                        navController.navigate(Routes.ONBOARDING) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.ONBOARDING) {
                WelcomeOnboardingScreen(
                    language = settings.language,
                    reducedMotion = settings.reducedMotion,
                    onLanguageSelected = viewModel::setLanguage,
                    onFinished = {
                        navController.navigate(Routes.ROLE_SELECT) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.ROLE_SELECT) {
                RoleSelectionScreen(
                    selectedRole = role,
                    reducedMotion = settings.reducedMotion,
                    onRolePicked = viewModel::selectRole,
                    onContinue = {
                        if (role == UserRole.VOLUNTEER) {
                            viewModel.resetVolunteerRegistrationState()
                            navController.navigate(Routes.VOLUNTEER_REGISTRATION)
                        } else {
                            navController.navigate(Routes.AUTH)
                        }
                    }
                )
            }

            composable(Routes.AUTH) {
                AuthAndProfileScreen(
                    role = role,
                    reducedMotion = settings.reducedMotion,
                    initialSignUp = true,
                    onBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Routes.ROLE_SELECT) {
                                popUpTo(Routes.AUTH) { inclusive = true }
                            }
                        }
                    },
                    onComplete = { fullName, org, district, phone, note, firebaseUser ->
                        viewModel.completeAuthAndProfile(fullName, org, district, phone, note, firebaseUser)
                        activeTabKey = "home"
                        navController.navigate(Routes.MAIN_HOME) {
                            popUpTo(Routes.ROLE_SELECT) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.AUTH_LOGIN) {
                AuthAndProfileScreen(
                    role = role,
                    reducedMotion = settings.reducedMotion,
                    initialSignUp = false,
                    onBack = {
                        navController.navigate(Routes.ROLE_SELECT) {
                            popUpTo(Routes.AUTH_LOGIN) { inclusive = true }
                        }
                    },
                    onComplete = { fullName, org, district, phone, note, firebaseUser ->
                        viewModel.completeAuthAndProfile(fullName, org, district, phone, note, firebaseUser)
                        activeTabKey = "home"
                        navController.navigate(Routes.MAIN_HOME) {
                            popUpTo(Routes.AUTH_LOGIN) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.VOLUNTEER_REGISTRATION) {
                VolunteerRegistrationHost(
                    viewModel = viewModel,
                    reducedMotion = settings.reducedMotion,
                    onBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Routes.ROLE_SELECT) {
                                popUpTo(Routes.VOLUNTEER_REGISTRATION) { inclusive = true }
                            }
                        }
                    },
                    onDoneNavigateHome = {
                        activeTabKey = "home"
                        val popped = navController.popBackStack(Routes.MAIN_HOME, inclusive = false)
                        if (!popped) {
                            navController.navigate(Routes.MAIN_HOME) {
                                popUpTo(Routes.ROLE_SELECT) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(Routes.MAIN_HOME) {
                MainHomeHost(
                    viewModel = viewModel,
                    role = role,
                    settings = settings,
                    copy = copy,
                    activeTabKey = activeTabKey,
                    onSelectTab = onSelectTab,
                    onOpenCreateDonation = onOpenCreateDonation,
                    onOpenVolunteerRegistration = onOpenVolunteerRegistration,
                    onOpenDonationDetail = onOpenDonationDetail,
                    onOpenVolunteerTask = onOpenVolunteerTask,
                    onOpenRecipientFood = onOpenRecipientFood,
                    onOpenNotifications = onOpenNotifications,
                    onLoggedOut = {
                        viewModel.logout()
                        activeTabKey = "home"
                        navController.navigate(Routes.AUTH_LOGIN) {
                            popUpTo(Routes.MAIN_HOME) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.CREATE_DONATION) {
                CreateDonationHost(
                    viewModel = viewModel,
                    reducedMotion = settings.reducedMotion,
                    onBack = { navController.popBackStack() },
                    onCreated = { createdId ->
                        navController.navigate(Routes.donationDetail(createdId)) {
                            popUpTo(Routes.CREATE_DONATION) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Routes.DONATION_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: return@composable
                DonationDetailHost(
                    id = id,
                    viewModel = viewModel,
                    reducedMotion = settings.reducedMotion,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.VOLUNTEER_TASK,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: return@composable
                VolunteerTaskHost(
                    id = id,
                    viewModel = viewModel,
                    reducedMotion = settings.reducedMotion,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.RECIPIENT_FOOD,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: return@composable
                RecipientFoodHost(
                    id = id,
                    viewModel = viewModel,
                    reducedMotion = settings.reducedMotion,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.NOTIFICATIONS) {
                NotificationsHost(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        PipDonationAssistant(
            viewModel = viewModel,
            reducedMotion = settings.reducedMotion,
            onNavigateGarden = onExploreGarden,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun MainHomeHost(
    viewModel: DemetraViewModel,
    role: UserRole,
    settings: UserSettings,
    copy: LocalizedCopy,
    activeTabKey: String,
    onSelectTab: (String) -> Unit,
    onOpenCreateDonation: () -> Unit,
    onOpenVolunteerRegistration: () -> Unit,
    onOpenDonationDetail: (String) -> Unit,
    onOpenVolunteerTask: (String) -> Unit,
    onOpenRecipientFood: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val authUid by viewModel.authUid.collectAsStateWithLifecycle()

    key(authUid) {
        val tabs = remember(role, copy) {
            when (role) {
                UserRole.DONOR -> listOf(
                    MainTabItem("home", copy.navHome, Icons.Filled.Home, Icons.Outlined.Home),
                    MainTabItem("garden", copy.navGardenOrRequests, Icons.Filled.LocalFlorist, Icons.Outlined.LocalFlorist),
                    MainTabItem("impact", copy.navImpactOrHistory, Icons.Filled.Insights, Icons.Outlined.Insights),
                    MainTabItem("settings", copy.navSettings, Icons.Filled.Settings, Icons.Outlined.Settings)
                )
                UserRole.VOLUNTEER -> listOf(
                    MainTabItem("home", copy.navHome, Icons.Filled.Home, Icons.Outlined.Home),
                    MainTabItem("explore", copy.navDonateOrExplore, Icons.Filled.Explore, Icons.Outlined.Explore),
                    MainTabItem("garden", copy.navGardenOrRequests, Icons.Filled.LocalFlorist, Icons.Outlined.LocalFlorist),
                    MainTabItem("impact", copy.navImpactOrHistory, Icons.Filled.Insights, Icons.Outlined.Insights),
                    MainTabItem("settings", copy.navSettings, Icons.Filled.Settings, Icons.Outlined.Settings)
                )
                UserRole.RECIPIENT -> listOf(
                    MainTabItem("home", copy.navHome, Icons.Filled.Home, Icons.Outlined.Home),
                    MainTabItem("explore", "Food", Icons.Filled.Restaurant, Icons.Outlined.Restaurant),
                    MainTabItem("impact", "Requests", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
                    MainTabItem("settings", copy.navSettings, Icons.Filled.Settings, Icons.Outlined.Settings)
                )
            }
        }

        if (activeTabKey != "home") {
            BackHandler { onSelectTab("home") }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(WarmCream)
        ) {
            val useNavRail = maxWidth >= 600.dp

            Row(modifier = Modifier.fillMaxSize()) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = SoftWhite,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        tabs.forEach { tab ->
                            val selected = activeTabKey == tab.key
                            NavigationRailItem(
                                selected = selected,
                                onClick = { onSelectTab(tab.key) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ForestGreen,
                                    selectedTextColor = ForestGreen,
                                    indicatorColor = SageTint,
                                    unselectedIconColor = MutedText,
                                    unselectedTextColor = MutedText
                                )
                            )
                        }
                    }
                }

                Scaffold(
                    containerColor = WarmCream,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    bottomBar = {
                        if (!useNavRail) {
                            NavigationBar(
                                containerColor = SoftWhite,
                                tonalElevation = 2.dp
                            ) {
                                tabs.forEach { tab ->
                                    val selected = activeTabKey == tab.key
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { onSelectTab(tab.key) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        },
                                        label = { Text(tab.label, maxLines = 1) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = ForestGreen,
                                            selectedTextColor = ForestGreen,
                                            indicatorColor = SageTint,
                                            unselectedIconColor = MutedText,
                                            unselectedTextColor = MutedText
                                        ),
                                        modifier = Modifier.testTag("nav_tab_${tab.key}")
                                    )
                                }
                            }
                        }
                    },
                    floatingActionButton = {
                        FloatingPipFab(
                            onClick = viewModel::openPip,
                            reducedMotion = settings.reducedMotion
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 680.dp)
                        ) {
                            MainTopHeaderHost(
                                viewModel = viewModel,
                                role = role,
                                copy = copy,
                                activeTabKey = activeTabKey,
                                onNotificationsClick = onOpenNotifications,
                                onRoleBadgeClick = { onSelectTab("settings") }
                            )

                            Box(modifier = Modifier.weight(1f)) {
                                val saveableStateHolder = rememberSaveableStateHolder()
                                val tabStateKey = remember(role, activeTabKey) { "${role.name}_$activeTabKey" }
                                Crossfade(
                                    targetState = tabStateKey,
                                    animationSpec = tween(if (settings.reducedMotion) 0 else 120),
                                    label = "main_tab_crossfade"
                                ) { stateKey ->
                                    val splitIdx = stateKey.indexOf('_')
                                    val currentRole = UserRole.valueOf(stateKey.substring(0, splitIdx))
                                    val currentTab = stateKey.substring(splitIdx + 1)
                                    saveableStateHolder.SaveableStateProvider(stateKey) {
                                        when (currentRole) {
                                            UserRole.DONOR -> when (currentTab) {
                                                "home" -> DonorHomeTabHost(
                                                    viewModel = viewModel,
                                                    settings = settings,
                                                    onCreateDonationClick = onOpenCreateDonation,
                                                    onDonationClick = onOpenDonationDetail,
                                                    onOpenGarden = { onSelectTab("garden") }
                                                )
                                                "garden" -> DonorGardenTabHost(
                                                    viewModel = viewModel,
                                                    settings = settings
                                                )
                                                "impact" -> DonorImpactTabHost(
                                                    viewModel = viewModel,
                                                    lowDataMode = settings.lowDataMode,
                                                    onDonationClick = onOpenDonationDetail
                                                )
                                                else -> SettingsTabHost(
                                                    viewModel = viewModel,
                                                    role = currentRole,
                                                    settings = settings,
                                                    onOpenVolunteerRegistration = onOpenVolunteerRegistration,
                                                    onLoggedOut = onLoggedOut
                                                )
                                            }

                                            UserRole.VOLUNTEER -> when (currentTab) {
                                                "home" -> VolunteerHomeTabHost(
                                                    viewModel = viewModel,
                                                    settings = settings,
                                                    onExploreClick = { onSelectTab("explore") },
                                                    onTaskClick = onOpenVolunteerTask,
                                                    onOpenGarden = { onSelectTab("garden") },
                                                    onOpenVolunteerRegistration = onOpenVolunteerRegistration
                                                )
                                                "explore" -> VolunteerExploreTabHost(
                                                    viewModel = viewModel,
                                                    settings = settings,
                                                    onTaskClick = onOpenVolunteerTask
                                                )
                                                "garden" -> VolunteerGardenTabHost(
                                                    viewModel = viewModel,
                                                    settings = settings
                                                )
                                                "impact" -> VolunteerImpactTabHost(
                                                    viewModel = viewModel,
                                                    lowDataMode = settings.lowDataMode,
                                                    onTaskClick = onOpenVolunteerTask
                                                )
                                                else -> SettingsTabHost(
                                                    viewModel = viewModel,
                                                    role = currentRole,
                                                    settings = settings,
                                                    onOpenVolunteerRegistration = onOpenVolunteerRegistration,
                                                    onLoggedOut = onLoggedOut
                                                )
                                            }

                                            UserRole.RECIPIENT -> when (currentTab) {
                                                "home" -> RecipientHomeTabHost(
                                                    viewModel = viewModel,
                                                    settings = settings,
                                                    onBrowseFoodClick = { onSelectTab("explore") },
                                                    onFoodClick = onOpenRecipientFood
                                                )
                                                "explore" -> RecipientExploreTabHost(
                                                    viewModel = viewModel,
                                                    settings = settings,
                                                    onFoodClick = onOpenRecipientFood
                                                )
                                                "impact" -> RecipientImpactTabHost(
                                                    viewModel = viewModel,
                                                    settings = settings,
                                                    onFoodClick = onOpenRecipientFood
                                                )
                                                else -> SettingsTabHost(
                                                    viewModel = viewModel,
                                                    role = currentRole,
                                                    settings = settings,
                                                    onOpenVolunteerRegistration = onOpenVolunteerRegistration,
                                                    onLoggedOut = onLoggedOut
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
    }
}

@Composable
private fun MainTopHeaderHost(
    viewModel: DemetraViewModel,
    role: UserRole,
    copy: LocalizedCopy,
    activeTabKey: String,
    onNotificationsClick: () -> Unit,
    onRoleBadgeClick: () -> Unit
) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val unreadCount by remember { derivedStateOf { notifications.count { !it.isRead } } }

    val screenTitle = remember(activeTabKey, profile.fullName, role, copy) {
        when (activeTabKey) {
            "home" -> "Hi, ${profile.fullName.substringBefore(" ")}"
            "explore" -> if (role == UserRole.VOLUNTEER) "Nearby Pickups" else copy.availableFoodTitle
            "garden" -> copy.harvestGardenTitle
            "impact" -> when (role) {
                UserRole.DONOR -> "Impact & History"
                UserRole.VOLUNTEER -> "Volunteer Impact"
                UserRole.RECIPIENT -> "Your Requests"
            }
            else -> "Settings & Persona"
        }
    }

    LaunchedEffect(role, activeTabKey, screenTitle) {
        viewModel.updateCurrentScreen("${role.title} $screenTitle")
    }

    DemetraTopHeader(
        title = screenTitle,
        subtitle = profile.district,
        role = role,
        unreadNotificationsCount = unreadCount,
        onNotificationsClick = onNotificationsClick,
        onRoleBadgeClick = onRoleBadgeClick
    )
}

@Composable
private fun DonorHomeTabHost(
    viewModel: DemetraViewModel,
    settings: UserSettings,
    onCreateDonationClick: () -> Unit,
    onDonationClick: (String) -> Unit,
    onOpenGarden: () -> Unit
) {
    val donations by viewModel.donations.collectAsStateWithLifecycle()
    val donorImpact by viewModel.donorImpact.collectAsStateWithLifecycle()
    val donorGarden by viewModel.donorGarden.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val selectedMapDistricts by viewModel.selectedMapDistricts.collectAsStateWithLifecycle()

    DonorDashboardScreen(
        donations = donations,
        impact = donorImpact,
        garden = donorGarden,
        userDistrict = profile.district,
        selectedMapDistricts = selectedMapDistricts,
        language = settings.language,
        lowDataMode = settings.lowDataMode,
        reducedMotion = settings.reducedMotion,
        onToggleMapDistrict = viewModel::toggleMapDistrict,
        onApplyMapDistricts = viewModel::setMapDistricts,
        onClearMapDistricts = viewModel::clearMapDistrictFilters,
        onCreateDonationClick = onCreateDonationClick,
        onDonationClick = onDonationClick,
        onOpenGarden = onOpenGarden
    )
}

@Composable
private fun DonorGardenTabHost(
    viewModel: DemetraViewModel,
    settings: UserSettings
) {
    val donorGarden by viewModel.donorGarden.collectAsStateWithLifecycle()
    val communityGrove by viewModel.communityGrove.collectAsStateWithLifecycle()
    val donorImpact by viewModel.donorImpact.collectAsStateWithLifecycle()

    HarvestGardenScreen(
        role = UserRole.DONOR,
        gardenState = donorGarden,
        communityGrove = communityGrove,
        impactSummary = donorImpact,
        language = settings.language,
        reducedMotion = settings.reducedMotion,
        onSelectTheme = viewModel::selectGardenTheme
    )
}

@Composable
private fun DonorImpactTabHost(
    viewModel: DemetraViewModel,
    lowDataMode: Boolean,
    onDonationClick: (String) -> Unit
) {
    val donations by viewModel.donations.collectAsStateWithLifecycle()
    val donorImpact by viewModel.donorImpact.collectAsStateWithLifecycle()

    DonorImpactHistoryScreen(
        donations = donations,
        impact = donorImpact,
        lowDataMode = lowDataMode,
        onDonationClick = onDonationClick
    )
}

@Composable
private fun VolunteerHomeTabHost(
    viewModel: DemetraViewModel,
    settings: UserSettings,
    onExploreClick: () -> Unit,
    onTaskClick: (String) -> Unit,
    onOpenGarden: () -> Unit,
    onOpenVolunteerRegistration: () -> Unit
) {
    val donations by viewModel.donations.collectAsStateWithLifecycle()
    val volunteerImpact by viewModel.volunteerImpact.collectAsStateWithLifecycle()
    val volunteerGarden by viewModel.volunteerGarden.collectAsStateWithLifecycle()
    val myVolunteerState by viewModel.myVolunteerProfile.collectAsStateWithLifecycle()
    val registeredVolunteer = (myVolunteerState as? RedistributionUiState.Success)?.data

    VolunteerDashboardScreen(
        donations = donations,
        impact = volunteerImpact,
        garden = volunteerGarden,
        registeredVolunteer = registeredVolunteer,
        lowDataMode = settings.lowDataMode,
        reducedMotion = settings.reducedMotion,
        onExploreClick = onExploreClick,
        onTaskClick = onTaskClick,
        onOpenGarden = onOpenGarden,
        onOpenVolunteerRegistration = onOpenVolunteerRegistration
    )
}

@Composable
private fun VolunteerExploreTabHost(
    viewModel: DemetraViewModel,
    settings: UserSettings,
    onTaskClick: (String) -> Unit
) {
    val donations by viewModel.donations.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val selectedMapDistricts by viewModel.selectedMapDistricts.collectAsStateWithLifecycle()

    VolunteerExploreScreen(
        donations = donations,
        userDistrict = profile.district,
        selectedMapDistricts = selectedMapDistricts,
        lowDataMode = settings.lowDataMode,
        reducedMotion = settings.reducedMotion,
        onToggleMapDistrict = viewModel::toggleMapDistrict,
        onApplyMapDistricts = viewModel::setMapDistricts,
        onClearMapDistricts = viewModel::clearMapDistrictFilters,
        onTaskClick = onTaskClick
    )
}

@Composable
private fun VolunteerGardenTabHost(
    viewModel: DemetraViewModel,
    settings: UserSettings
) {
    val volunteerGarden by viewModel.volunteerGarden.collectAsStateWithLifecycle()
    val communityGrove by viewModel.communityGrove.collectAsStateWithLifecycle()
    val volunteerImpact by viewModel.volunteerImpact.collectAsStateWithLifecycle()

    HarvestGardenScreen(
        role = UserRole.VOLUNTEER,
        gardenState = volunteerGarden,
        communityGrove = communityGrove,
        impactSummary = volunteerImpact,
        language = settings.language,
        reducedMotion = settings.reducedMotion,
        onSelectTheme = viewModel::selectGardenTheme
    )
}

@Composable
private fun VolunteerImpactTabHost(
    viewModel: DemetraViewModel,
    lowDataMode: Boolean,
    onTaskClick: (String) -> Unit
) {
    val donations by viewModel.donations.collectAsStateWithLifecycle()
    val volunteerImpact by viewModel.volunteerImpact.collectAsStateWithLifecycle()

    VolunteerImpactHistoryScreen(
        donations = donations,
        impact = volunteerImpact,
        lowDataMode = lowDataMode,
        onTaskClick = onTaskClick
    )
}

@Composable
private fun RecipientHomeTabHost(
    viewModel: DemetraViewModel,
    settings: UserSettings,
    onBrowseFoodClick: () -> Unit,
    onFoodClick: (String) -> Unit
) {
    val donations by viewModel.donations.collectAsStateWithLifecycle()

    RecipientDashboardScreen(
        donations = donations,
        language = settings.language,
        lowDataMode = settings.lowDataMode,
        reducedMotion = settings.reducedMotion,
        onBrowseFoodClick = onBrowseFoodClick,
        onFoodClick = onFoodClick
    )
}

@Composable
private fun RecipientExploreTabHost(
    viewModel: DemetraViewModel,
    settings: UserSettings,
    onFoodClick: (String) -> Unit
) {
    val donations by viewModel.donations.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val selectedMapDistricts by viewModel.selectedMapDistricts.collectAsStateWithLifecycle()

    RecipientAvailableFoodScreen(
        donations = donations,
        userDistrict = profile.district,
        selectedMapDistricts = selectedMapDistricts,
        lowDataMode = settings.lowDataMode,
        reducedMotion = settings.reducedMotion,
        onToggleMapDistrict = viewModel::toggleMapDistrict,
        onApplyMapDistricts = viewModel::setMapDistricts,
        onClearMapDistricts = viewModel::clearMapDistrictFilters,
        onFoodClick = onFoodClick
    )
}

@Composable
private fun RecipientImpactTabHost(
    viewModel: DemetraViewModel,
    settings: UserSettings,
    onFoodClick: (String) -> Unit
) {
    val donations by viewModel.donations.collectAsStateWithLifecycle()

    RecipientRequestsHistoryScreen(
        donations = donations,
        lowDataMode = settings.lowDataMode,
        reducedMotion = settings.reducedMotion,
        onFoodClick = onFoodClick
    )
}

@Composable
private fun SettingsTabHost(
    viewModel: DemetraViewModel,
    role: UserRole,
    settings: UserSettings,
    onOpenVolunteerRegistration: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()

    SettingsScreen(
        role = role,
        profile = profile,
        settings = settings,
        onRoleSwitch = viewModel::selectRole,
        onDistrictChange = viewModel::updateDistrict,
        onLanguageChange = viewModel::setLanguage,
        onReducedMotionChange = viewModel::setReducedMotion,
        onLowDataModeChange = viewModel::setLowDataMode,
        onOpenVolunteerRegistration = if (role == UserRole.VOLUNTEER) onOpenVolunteerRegistration else null,
        onLogout = onLoggedOut
    )
}

@Composable
private fun VolunteerRegistrationHost(
    viewModel: DemetraViewModel,
    reducedMotion: Boolean,
    onBack: () -> Unit,
    onDoneNavigateHome: () -> Unit
) {
    val myVolunteerState by viewModel.myVolunteerProfile.collectAsStateWithLifecycle()
    val existingVolunteer = (myVolunteerState as? RedistributionUiState.Success)?.data
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val registrationState by viewModel.volunteerRegistrationState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.updateCurrentScreen("Volunteer Registration") }

    VolunteerRegistrationScreen(
        existingVolunteer = existingVolunteer,
        userProfile = userProfile,
        registrationState = registrationState,
        reducedMotion = reducedMotion,
        onBack = onBack,
        onResetState = viewModel::resetVolunteerRegistrationState,
        onSubmitRegistration = viewModel::registerVolunteer,
        onDoneNavigateHome = onDoneNavigateHome
    )
}

@Composable
private fun CreateDonationHost(
    viewModel: DemetraViewModel,
    reducedMotion: Boolean,
    onBack: () -> Unit,
    onCreated: (String) -> Unit
) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.updateCurrentScreen("Create Food Donation") }
    CreateDonationWizardScreen(
        defaultDistrict = profile.district,
        reducedMotion = reducedMotion,
        onBack = onBack,
        onSubmitDonation = { title, cat, kg, servings, loc, dist, win, exp, tags, uri ->
            val created = viewModel.createDonation(
                title = title,
                category = cat,
                quantityKg = kg,
                servingsEstimate = servings,
                locationName = loc,
                district = dist,
                pickupWindow = win,
                expiryNote = exp,
                dietaryTags = tags,
                pickedPhotoUri = uri
            )
            onCreated(created.id)
        }
    )
}

@Composable
private fun DonationDetailHost(
    id: String,
    viewModel: DemetraViewModel,
    reducedMotion: Boolean,
    onBack: () -> Unit
) {
    val donations by viewModel.donations.collectAsStateWithLifecycle()
    val item = remember(donations, id) { donations.find { it.id == id } } ?: return
    LaunchedEffect(id, item.title) { viewModel.updateCurrentScreen("Donation Tracking (${item.title})") }

    DonationDetailTrackingScreen(
        donation = item,
        reducedMotion = reducedMotion,
        onBack = onBack,
        onAssignVolunteer = { viewModel.assignSuggestedVolunteer(id) },
        onVerifyHandover = { viewModel.verifyDonorHandover(id) }
    )
}

@Composable
private fun VolunteerTaskHost(
    id: String,
    viewModel: DemetraViewModel,
    reducedMotion: Boolean,
    onBack: () -> Unit
) {
    val donations by viewModel.donations.collectAsStateWithLifecycle()
    val item = remember(donations, id) { donations.find { it.id == id } } ?: return
    LaunchedEffect(id, item.title) { viewModel.updateCurrentScreen("Volunteer Collection (${item.title})") }

    VolunteerTaskDetailScreen(
        donation = item,
        reducedMotion = reducedMotion,
        onBack = onBack,
        onAcceptTask = { viewModel.acceptVolunteerTask(id) },
        onVerifyPickup = { viewModel.verifyPickup(id) },
        onVerifyDelivery = { viewModel.verifyVolunteerDelivery(id) }
    )
}

@Composable
private fun RecipientFoodHost(
    id: String,
    viewModel: DemetraViewModel,
    reducedMotion: Boolean,
    onBack: () -> Unit
) {
    val donations by viewModel.donations.collectAsStateWithLifecycle()
    val item = remember(donations, id) { donations.find { it.id == id } } ?: return
    LaunchedEffect(id, item.title) { viewModel.updateCurrentScreen("Available Food (${item.title})") }

    RecipientFoodDetailScreen(
        donation = item,
        reducedMotion = reducedMotion,
        onBack = onBack,
        onRequestFood = { portions -> viewModel.requestFoodAsRecipient(id, portions) },
        onConfirmReceipt = { viewModel.confirmRecipientReceipt(id) }
    )
}

@Composable
private fun NotificationsHost(
    viewModel: DemetraViewModel,
    onBack: () -> Unit
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.updateCurrentScreen("Notifications") }
    NotificationsScreen(
        notifications = notifications,
        onBack = onBack,
        onMarkAllRead = viewModel::markAllNotificationsRead
    )
}

@Composable
private fun FlowerCelebrationHost(
    viewModel: DemetraViewModel,
    reducedMotion: Boolean,
    onExploreGarden: () -> Unit
) {
    val latestFlowerEvent by viewModel.latestFlowerEvent.collectAsStateWithLifecycle()
    latestFlowerEvent?.let { event ->
        FlowerGrowthCelebrationDialog(
            event = event,
            reducedMotion = reducedMotion,
            onDismiss = viewModel::clearFlowerEvent,
            onExploreGarden = onExploreGarden
        )
    }
}

@Composable
private fun PipAssistantHost(
    viewModel: DemetraViewModel,
    role: UserRole,
    askHint: String,
    reducedMotion: Boolean,
    onQuickActionNavigate: (String) -> Unit
) {
    val isPipOpen by viewModel.isPipOpen.collectAsStateWithLifecycle()
    if (!isPipOpen) return

    val pipMessages by viewModel.pipMessages.collectAsStateWithLifecycle()
    val pipUiState by viewModel.pipUiState.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreenContext.collectAsStateWithLifecycle()
    val pipSuggestions = remember(role, currentScreen) { viewModel.contextualSuggestions() }

    PipAssistantSheet(
        role = role,
        currentScreen = currentScreen,
        messages = pipMessages,
        suggestions = pipSuggestions,
        uiState = pipUiState,
        askHint = askHint,
        reducedMotion = reducedMotion,
        onDismiss = viewModel::closePip,
        onSendQuery = viewModel::askPip,
        onDismissError = viewModel::dismissPipError,
        onQuickActionNavigate = onQuickActionNavigate
    )
}
