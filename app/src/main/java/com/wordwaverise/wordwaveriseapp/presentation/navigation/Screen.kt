package com.wordwaverise.wordwaveriseapp.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Group
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
    ) {
        /**
         * Тот же экран, открытый по заданию преподавателя.
         *
         * Аргумент необязательный, поэтому переход на голый `tasks` из нижней вкладки по-прежнему
         * подходит под этот шаблон — второй вкладки не появляется.
         */
        const val ROUTE_WITH_ASSIGNMENT = "tasks?assignment={assignment}"

        fun createAssignmentRoute(assignmentId: Int) = "tasks?assignment=$assignmentId"

        const val NO_ASSIGNMENT = -1
    }

    data object Profile : Screen(
        route = "profile",
        title = "Профиль",
        icon = Icons.Outlined.AccountCircle
    )

    data object WordDetail : Screen(
        route = "word_detail/{word}?exact={exact}",
        title = "Детали слова",
        icon = Icons.Outlined.Search
    ) {
        /**
         * [exact] отключает резолвер: ни лемматизации, ни исправлений.
         *
         * ⚠️ Обязателен для слова из сохранённых. Оно уже результат чужого решения: человек мог
         * найти его через «искать точно» именно потому, что резолвер уводил на лемму. Открывать
         * его обычным поиском значит применять лемматизацию заново — «busker» превращался в
         * «busk» при каждом нажатии.
         */
        fun createRoute(word: String, exact: Boolean = false) = "word_detail/$word?exact=$exact"
    }

    /**
     * Классы ученика. **Вне** [bottomNavigationScreens] намеренно: четыре вкладки — общий
     * каркас с сайтом, и пятая сжала бы подписи ради экрана, который открывают не каждый день.
     * Открывается из Профиля.
     */
    data object Groups : Screen(
        route = "groups",
        title = "Группы",
        icon = Icons.Outlined.Group
    )

    companion object {
        val bottomNavigationScreens = listOf(Search, Saved, Tasks, Profile)
    }
}
