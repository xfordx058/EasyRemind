package com.app.easyremind.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.app.easyremind.EasyRemindApp
import com.app.easyremind.focus.FocusViewModel
import com.app.easyremind.ui.components.softCard
import com.app.easyremind.ui.screens.AboutScreen
import com.app.easyremind.ui.screens.AppearanceScreen
import com.app.easyremind.ui.screens.ClassDetailsScreen
import com.app.easyremind.ui.screens.ClassEditorScreen
import com.app.easyremind.ui.screens.FocusScreen
import com.app.easyremind.ui.screens.FocusTimerSettingsScreen
import com.app.easyremind.ui.screens.HomeScreen
import com.app.easyremind.ui.screens.NotificationSettingsScreen
import com.app.easyremind.ui.screens.PermissionScreen
import com.app.easyremind.ui.screens.ScheduleScreen
import com.app.easyremind.ui.screens.ScheduleSettingsScreen
import com.app.easyremind.ui.screens.SettingsScreen
import com.app.easyremind.ui.screens.SplashScreen
import com.app.easyremind.ui.screens.WelcomeScreen
import com.app.easyremind.ui.viewmodel.ClassDetailViewModel
import com.app.easyremind.ui.viewmodel.ClassEditorViewModel
import com.app.easyremind.ui.viewmodel.HomeViewModel
import com.app.easyremind.ui.viewmodel.ScheduleViewModel
import com.app.easyremind.ui.viewmodel.SettingsViewModel
import com.app.easyremind.ui.viewmodel.ViewModelFactory

object Routes {
    const val SPLASH = "splash"
    const val WELCOME = "welcome"
    const val PERMISSION = "permission"
    const val HOME = "home"
    const val SCHEDULE = "schedule"
    const val CLASS_EDITOR = "class_editor?classId={classId}"
    const val CLASS_EDITOR_BASE = "class_editor"
    const val CLASS_DETAIL = "class/{classId}"
    const val FOCUS = "focus"
    const val SETTINGS = "settings"
    const val SETTINGS_NOTIFICATIONS = "settings/notifications"
    const val SETTINGS_SCHEDULE = "settings/schedule"
    const val SETTINGS_FOCUS = "settings/focus"
    const val SETTINGS_APPEARANCE = "settings/appearance"
    const val SETTINGS_ABOUT = "settings/about"

    fun classDetail(id: Long) = "class/$id"
    fun classEditor(id: Long = -1L) = "class_editor?classId=$id"
}

private val topLevel = setOf(
    Routes.HOME,
    Routes.SCHEDULE,
    Routes.FOCUS,
    Routes.SETTINGS,
)

@Composable
fun EasyRemindRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomNav(
                navController = navController,
                visible = topLevel.contains(currentRoute),
            )
        },
        floatingActionButton = {
            if (currentRoute == Routes.HOME) {
                AddClassFab(onClick = { navController.navigate(Routes.classEditor()) })
            }
        },
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}

@Composable
private fun BottomNav(navController: NavHostController, visible: Boolean) {
    if (!visible) return
    val items = listOf(
        NavItem(Icons.Default.Home, "Home", Routes.HOME),
        NavItem(Icons.Default.Schedule, "Schedule", Routes.SCHEDULE),
        NavItem(Icons.Default.Timer, "Focus", Routes.FOCUS),
        NavItem(Icons.Default.Settings, "Settings", Routes.SETTINGS),
    )
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        windowInsets = androidx.compose.material3.NavigationBarDefaults.windowInsets,
    ) {
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = backStackEntry?.destination
        items.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                    )
                },
                label = { Text(item.label) },
                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

private data class NavItem(val icon: ImageVector, val label: String, val route: String)

@Composable
private fun AddClassFab(onClick: () -> Unit) {
    val (dark, light) = com.app.easyremind.ui.components.neoShadowColors()
    val shape = com.app.easyremind.ui.theme.LargeShape
    Box(
        modifier = Modifier
            .size(60.dp)
            .softCard(shape, dark, light, offset = 4.dp, elevation = 10.dp)
            .background(MaterialTheme.colorScheme.primary, shape)
            .clip(shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add class",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val app = LocalContext.current.applicationContext as EasyRemindApp

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier,
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onFinished = {
                    val done = app.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                        .getBoolean("onboarding_done", false)
                    navController.navigate(
                        if (done) Routes.HOME else Routes.WELCOME,
                    ) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.WELCOME) {
            WelcomeScreen(
                onContinue = {
                    navController.navigate(Routes.PERMISSION) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                },
                onSkip = {
                    app.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit()
                        .putBoolean("onboarding_done", true).apply()
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.PERMISSION) {
            PermissionScreen(
                onFinished = {
                    app.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit()
                        .putBoolean("onboarding_done", true).apply()
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.PERMISSION) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.HOME) {
            val vm: HomeViewModel = viewModel(factory = ViewModelFactory.home)
            val state by vm.uiState.collectAsStateWithLifecycle()
            HomeScreen(
                state = state,
                onOpenSchedule = { navController.navigate(Routes.SCHEDULE) },
                onAddClass = { navController.navigate(Routes.classEditor()) },
                onOpenClass = { id -> navController.navigate(Routes.classDetail(id)) },
                onOpenNotificationSettings = { navController.navigate(Routes.SETTINGS_NOTIFICATIONS) },
            )
        }

        composable(Routes.SCHEDULE) {
            val vm: ScheduleViewModel = viewModel(factory = ViewModelFactory.schedule)
            val state by vm.uiState.collectAsStateWithLifecycle()
            ScheduleScreen(
                state = state,
                onAddClass = { navController.navigate(Routes.classEditor()) },
                onOpenClass = { id -> navController.navigate(Routes.classDetail(id)) },
                onSelectDay = { day -> vm.selectDay(day) },
                onToggleView = { grid -> vm.setViewMode(grid) },
            )
        }

        composable(
            route = Routes.CLASS_EDITOR,
            arguments = listOf(navArgument("classId") {
                type = NavType.LongType
                defaultValue = -1L
            }),
        ) { entry ->
            val classId = entry.arguments?.getLong("classId") ?: -1L
            val vm: ClassEditorViewModel = viewModel(
                key = "class_editor_$classId",
                factory = ClassEditorViewModel.factory(classId),
            )
            ClassEditorScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.CLASS_DETAIL,
            arguments = listOf(navArgument("classId") { type = NavType.LongType }),
        ) { entry ->
            val classId = entry.arguments?.getLong("classId") ?: -1L
            val vm: ClassDetailViewModel = viewModel(
                key = "class_detail_$classId",
                factory = ClassDetailViewModel.factory(classId),
            )
            ClassDetailsScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onEdit = { id ->
                    navController.navigate(Routes.classEditor(id))
                },
                onDeleted = { navController.popBackStack() },
            )
        }

        composable(Routes.FOCUS) {
            val vm: FocusViewModel = viewModel(factory = ViewModelFactory.focus)
            FocusScreen(
                viewModel = vm,
                onBackHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }

        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = viewModel(factory = ViewModelFactory.settings)
            SettingsScreen(
                viewModel = vm,
                onOpenNotifications = { navController.navigate(Routes.SETTINGS_NOTIFICATIONS) },
                onOpenScheduleSettings = { navController.navigate(Routes.SETTINGS_SCHEDULE) },
                onOpenFocusSettings = { navController.navigate(Routes.SETTINGS_FOCUS) },
                onOpenAppearance = { navController.navigate(Routes.SETTINGS_APPEARANCE) },
                onOpenAbout = { navController.navigate(Routes.SETTINGS_ABOUT) },
            )
        }

        composable(Routes.SETTINGS_NOTIFICATIONS) {
            val vm: SettingsViewModel = viewModel()
            NotificationSettingsScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS_SCHEDULE) {
            val vm: SettingsViewModel = viewModel()
            ScheduleSettingsScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS_FOCUS) {
            val vm: SettingsViewModel = viewModel()
            FocusTimerSettingsScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS_APPEARANCE) {
            val vm: SettingsViewModel = viewModel()
            AppearanceScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS_ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}