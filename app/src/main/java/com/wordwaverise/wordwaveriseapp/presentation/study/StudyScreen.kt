package com.wordwaverise.wordwaveriseapp.presentation.study

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wordwaverise.wordwaveriseapp.R
import com.wordwaverise.wordwaveriseapp.presentation.components.Segment
import com.wordwaverise.wordwaveriseapp.presentation.components.SegmentedSwitch
import com.wordwaverise.wordwaveriseapp.presentation.navigation.Screen
import com.wordwaverise.wordwaveriseapp.presentation.saved.SavedScreen
import com.wordwaverise.wordwaveriseapp.presentation.saved.SavedWordsViewModel
import com.wordwaverise.wordwaveriseapp.presentation.tasks.TasksScreen
import com.wordwaverise.wordwaveriseapp.presentation.tasks.TasksViewModel
import com.wordwaverise.wordwaveriseapp.ui.theme.waveSurface

/**
 * «Учёба»: свой словарь и задания под одной вкладкой.
 *
 * Слились они ради четвёртого слота в нижней панели — пятая вкладка сжала бы подписи, а «Книги»
 * открывают чаще, чем задания отдельно от слов. Ни одна половина при этом не урезана: это те же
 * два экрана, каждый со своим ViewModel, под общим переключателем.
 *
 * ⚠️ Переключатель живёт здесь, а не в общей панели сверху: у Android верхней панели нет вовсе
 * (`Scaffold` в MainActivity задаёт только `bottomBar`), и заводить её ради одного контрола
 * значило бы пересчитать отступы на всех экранах приложения.
 *
 * ⚠️ Отступ у половин разный и таким остаётся: у списка слов чипы папок скроллятся от самого
 * края экрана, а у заданий содержимое отбито на 16.dp. Выровнять их значило бы либо обрезать
 * ленту папок, либо прижать задания к краю.
 */
@Composable
fun StudyScreen(
    startSegment: String?,
    importToken: String?,
    startAssignmentId: Int?,
    onWordClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Пришли по ссылке или по заданию — сегмент назван в маршруте; иначе открываются слова.
    var segment by rememberSaveable {
        mutableStateOf(startSegment ?: Screen.Study.SEGMENT_WORDS)
    }

    val savedViewModel: SavedWordsViewModel = hiltViewModel()
    val savedState = savedViewModel.state.value

    // Папка забирается сама: человек нажал ссылку — это и есть просьба. Ещё одна кнопка
    // «добавить» после неё была бы вопросом, на который уже ответили.
    LaunchedEffect(importToken) {
        if (!importToken.isNullOrBlank()) {
            segment = Screen.Study.SEGMENT_WORDS
            savedViewModel.setImportLink(importToken)
            savedViewModel.importSharedFolder()
        }
    }

    // Ссылка уходит в системный лист «Поделиться»: на телефоне папку отправляют в конкретный
    // чат, и «скопировано в буфер» оставляет человека доделывать это руками.
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.podelitsya_papkoy_slov)
    val shareUrl = savedState.pendingShareUrl
    LaunchedEffect(shareUrl) {
        val url = shareUrl ?: return@LaunchedEffect
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        context.startActivity(Intent.createChooser(send, shareTitle))
        savedViewModel.shareHandled()
    }

    /**
     * Точка на «Заданиях», когда есть невыполненное.
     *
     * ⚠️ Та же самая TasksViewModel, что заводит себе TasksScreen: Hilt отдаёт её по владельцу,
     * а владелец у обеих половин один — эта запись в стеке навигации. Второй копии состояния
     * не появляется, и открывать вкладку ради счётчика не приходится.
     *
     * Точка, а не число: сколько именно заданий висит, видно на самом экране, а на переключателе
     * важно только «там что-то ждёт».
     */
    val tasksViewModel: TasksViewModel = hiltViewModel()
    val tasksState by tasksViewModel.state.collectAsState()
    val hasOpenAssignments = tasksState.assignments.any { !it.completed }

    val segments = listOf(
        Segment(Screen.Study.SEGMENT_WORDS, "Слова"),
        Segment(Screen.Study.SEGMENT_TASKS, "Задания", badge = hasOpenAssignments)
    )

    /**
     * Половины листаются и пальцем, и переключателем — это одно и то же движение, сказанное
     * двумя способами. Пейджер здесь ведущий: у него есть промежуточные состояния, которых у
     * булева переключателя нет, и синхронизировать их в обратную сторону значило бы дёргать
     * экран на середине жеста.
     */
    val pages = listOf(Screen.Study.SEGMENT_WORDS, Screen.Study.SEGMENT_TASKS)
    val pagerState = rememberPagerState(
        initialPage = pages.indexOf(segment).coerceAtLeast(0),
        pageCount = { pages.size }
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { segment = pages[it] }
    }
    LaunchedEffect(segment) {
        val index = pages.indexOf(segment)
        if (index >= 0 && index != pagerState.currentPage) pagerState.animateScrollToPage(index)
    }

    Column(modifier = modifier.fillMaxSize().waveSurface()) {
        SegmentedSwitch(
            segments = segments,
            selected = segment,
            onSelect = { segment = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        HorizontalPager(state = pagerState) { index ->
            when (pages[index]) {
                Screen.Study.SEGMENT_TASKS -> TasksScreen(startAssignmentId = startAssignmentId)

                else -> SavedScreen(
                state = savedState,
                onDeleteWord = savedViewModel::deleteEntry,
                onWordClick = onWordClick,
                onSelectCategory = savedViewModel::selectCategory,
                onShowCategorySheet = savedViewModel::showCategorySheet,
                onHideCategorySheet = savedViewModel::hideCategorySheet,
                onSetWordToFile = savedViewModel::setWordToFile,
                onToggleFolder = savedViewModel::toggleFolder,
                onSaveFolders = savedViewModel::saveFolders,
                onCreateCategory = savedViewModel::createCategory,
                onDeleteCategory = savedViewModel::deleteCategory,
                onRenameCategory = savedViewModel::renameCategory,
                onShareCategory = savedViewModel::shareCategory,
                onImportLinkChange = savedViewModel::setImportLink,
                onImportFolder = savedViewModel::importSharedFolder,
                onNewCategoryNameChange = savedViewModel::setNewCategoryName,
                onNewCategoryParentChange = savedViewModel::setNewCategoryParent,
                onSetCategoryParent = savedViewModel::setCategoryParent,
                onSearchChange = savedViewModel::setSearchQuery,
                onSortChange = savedViewModel::setSortBy,
                onFolderQueryChange = savedViewModel::setFolderQuery,
                onFolderSortChange = savedViewModel::setFolderSort,
                    onRefresh = savedViewModel::refresh
                )
            }
        }
    }
}
