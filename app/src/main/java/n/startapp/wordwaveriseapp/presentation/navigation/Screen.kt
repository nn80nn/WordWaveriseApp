package n.startapp.wordwaveriseapp.presentation.navigation

import androidx.annotation.DrawableRes
import n.startapp.wordwaveriseapp.R

sealed class Screen(
    val route: String,
    val title: String,
    @DrawableRes val icon: Int
) {
    data object Search : Screen(
        route = "search",
        title = "Поиск",
        icon = R.drawable.ic_search
    )

    data object Saved : Screen(
        route = "saved",
        title = "Сохранённые",
        icon = R.drawable.ic_save
    )

    data object Tasks : Screen(
        route = "tasks",
        title = "Задания",
        icon = R.drawable.ic_tasks
    )

    data object Profile : Screen(
        route = "profile",
        title = "Профиль",
        icon = R.drawable.ic_profile
    )

    companion object {
        val bottomNavigationScreens = listOf(Search, Saved, Tasks, Profile)
    }
}
