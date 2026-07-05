package com.wordwaverise.wordwaveriseapp.presentation.navigation

import androidx.annotation.DrawableRes
import com.wordwaverise.wordwaveriseapp.R

sealed class Screen(
    val route: String,
    val title: String,
    @DrawableRes val icon: Int
) {
    data object Search : Screen(
        route = "search",
        title = "Поиск",
        icon = R.drawable.search2
    )

    data object Saved : Screen(
        route = "saved",
        title = "Сохранённые",
        icon = R.drawable.save2
    )

    data object Tasks : Screen(
        route = "tasks",
        title = "Задания",
        icon = R.drawable.tacks2
    )

    data object Profile : Screen(
        route = "profile",
        title = "Профиль",
        icon = R.drawable.profile2
    )

    data object WordDetail : Screen(
        route = "word_detail/{word}",
        title = "Детали слова",
        icon = R.drawable.search2
    ) {
        fun createRoute(word: String) = "word_detail/$word"
    }

    companion object {
        val bottomNavigationScreens = listOf(Search, Saved, Tasks, Profile)
    }
}
