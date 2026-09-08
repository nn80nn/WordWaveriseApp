package com.wordwaverise.wordwaveriseapp.presentation.reader

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.BlockDto
import com.wordwaverise.wordwaveriseapp.presentation.components.SaveToFolderDialog
import com.wordwaverise.wordwaveriseapp.presentation.components.Segment
import com.wordwaverise.wordwaveriseapp.presentation.components.SegmentedSwitch
import com.wordwaverise.wordwaveriseapp.presentation.search.components.ContextCard
import com.wordwaverise.wordwaveriseapp.ui.theme.Comfortaa
import com.wordwaverise.wordwaveriseapp.ui.theme.WaveTheme
import com.wordwaverise.wordwaveriseapp.ui.theme.waveSurface
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Чтение.
 *
 * Слово выбирается нажатием прямо в тексте: позиция символа переводится в токен по смещениям,
 * уже приехавшим с сервера. Отдельного элемента на слово нет — на окно их вышли бы тысячи, и
 * текст перестал бы быть текстом.
 *
 * ⚠️ Смещения вложенные: у предложения они считаются от блока, у токена — от предложения.
 * Перепутать их значит подсветить не то слово, и заметно это станет не сразу.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: Int,
    onBack: () -> Unit,
    onOpenArticle: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = WaveTheme.colors
    val snackbar = remember { SnackbarHostState() }
    var contentsOpen by remember { mutableStateOf(false) }

    LaunchedEffect(bookId) { viewModel.start(bookId) }

    // Последний скролл не должен пропасть в дебаунсе.
    DisposableEffect(Unit) { onDispose { viewModel.commitPosition() } }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(message)
        viewModel.messageShown()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            ReaderTopBar(
                title = state.book?.title.orEmpty(),
                subtitle = subtitleOf(state),
                showContents = state.chapters.size > 1,
                bookmarked = state.bookmarkedHere,
                onBack = onBack,
                onContents = { contentsOpen = true },
                onBookmark = viewModel::toggleBookmark,
                onBookmarks = { viewModel.showBookmarks(true) },
                onSettings = { viewModel.showSettings(true) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .waveSurface()
                .padding(padding)
        ) {
            when {
                state.isLoading && state.blocks.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = colors.secondary) }

                state.error != null && state.blocks.isEmpty() -> Text(
                    text = state.error!!,
                    color = colors.error,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp)
                )

                state.paged -> PagedReader(state = state, viewModel = viewModel)

                else -> ScrollReader(state = state, viewModel = viewModel)
            }

            if (state.target != null) {
                HintSheet(
                    state = state,
                    viewModel = viewModel,
                    onOpenArticle = onOpenArticle,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }

    if (contentsOpen) {
        ModalBottomSheet(
            onDismissRequest = { contentsOpen = false },
            containerColor = colors.surface
        ) {
            Text(
                text = "Оглавление",
                fontFamily = Comfortaa,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = colors.textPrimary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(state.chapters, key = { it.index }) { chapter ->
                    Text(
                        text = chapter.title ?: "Глава ${chapter.index + 1}",
                        fontSize = 14.sp,
                        color = colors.textPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                contentsOpen = false
                                viewModel.jumpTo(chapter.firstOrdinal)
                            }
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }

    if (state.bookmarksOpen) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.showBookmarks(false) },
            containerColor = colors.surface
        ) {
            Text(
                text = "Закладки",
                fontFamily = Comfortaa,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = colors.textPrimary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            if (state.bookmarks.isEmpty()) {
                Text(
                    text = "Пока ни одной. Нажмите на флажок сверху, чтобы отметить место, " +
                        "к которому захочется вернуться.",
                    fontSize = 14.sp,
                    color = colors.textMuted,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(state.bookmarks, key = { it.ordinal }) { mark ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.showBookmarks(false)
                                viewModel.jumpTo(mark.ordinal)
                            }
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        val chapter = state.chapters.firstOrNull { it.index == mark.chapterIndex }?.title
                        if (chapter != null) {
                            Text(chapter, fontSize = 11.sp, color = colors.textMuted)
                            Spacer(Modifier.height(2.dp))
                        }
                        Text(
                            text = mark.preview,
                            fontSize = 14.sp,
                            color = colors.textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }

    if (state.settingsOpen) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.showSettings(false) },
            containerColor = colors.surface
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "Как листать",
                    fontFamily = Comfortaa,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = colors.textPrimary
                )
                Spacer(Modifier.height(12.dp))
                SegmentedSwitch(
                    segments = listOf(
                        Segment(SCROLL_MODE, "Скроллом"),
                        Segment(PAGED_MODE, "Страницами")
                    ),
                    selected = if (state.paged) PAGED_MODE else SCROLL_MODE,
                    onSelect = { viewModel.setPaged(it == PAGED_MODE) }
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Страницы листаются пальцем влево и вправо. Место, где вы остановились, " +
                        "запоминается в любом режиме.",
                    fontSize = 13.sp,
                    color = colors.textMuted
                )
                Spacer(Modifier.height(24.dp))
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }

    if (state.folderSheetOpen) {
        SaveToFolderDialog(
            word = state.currentLemma.orEmpty(),
            summary = state.currentTranslation,
            folders = state.ownFolders,
            chosen = state.chosenFolders,
            saving = state.isSaving,
            onToggle = viewModel::toggleFolder,
            onSelectNone = viewModel::clearFolders,
            onCreate = viewModel::createFolder,
            onConfirm = viewModel::confirmFolders,
            onDismiss = viewModel::closeFolderSheet
        )
    }
}

private const val SCROLL_MODE = "scroll"
private const val PAGED_MODE = "paged"

/**
 * Поля страницы.
 *
 * ⚠️ Это не украшение, а часть арифметики: высота страницы считается **по этому же полю**.
 * Пока поля вычитались из текста, но не из высоты, страница вмещала на строку больше, чем
 * показывала, — и первая строка выезжала под панель обрезанной пополам.
 */
private val PAGE_PAD_H = 20.dp
private val PAGE_PAD_TOP = 14.dp
private val PAGE_PAD_BOTTOM = 18.dp

/** Скорость броска, после которой страница переворачивается независимо от пройденного пути. */
private const val FLICK_VELOCITY = 250f

@Composable
private fun subtitleOf(state: ReaderState): String? {
    val percent = ((state.book?.position?.progress ?: 0.0) * 100).roundToInt()
    val chapter = state.chapters
        .firstOrNull { it.index == state.book?.position?.chapterIndex }
        ?.title
    return listOfNotNull(chapter, "$percent%").joinToString(" · ").takeIf { percent > 0 || chapter != null }
}

@Composable
private fun ReaderTopBar(
    title: String,
    subtitle: String?,
    showContents: Boolean,
    bookmarked: Boolean,
    onBack: () -> Unit,
    onContents: () -> Unit,
    onBookmark: () -> Unit,
    onBookmarks: () -> Unit,
    onSettings: () -> Unit
) {
    val colors = WaveTheme.colors
    Column(modifier = Modifier.background(colors.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp)
                .height(52.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "К полке",
                    tint = colors.textSecondary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            // Нажатие ставит и снимает закладку здесь, долгое — открывает их список: спрятать
            // список за третьей кнопкой значило бы занять панель ради того, что открывают редко.
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .pointerInput(bookmarked) {
                        detectTapGestures(onTap = { onBookmark() }, onLongPress = { onBookmarks() })
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (bookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (bookmarked) "Убрать закладку" else "Поставить закладку",
                    tint = if (bookmarked) colors.secondary else colors.textSecondary
                )
            }
            if (showContents) {
                IconButton(onClick = onContents) {
                    Icon(
                        Icons.AutoMirrored.Outlined.List,
                        contentDescription = "Оглавление",
                        tint = colors.textSecondary
                    )
                }
            }
            IconButton(onClick = onSettings) {
                Icon(
                    Icons.Outlined.Tune,
                    contentDescription = "Настройки чтения",
                    tint = colors.textSecondary
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, color = colors.hairline)
    }
}

// ── Скролл ────────────────────────────────────────────────────────────

@Composable
private fun ScrollReader(state: ReaderState, viewModel: ReaderViewModel) {
    val listState = rememberLazyListState()

    // Встать туда, где книгу бросили. Окно начинается раньше этого места, поэтому индекс
    // ищется по ordinal, а не берётся нулём, а смещение возвращает в ту же строку абзаца.
    LaunchedEffect(state.openAt, state.blocks.size) {
        val target = state.openAt ?: return@LaunchedEffect
        val index = state.blocks.indexOfFirst { it.ordinal == target }
        if (index >= 0) {
            listState.scrollToItem(index, state.openOffset)
            viewModel.openHandled()
        }
    }

    /**
     * ⚠️ Дописанный спереди кусок сдвигает весь список, и человек, долиставший до верха,
     * прыгнул бы вперёд ровно в тот момент, когда шёл назад. Якорь — ordinal верхнего блока
     * и его смещение: после подстановки позиция восстанавливается по ним.
     */
    var anchor by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    LaunchedEffect(state.firstOrdinal) {
        val (ordinal, offset) = anchor ?: return@LaunchedEffect
        val index = state.blocks.indexOfFirst { it.ordinal == ordinal }
        if (index >= 0) listState.scrollToItem(index, offset)
        anchor = null
    }

    LaunchedEffect(listState, state.blocks.size) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (first, offset) ->
                // ⚠️ То же, что и у страниц: список ещё стоит в начале, потому что не доехал
                // до места, а не потому, что человек туда вернулся.
                if (viewModel.state.value.openAt != null) return@collect
                /**
                 * ⚠️ Место — это абзац, который человек **читает**, а не тот, что задел верхний
                 * край экрана одной строкой. `firstVisibleItemIndex` — второе: абзац считается
                 * видимым, пока от него остался хоть пиксель, и позиция уезжала на абзац назад.
                 */
                val info = listState.layoutInfo.visibleItemsInfo
                val reading = info.firstOrNull { it.offset >= 0 }
                    ?: info.firstOrNull { it.offset + it.size > it.size / 2 }
                    ?: info.firstOrNull()
                val index = reading?.index ?: first
                state.blocks.getOrNull(index)?.let {
                    // Смещение внутри абзаца имеет смысл только для того, что стоит наверху.
                    viewModel.savePosition(it.ordinal, if (index == first) offset else 0)
                }

                val last = info.lastOrNull()?.index ?: 0
                if (last >= state.blocks.size - 6) viewModel.loadMore()

                if (first <= 2 && !state.atStart && anchor == null) {
                    state.blocks.getOrNull(first)?.let { anchor = it.ordinal to offset }
                    viewModel.loadBefore()
                }
            }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            start = PAGE_PAD_H,
            end = PAGE_PAD_H,
            top = PAGE_PAD_TOP,
            bottom = 48.dp
        )
    ) {
        items(state.blocks, key = { it.ordinal }) { block ->
            BlockText(
                block = block,
                selected = state.target?.takeIf { it.blockOrdinal == block.ordinal },
                onTap = viewModel::analyze
            )
        }
        if (state.atEnd && state.blocks.isNotEmpty()) {
            item { EndOfBook() }
        }
    }
}

@Composable
private fun EndOfBook() {
    Text(
        text = "Конец книги",
        fontSize = 12.sp,
        color = WaveTheme.colors.textMuted,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        textAlign = TextAlign.Center
    )
}

// ── Страницы ──────────────────────────────────────────────────────────

/**
 * Страница — экран текста, и листается она пальцем влево-вправо.
 *
 * ⚠️ Весь загруженный кусок — **один** `Text`, а страница это его вертикальный сдвиг с обрезкой.
 * Резать текст на куски заранее нельзя: где кончится строка, знает только раскладка, а она
 * зависит от ширины экрана, шрифта и того, как перенеслось конкретное слово.
 *
 * ⚠️ Страница держится за **абзац**, а не за свой номер. Догрузка дописывает текст и
 * перекладывает разбиение, и номер после этого указывал бы на другое место — читатель, дойдя
 * до конца загруженного, отлетал бы в начало ровно в момент подгрузки.
 */
@Composable
private fun PagedReader(state: ReaderState, viewModel: ReaderViewModel) {
    val colors = WaveTheme.colors
    val scope = rememberCoroutineScope()

    val flow = remember(state.blocks, state.target) { buildFlow(state, colors) }

    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var pageHeightPx by remember { mutableStateOf(0) }
    val insetPx = with(LocalDensity.current) { (PAGE_PAD_TOP + PAGE_PAD_BOTTOM).roundToPx() }
    var page by remember { mutableStateOf(0) }
    /** Абзац, за который держится текущая страница. Переживает перекладку текста. */
    var anchorOrdinal by remember { mutableStateOf<Int?>(null) }
    val drag = remember { Animatable(0f) }
    /**
     * ⚠️ Сколько палец увёл страницу — считается **здесь**, а не читается из `drag`.
     *
     * `Animatable.snapTo` — suspend, поэтому каждый шаг жеста уезжает в отдельную корутину и
     * значение отстаёт от пальца на несколько кадров. Решение «листать или вернуть», принятое
     * по нему в момент отпускания, видело почти ноль — короткий бросок не листал вовсе.
     */
    var shift by remember { mutableStateOf(0f) }
    var widthPx by remember { mutableStateOf(1) }

    val pageTops = remember(layout, pageHeightPx) {
        val result = layout
        if (result == null || pageHeightPx <= 0) listOf(0f) else pagesOf(result, pageHeightPx.toFloat())
    }

    fun ordinalAt(top: Float): Int? {
        val result = layout ?: return null
        return flow.blockAt(result.getOffsetForPosition(Offset(0f, top + 1f)))
    }

    fun pageOf(ordinal: Int): Int? {
        val result = layout ?: return null
        val start = flow.startOf(ordinal) ?: return null
        val top = result.getLineTop(result.getLineForOffset(start))
        return pageTops.indexOfLast { it <= top }.coerceAtLeast(0)
    }

    // Открыть там, где бросили.
    LaunchedEffect(state.openAt, pageTops.size) {
        val target = state.openAt ?: return@LaunchedEffect
        val found = pageOf(target) ?: return@LaunchedEffect
        page = found
        anchorOrdinal = target
        viewModel.openHandled()
    }

    // Текст переложили — страница ищется заново по абзацу, а не остаётся номером.
    LaunchedEffect(pageTops, flow) {
        val anchor = anchorOrdinal ?: return@LaunchedEffect
        pageOf(anchor)?.let { if (it != page) page = it }
    }

    /**
     * Место запоминается по абзацу, который стоит наверху страницы.
     *
     * ⚠️ Пока место не восстановлено (`openAt`), писать нечего: экран стоит на нулевой
     * странице просто потому, что ещё не доехал до нужной, и запись отсюда стирала бы
     * настоящее место — именно так переключение режима отправляло книгу в начало.
     */
    LaunchedEffect(page, pageTops.size, state.openAt) {
        if (state.openAt != null) return@LaunchedEffect
        val top = pageTops.getOrNull(page) ?: return@LaunchedEffect
        ordinalAt(top)?.let {
            anchorOrdinal = it
            viewModel.savePosition(it, top.toInt())
        }

        if (page >= pageTops.size - 2) viewModel.loadMore()
        if (page == 0 && !state.atStart) viewModel.loadBefore()
    }

    /**
     * Куда идёт начатый перелист: +1 вперёд, −1 назад, 0 — никуда.
     *
     * Нужен, чтобы соседняя страница была нарисована **до** того, как палец её откроет: без
     * неё за уходящим текстом видна пустота, и жест читается как поломка, а не как перелист.
     */
    var pending by remember { mutableStateOf(0) }

    fun turn(direction: Int) {
        val next = page + direction
        if (next < 0 || next > pageTops.lastIndex) return
        shift = 0f
        scope.launch {
            drag.animateTo(-direction * widthPx.toFloat(), tween(220))
            page = next
            pending = 0
            drag.snapTo(0f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged {
                widthPx = it.width.coerceAtLeast(1)
                // Ровно то, что видно между полями: страница обязана вмещать столько же,
                // сколько показывает, иначе её край режет строку пополам.
                pageHeightPx = (it.height - insetPx).coerceAtLeast(1)
            }
            .clipToBounds()
            /**
             * ⚠️ Страницу листает **бросок**, а не длинный протяг.
             *
             * Порог в пятую часть экрана требовал провести палец через полтелефона — движение,
             * которое нельзя делать на каждой странице. `draggable` отдаёт в `onDragStopped`
             * скорость, поэтому короткий быстрый бросок засчитывается наравне с медленным
             * протягом на шестую часть ширины, как в любой другой читалке.
             *
             * ⚠️ Тап живёт отдельным `pointerInput` **после** `draggable`: у жестов разные оси,
             * и делить один поток событий им больше незачем — `draggable` сам отпускает
             * нажатие, которое никуда не поехало.
             */
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { amount ->
                    val next = shift + amount
                    // У края книги следующей страницы нет, и резинка здесь врала бы,
                    // что она есть: палец идёт втрое медленнее и ничего не открывает.
                    val atEdge = (page == 0 && next > 0) ||
                        (page == pageTops.lastIndex && next < 0)
                    val clamped = if (atEdge) next / 3f else next
                    pending = when {
                        atEdge -> 0
                        clamped < 0f -> 1
                        clamped > 0f -> -1
                        else -> 0
                    }
                    shift = clamped
                    scope.launch { drag.snapTo(clamped) }
                },
                onDragStopped = { velocity ->
                    val enough = abs(velocity) > FLICK_VELOCITY || abs(shift) > widthPx / 8f
                    when {
                        enough && shift < 0 && page < pageTops.lastIndex -> turn(1)
                        enough && shift > 0 && page > 0 -> turn(-1)
                        else -> {
                            shift = 0f
                            drag.animateTo(0f, tween(180))
                            pending = 0
                        }
                    }
                }
            )
            .pointerInput(flow, pageTops, page) {
                detectTapGestures { position ->
                    val result = layout ?: return@detectTapGestures
                    val top = pageTops.getOrNull(page) ?: return@detectTapGestures
                    val at = result.getOffsetForPosition(position + Offset(0f, top))
                    flow.locate(at)?.let(viewModel::analyze)
                }
            }
    ) {
        // Соседняя страница — под уходящей и на её же ширине в стороне. Рисуется только на
        // время жеста: держать её всегда значит вёрстку всей книги дважды.
        if (pending != 0) {
            val neighbour = page + pending
            if (neighbour in 0..pageTops.lastIndex) {
                PageLayer(
                    flow = flow,
                    top = pageTops[neighbour],
                    offsetX = drag.value + pending * widthPx.toFloat(),
                    onLayout = null,
                    colors = colors
                )
            }
        }

        PageLayer(
            flow = flow,
            top = pageTops.getOrNull(page) ?: 0f,
            offsetX = drag.value,
            onLayout = { layout = it },
            colors = colors
        )

        if (state.atEnd && page == pageTops.lastIndex && pending == 0) {
            Box(Modifier.align(Alignment.BottomCenter)) { EndOfBook() }
        }
    }
}

/**
 * Одна страница: тот же текст, сдвинутый по вертикали и обрезанный по экрану.
 *
 * ⚠️ Фон непрозрачный. Две страницы во время перелиста лежат друг на друге, и без заливки
 * уходящий текст просвечивал бы сквозь приходящий.
 */
@Composable
private fun PageLayer(
    flow: TextFlow,
    top: Float,
    offsetX: Float,
    onLayout: ((TextLayoutResult) -> Unit)?,
    colors: com.wordwaverise.wordwaveriseapp.ui.theme.WaveColors
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { translationX = offsetX }
            .background(colors.background)
            // ⚠️ Поля стоят **до** обрезки, поэтому обрезается именно то поле текста, которое
            // человек видит. Раньше поля были внутри текста, и строка, не влезшая в страницу,
            // дорисовывалась в отступ — сверху из-под панели торчала её половина.
            .padding(start = PAGE_PAD_H, end = PAGE_PAD_H, top = PAGE_PAD_TOP, bottom = PAGE_PAD_BOTTOM)
            .clipToBounds()
    ) {
        Text(
            text = flow.text,
            fontSize = 18.sp,
            lineHeight = 31.sp,
            color = colors.textPrimary,
            onTextLayout = { onLayout?.invoke(it) },
            modifier = Modifier
                .fillMaxWidth()
                // ⚠️ Без `unbounded` текст меряется высотой экрана и рисует только первый
                // экран строк: сдвиг на вторую страницу открывал пустоту, потому что рисовать
                // там было нечего. Обрезает внешний Box, а не сам текст.
                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                // Сдвиг целыми пикселями: дробный оставляет над первой строкой полоску
                // предыдущей — ровно тот мусор, ради которого и стоит обрезка.
                .offset { IntOffset(0, -top.roundToInt()) }
        )
    }
}

/** Границы страниц — верх той строки, с которой страница начинается. */
private fun pagesOf(layout: TextLayoutResult, pageHeight: Float): List<Float> {
    if (pageHeight <= 0f || layout.lineCount == 0) return listOf(0f)
    val tops = mutableListOf(0f)
    var current = 0f
    for (line in 0 until layout.lineCount) {
        if (layout.getLineBottom(line) - current > pageHeight) {
            current = layout.getLineTop(line)
            tops += current
        }
    }
    return tops
}

/**
 * Загруженный кусок книги одной строкой — и карта, по которой смещение в ней превращается
 * обратно в блок, предложение и токен.
 */
private class TextFlow(
    val text: AnnotatedString,
    private val blocks: List<Triple<Int, Int, BlockDto>>
) {
    fun startOf(ordinal: Int): Int? = blocks.firstOrNull { it.third.ordinal == ordinal }?.first

    fun blockAt(offset: Int): Int? =
        blocks.firstOrNull { offset >= it.first && offset < it.second }?.third?.ordinal

    fun locate(offset: Int): TapTarget? {
        val entry = blocks.firstOrNull { offset >= it.first && offset < it.second } ?: return null
        return locateInBlock(entry.third, offset - entry.first)
    }
}

private fun buildFlow(state: ReaderState, colors: com.wordwaverise.wordwaveriseapp.ui.theme.WaveColors): TextFlow {
    val ranges = mutableListOf<Triple<Int, Int, BlockDto>>()
    val text = buildAnnotatedString {
        for ((index, block) in state.blocks.withIndex()) {
            if (index > 0) append("\n\n")
            val start = length
            appendBlock(
                block,
                state.target?.takeIf { it.blockOrdinal == block.ordinal },
                colors,
                styled = true
            )
            ranges += Triple(start, length, block)
        }
    }
    return TextFlow(text, ranges)
}

// ── Общая разметка блока ──────────────────────────────────────────────

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendBlock(
    block: BlockDto,
    selected: TapTarget?,
    colors: com.wordwaverise.wordwaveriseapp.ui.theme.WaveColors,
    /**
     * В режиме страниц весь кусок — один `Text`, поэтому размер и насыщенность заголовка
     * приходится нести стилем участка. Без этого название главы неотличимо от абзаца, а
     * граница между главами — единственное, что размечает книгу на глаз.
     */
    styled: Boolean = false
) {
    val heading = styled && block.kind == "HEADING"
    if (heading) {
        pushStyle(SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold))
    }
    if (block.sentences.isEmpty()) {
        append(block.text)
        if (heading) pop()
        return
    }
    var cursor = 0
    for (sentence in block.sentences) {
        if (sentence.start > cursor) append(block.text.substring(cursor, sentence.start))
        val chosen = selected?.takeIf { it.sentenceIndex == sentence.index }?.tokenIndex
        val group = sentence.tokens.firstOrNull { it.index == chosen }
        // Фразовый глагол подсвечивается целиком, потому что целиком и разрешается.
        val highlighted = buildSet {
            if (chosen != null) add(chosen)
            group?.groupWith?.let { addAll(it) }
        }

        var inner = 0
        for (token in sentence.tokens) {
            if (token.start > inner) append(sentence.text.substring(inner, token.start))
            val piece = sentence.text.substring(token.start, token.end)
            if (token.index in highlighted) {
                withStyle(
                    SpanStyle(
                        background = colors.secondary.copy(alpha = 0.24f),
                        fontWeight = FontWeight.SemiBold
                    )
                ) { append(piece) }
            } else {
                append(piece)
            }
            inner = token.end
        }
        if (inner < sentence.text.length) append(sentence.text.substring(inner))
        cursor = sentence.end
    }
    if (cursor < block.text.length) append(block.text.substring(cursor))
    if (heading) pop()
}

/**
 * Смещение в тексте блока → предложение и токен.
 *
 * Каретка встаёт между символами, поэтому у левого края слова смещение совпадает с его началом,
 * а у правого — с концом предыдущего: второй случай засчитывается слову слева, иначе тап по
 * концу слова читался бы как промах.
 */
private fun locateInBlock(block: BlockDto, at: Int): TapTarget? {
    val sentence = block.sentences.firstOrNull { at >= it.start && at < it.end }
        ?: block.sentences.firstOrNull { at == it.end }
        ?: return null
    val inner = at - sentence.start
    val token = sentence.tokens.firstOrNull { it.tappable && inner >= it.start && inner < it.end }
        ?: sentence.tokens.firstOrNull { it.tappable && inner == it.end }
        ?: return null
    return TapTarget(
        blockOrdinal = block.ordinal,
        sentenceIndex = sentence.index,
        tokenIndex = token.index
    )
}

/**
 * Один блок книги в режиме скролла.
 *
 * ⚠️ Промах по пунктуации ничего не открывает, и это верно: `tappable = false` у сервера значит
 * «здесь нечего смотреть», а не «попробуйте точнее».
 */
@Composable
private fun BlockText(
    block: BlockDto,
    selected: TapTarget?,
    onTap: (TapTarget) -> Unit
) {
    val colors = WaveTheme.colors
    val text = remember(block, selected) {
        buildAnnotatedString { appendBlock(block, selected, colors) }
    }

    val isHeading = block.kind == "HEADING"
    val isQuote = block.kind == "QUOTE"
    val isItem = block.kind == "LIST_ITEM"

    var layout by remember(block) { mutableStateOf<TextLayoutResult?>(null) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = if (isHeading) 24.dp else 0.dp,
                bottom = if (isHeading) 10.dp else 14.dp
            )
    ) {
        if (isQuote) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .heightIn(min = 20.dp)
                    .fillMaxHeight()
                    .background(colors.secondary.copy(alpha = 0.35f))
            )
            Spacer(Modifier.width(12.dp))
        }
        if (isItem) {
            // Маркер отдельным элементом: внутри строки он сдвинул бы все смещения на два символа.
            Text("—", fontSize = 18.sp, lineHeight = 31.sp, color = colors.secondary)
            Spacer(Modifier.width(8.dp))
        }

        Text(
            text = text,
            fontFamily = if (isHeading) Comfortaa else null,
            fontSize = if (isHeading) 22.sp else 18.sp,
            lineHeight = if (isHeading) 28.sp else 31.sp,
            fontWeight = if (isHeading) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (isQuote) FontStyle.Italic else FontStyle.Normal,
            color = colors.textPrimary,
            onTextLayout = { layout = it },
            modifier = Modifier.pointerInput(block) {
                detectTapGestures { position ->
                    val result = layout ?: return@detectTapGestures
                    locateInBlock(block, result.getOffsetForPosition(position))?.let(onTap)
                }
            }
        )
    }
}

// ── Лист разбора ──────────────────────────────────────────────────────

/**
 * Лист с разбором слова. Поверх текста, потому что читать под ним продолжают.
 *
 * Закрывается свайпом вниз — тем же жестом, что и любой лист в системе. Кнопка «Закрыть»
 * занимала целую строку ради действия, которое палец делает сам.
 */
@Composable
private fun HintSheet(
    state: ReaderState,
    viewModel: ReaderViewModel,
    onOpenArticle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = WaveTheme.colors
    var dragged by remember(state.target) { mutableStateOf(0f) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(state.target) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragged > 120f) viewModel.closeTap()
                        dragged = 0f
                    },
                    onVerticalDrag = { change, amount ->
                        change.consume()
                        dragged = (dragged + amount).coerceAtLeast(0f)
                    }
                )
            }
            .graphicsLayer { translationY = dragged },
        color = colors.background,
        shadowElevation = 0.dp
    ) {
        Column {
            HorizontalDivider(thickness = 1.dp, color = colors.hairline)
            // Ручка вместо кнопки: она же и говорит, что лист тянется.
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .align(Alignment.CenterHorizontally)
                    .width(36.dp)
                    .height(4.dp)
                    .background(colors.border, MaterialTheme.shapes.small)
            )
            Box(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp)
                    .navigationBarsPadding()
            ) {
                ContextCard(
                    hint = state.hint,
                    isHinting = state.isHinting,
                    analysis = state.analysis,
                    isAnalyzing = state.isAnalyzing,
                    onDetails = viewModel::loadDetails,
                    canSave = true,
                    saved = state.currentSaved,
                    saving = state.isSaving,
                    saveHint = state.bookFolderName,
                    onSave = {
                        if (state.currentSaved) viewModel.unsave() else viewModel.saveQuietly()
                    },
                    onChooseFolders = viewModel::openFolderSheet,
                    onOpenArticle = onOpenArticle
                )
            }
        }
    }
}
