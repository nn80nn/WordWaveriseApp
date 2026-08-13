package com.wordwaverise.wordwaveriseapp.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Tab icons are vectors, not the raster PNGs the bar used to ship: those were
 * baked in the previous brand cyan and had to be re-tinted at runtime, and they
 * softened on dense screens because there was only one density of each.
 */
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Search : Screen(
        route = "search",
        title = "Поиск",
        icon = Icons.Outlined.Search
    )

    data object Saved : Screen(
        route = "saved",
        title = "Слова",
        icon = Icons.Outlined.MenuBook
    )

    data object Tasks : Screen(
        route = "tasks",
        title = "Задания",
        icon = Icons.Outlined.Checklist
    )

    data object Profile : Screen(
        route = "profile",
        title = "Профиль",
        icon = Icons.Outlined.AccountCircle
    )

    data object WordDetail : Screen(
        route = "word_detail/{word}",
        title = "Детали слова",
        icon = Icons.Outlined.Search
    ) {
        fun createRoute(word: String) = "word_detail/$word"
    }

    companion object {
        val bottomNavigationScreens = listOf(Search, Saved, Tasks, Profile)
    }
}
