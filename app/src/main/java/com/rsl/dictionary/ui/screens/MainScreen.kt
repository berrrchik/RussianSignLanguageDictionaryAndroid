package com.rsl.dictionary.ui.screens

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavHostController
import com.rsl.dictionary.R
import com.rsl.dictionary.ui.components.StartupSplashScreen
import com.rsl.dictionary.ui.navigation.BottomTab
import com.rsl.dictionary.ui.navigation.Screen
import com.rsl.dictionary.viewmodels.StartupViewModel

private const val DetailEnterDurationMs = 280
private const val DetailExitDurationMs = 320
private const val StartupSplashExitDurationMs = 450

private fun String?.isDetailRoute(): Boolean {
    if (this == null) return false

    return startsWith("category_detail") ||
        startsWith("sign_detail") ||
        startsWith("lesson_detail")
}

@Composable
fun MainScreen(
    startupViewModel: StartupViewModel
) {
    val navController = rememberNavController()
    val isPreparing by startupViewModel.isPreparing.collectAsStateWithLifecycle()
    val startupError by startupViewModel.startupError.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (!isPreparing) {
                    RslBottomNavBar(navController)
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Search.route,
                    enterTransition = {
                        if (targetState.destination.route.isDetailRoute()) {
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = tween(
                                    durationMillis = DetailEnterDurationMs,
                                    easing = LinearOutSlowInEasing
                                )
                            )
                        } else {
                            EnterTransition.None
                        }
                    },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = {
                        if (initialState.destination.route.isDetailRoute()) {
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween(
                                    durationMillis = DetailExitDurationMs,
                                    easing = FastOutSlowInEasing
                                )
                            )
                        } else {
                            ExitTransition.None
                        }
                    }
                ) {
                    composable(Screen.Search.route) { SearchScreen(navController = navController) }
                    composable(Screen.Favorites.route) { FavoritesScreen(navController = navController) }
                    composable(Screen.Categories.route) {
                        CategoriesScreen(navController = navController)
                    }
                    composable(
                        Screen.CategoryDetail.route,
                        arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        CategoryDetailScreen(
                            categoryId = backStackEntry.arguments?.getString("categoryId") ?: "",
                            navController = navController
                        )
                    }
                    composable(
                        Screen.SignDetail.route,
                        arguments = listOf(
                            navArgument("signId") { type = NavType.StringType },
                            navArgument("visitedSignIds") {
                                type = NavType.StringType
                                defaultValue = "[]"
                            }
                        )
                    ) { backStackEntry ->
                        SignDetailScreen(
                            signId = backStackEntry.arguments?.getString("signId") ?: "",
                            navController = navController
                        )
                    }
                    composable(Screen.Lessons.route) {
                        LessonsScreen(navController = navController)
                    }
                    composable(
                        Screen.LessonDetail.route,
                        arguments = listOf(navArgument("lessonId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        LessonDetailScreen(
                            lessonId = backStackEntry.arguments?.getString("lessonId") ?: "",
                            navController = navController
                        )
                    }
                    composable(Screen.Settings.route) { SettingsScreen() }
                }
            }
        }

        AnimatedVisibility(
            visible = isPreparing,
            exit = fadeOut(
                animationSpec = tween(
                    durationMillis = StartupSplashExitDurationMs,
                    easing = LinearOutSlowInEasing
                )
            ) + scaleOut(
                targetScale = 0.985f,
                animationSpec = tween(
                    durationMillis = StartupSplashExitDurationMs,
                    easing = FastOutSlowInEasing
                )
            )
        ) {
            StartupSplashScreen()
        }

        startupError?.let { errorMessage ->
            AlertDialog(
                onDismissRequest = { startupViewModel.clearError() },
                title = { Text(stringResource(R.string.sync_error_title)) },
                text = {
                    Text(
                        text = buildString {
                            append(errorMessage)
                            append("\n\n")
                            append(stringResource(R.string.startup_error_help))
                        }
                    )
                },
                confirmButton = {
                    TextButton(onClick = { startupViewModel.retry() }) {
                        Text(stringResource(R.string.retry))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { startupViewModel.clearError() }) {
                        Text(stringResource(R.string.ok))
                    }
                }
            )
        }
    }
}

@Composable
private fun RslBottomNavBar(navController: NavHostController) {
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        BottomTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.screen.route,
                onClick = {
                    navController.navigate(tab.screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = {
                    Text(
                        text = tab.label,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        fontSize = 10.sp,
                        lineHeight = 12.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    unselectedIconColor = MaterialTheme.colorScheme.secondary,
                    unselectedTextColor = MaterialTheme.colorScheme.secondary
                )
            )
        }
    }
}
