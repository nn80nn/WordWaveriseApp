package com.wordwaverise.wordwaveriseapp.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoStories
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

    /**
     * Слова и задания под одной вкладкой, с переключателем сегментов сверху.
     *
     * Слились они ради четвёртого слота: пятая вкладка сжала бы подписи, а «Книги» открывают
     * чаще, чем задания отдельно от слов. Ни одна из половин при этом не урезана — обе остались
     * целыми экранами, у каждой свой ViewModel.
     *
     * ⚠️ Один маршрут, а не два. Подсветка вкладки сравнивает маршрут целиком
     * ([BottomNavigationBar]), поэтому на втором маршруте вкладка бы гасла — то есть человек
     * терял бы место ровно тогда, когда он внутри него.
     */
    data object Study : Screen(
        route = "study",
        title = "Учёба",
        icon = Icons.Outlined.Checklist
    ) {
        /**
         * Оба аргумента необязательны, поэтому переход на голый `study` из нижней вкладки
         * по-прежнему подходит под этот шаблон — второй вкладки не появляется.
         */
        const val ROUTE_FULL = "study?segment={segment}&import={import}&assignment={assignment}"

        const val SEGMENT_WORDS = "words"
        const val SEGMENT_TASKS = "tasks"

        const val NO_ASSIGNMENT = -1

        /**
         * Тот же экран, открытый по нажатой ссылке на общую папку (`/f/{token}`).
         *
         * ⚠️ Сегмент назван явно: ссылка на папку — это про слова, и открыть её на «Заданиях»
         * значило бы спрятать то, ради чего по ней нажали.
         */
        fun createImportRoute(token: String) = "study?segment=$SEGMENT_WORDS&import=$token"

        /** Тот же экран, открытый по заданию преподавателя. */
        fun createAssignmentRoute(assignmentId: Int) =
            "study?segment=$SEGMENT_TASKS&assignment=$assignmentId"
    }

    /** Полка. Книга — это текст, который человек загрузил себе; чужой её не видит. */
    data object Books : Screen(
        route = "books",
        title = "Книги",
        icon = Icons.Outlined.AutoStories
    )

    /**
     * Чтение. Вне [bottomNavigationScreens]: страница книги — это весь экран, и панель
     * вкладок на нём скрывается (см. `showBottomBar` в MainActivity).
     */
    data object Reader : Screen(
        route = "reader/{bookId}",
        title = "Чтение",
        icon = Icons.AutoMirrored.Outlined.MenuBook
    ) {
        fun createRoute(bookId: Int) = "reader/$bookId"
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
    ) {
        /** Тот же экран, открытый по нажатому приглашению (`/g/{token}`). */
        const val ROUTE_WITH_INVITE = "groups?invite={invite}"

        fun createInviteRoute(token: String) = "groups?invite=$token"
    }

    companion object {
        /** Порядок вкладок — тот же, что в вебе (`BottomTabBar.vue`). */
        val bottomNavigationScreens = listOf(Search, Study, Books, Profile)
    }
}
