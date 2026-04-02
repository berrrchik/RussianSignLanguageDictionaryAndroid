package com.rsl.dictionary.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed class Screen(val route: String) {
    object Search : Screen("search")
    object Favorites : Screen("favorites")
    object Categories : Screen("categories")
    object CategoryDetail : Screen("category_detail/{categoryId}") {
        fun createRoute(categoryId: String) = "category_detail/$categoryId"
    }

    object SignDetail : Screen("sign_detail/{signId}?visitedSignIds={visitedSignIds}") {
        fun createRoute(
            signId: String,
            visitedSignIds: Set<String> = emptySet()
        ): String {
            val encodedVisitedSignIds = Uri.encode(
                Json.encodeToString(visitedSignIds.toList())
            )
            return "sign_detail/$signId?visitedSignIds=$encodedVisitedSignIds"
        }
    }

    object Lessons : Screen("lessons")
    object LessonDetail : Screen("lesson_detail/{lessonId}") {
        fun createRoute(lessonId: String) = "lesson_detail/$lessonId"
    }

    object Settings : Screen("settings")
}

enum class BottomTab(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
) {
    SEARCH(Screen.Search, "Поиск", Icons.Default.Search),
    FAVORITES(Screen.Favorites, "Избранное", Icons.Default.Favorite),
    CATEGORIES(Screen.Categories, "Категории", Icons.Default.Home),
    LESSONS(Screen.Lessons, "Обучение", Icons.Default.Info),
    SETTINGS(Screen.Settings, "Настройки", Icons.Default.Settings)
}
