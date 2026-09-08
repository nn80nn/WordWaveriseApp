package com.wordwaverise.wordwaveriseapp.presentation.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.BlockDto
import com.wordwaverise.wordwaveriseapp.presentation.components.SaveToFolderDialog
import com.wordwaverise.wordwaveriseapp.presentation.search.components.ContextCard
import com.wordwaverise.wordwaveriseapp.ui.theme.Comfortaa
import com.wordwaverise.wordwaveriseapp.ui.theme.WaveTheme
import com.wordwaverise.wordwaveriseapp.ui.theme.waveSurface
import kotlin.math.roundToInt

/**
 * Чтение.
 *
 * Слово выбирается нажатием прямо в тексте: каждое предложение — это `ClickableText`, у которого
 * позиция символа переводится в токен по смещениям, уже приехавшим с сервера. Отдельного элемента
 * на слово нет — на окно их вышли бы тысячи, и текст перестал бы быть текстом.
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
    val listState = rememberLazyListState()
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

    // Позиция — верхний видимый блок; хвост ленты подтягивает следующее окно.
    LaunchedEffect(listState, state.blocks.size) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.layoutInfo.visibleItemsInfo.size }
            .collect { (first, _) ->
                state.blocks.getOrNull(first)?.let { viewModel.savePosition(it.ordinal) }
                val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                if (last >= state.blocks.size - 6) viewModel.loadMore()
            }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column(modifier = Modifier.background(colors.background)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
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
                            text = state.book?.title.orEmpty(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val percent = ((state.book?.position?.progress ?: 0.0) * 100).roundToInt()
                        val chapter = state.chapters
                            .firstOrNull { it.index == state.book?.position?.chapterIndex }
                            ?.title
                        if (chapter != null || percent > 0) {
                            Text(
                                text = listOfNotNull(chapter, "$percent%").joinToString(" · "),
                                fontSize = 12.sp,
                                color = colors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (state.chapters.size > 1) {
                        IconButton(onClick = { contentsOpen = true }) {
                            Icon(
                                Icons.AutoMirrored.Outlined.List,
                                contentDescription = "Оглавление",
                                tint = colors.textSecondary
                            )
                        }
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = colors.hairline)
            }
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

                else -> LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 8.dp,
                        bottom = 120.dp
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
                        item {
                            Text(
                                text = "Конец книги",
                                fontSize = 12.sp,
                                color = colors.textMuted,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Лист разбора. Поверх текста, потому что читать под ним продолжают.
            if (state.target != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    color = colors.background,
                    shadowElevation = 0.dp
                ) {
                    Column {
                        HorizontalDivider(thickness = 1.dp, color = colors.hairline)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = viewModel::closeTap) {
                                Text("Закрыть", fontSize = 13.sp, color = colors.textMuted)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .heightIn(max = 340.dp)
                                .padding(horizontal = 16.dp)
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
                                    if (state.currentSaved) viewModel.unsave()
                                    else viewModel.saveQuietly()
                                },
                                onChooseFolders = viewModel::openFolderSheet,
                                onOpenArticle = onOpenArticle
                            )
                        }
                    }
                }
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

/**
 * Один блок книги. Слово находится по позиции символа — токены дают точное соответствие.
 *
 * Отдельного элемента на слово нет: на окно их вышли бы тысячи, и текст перестал бы быть текстом.
 * Собранная строка **посимвольно равна** `block.text`, поэтому смещение из `TextLayoutResult`
 * прикладывается к предложениям и токенам напрямую, без поправок.
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
        buildAnnotatedString {
            if (block.sentences.isEmpty()) {
                append(block.text)
                return@buildAnnotatedString
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
        }
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
            Text(
                text = "—",
                fontSize = 18.sp,
                lineHeight = 31.sp,
                color = colors.secondary
            )
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
                    val at = result.getOffsetForPosition(position)
                    val target = locate(block, at) ?: return@detectTapGestures
                    onTap(target)
                }
            }
        )
    }
}

/**
 * Смещение в тексте блока → предложение и токен.
 *
 * Каретка встаёт между символами, поэтому у левого края слова смещение совпадает с его началом,
 * а у правого — с концом предыдущего: второй случай засчитывается слову слева, иначе тап по
 * концу слова читался бы как промах.
 */
private fun locate(block: BlockDto, at: Int): TapTarget? {
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
