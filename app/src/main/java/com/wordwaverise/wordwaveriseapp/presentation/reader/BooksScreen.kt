package com.wordwaverise.wordwaveriseapp.presentation.reader

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.BookDto
import com.wordwaverise.wordwaveriseapp.ui.theme.Comfortaa
import com.wordwaverise.wordwaveriseapp.ui.theme.Eyebrow
import com.wordwaverise.wordwaveriseapp.ui.theme.WaveTheme
import com.wordwaverise.wordwaveriseapp.ui.theme.waveSurface
import kotlin.math.roundToInt

/**
 * Полка.
 *
 * Обложек нет и не будет: книга хранится нормализованным текстом, а не файлом. Поэтому карточка —
 * это название, автор и то единственное, что про книгу хочется знать между заходами: докуда
 * дочитано.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksScreen(
    /** Книга, открытая в чужом приложении: загружается сама, как только экран появился. */
    incomingFile: String? = null,
    onOpenBook: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BooksViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val colors = WaveTheme.colors
    val snackbar = remember { SnackbarHostState() }

    // Пикер зовётся со звёздочкой вместо списка MIME-типов: у FB2 тип непостоянен между
    // файловыми провайдерами (то application/xml, то application/octet-stream, то пусто), а
    // формат сервер определяет по байтам. Список здесь прятал бы ровно те файлы, ради которых
    // читалку и открыли.
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) viewModel.importFile(uri) }

    // Полка перечитывается при каждом возврате: прогресс книги меняется в читалке, а не
    // здесь, и без этого шкала показывала бы то, что было до чтения.
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    LaunchedEffect(incomingFile) {
        // Нажатие по книге — это уже просьба её открыть, поэтому подтверждения не спрашиваем:
        // это был бы второй вопрос про то же самое.
        val uri = incomingFile?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        viewModel.importFile(android.net.Uri.parse(uri))
    }

    LaunchedEffect(state.openBookId) {
        val id = state.openBookId ?: return@LaunchedEffect
        viewModel.openHandled()
        onOpenBook(id)
    }

    LaunchedEffect(state.notice, state.error) {
        val message = state.notice ?: state.error ?: return@LaunchedEffect
        snackbar.showSnackbar(message)
        viewModel.clearNotice()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .waveSurface()
                .padding(padding)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Eyebrow(text = "ЧИТАТЬ ПО-АНГЛИЙСКИ")
                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { picker.launch(arrayOf("*/*")) },
                        enabled = !state.isImporting,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.secondary,
                            contentColor = colors.onAccent
                        )
                    ) {
                        Text("Загрузить файл", fontSize = 14.sp)
                    }
                    OutlinedButton(
                        onClick = { viewModel.showPaste(true) },
                        enabled = !state.isImporting,
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                    ) {
                        Text("Вставить текст", fontSize = 14.sp, color = colors.textPrimary)
                    }
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    text = "EPUB, FB2 (в том числе .fb2.zip), PDF, TXT и HTML. До 32 МБ.",
                    fontSize = 12.sp,
                    color = colors.textMuted
                )

                if (state.isImporting) {
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.secondary,
                        trackColor = colors.tag
                    )
                }
            }

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.secondary)
                }

                state.books.isEmpty() -> EmptyShelf()

                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.books, key = { it.id }) { book ->
                        BookCard(
                            book = book,
                            onOpen = { onOpenBook(book.id) },
                            onDelete = { viewModel.askDelete(book) }
                        )
                    }
                }
            }
        }
    }

    if (state.pasteOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.showPaste(false) },
            shape = RoundedCornerShape(24.dp),
            containerColor = colors.surface,
            title = {
                Text(
                    "Вставить текст",
                    fontFamily = Comfortaa,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = state.pasteTitle,
                        onValueChange = viewModel::setPasteTitle,
                        placeholder = { Text("Название (необязательно)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.pasteText,
                        onValueChange = viewModel::setPasteText,
                        placeholder = { Text("Вставьте английский текст…") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp, max = 260.dp)
                    )
                    // ⚠️ Название входит в хэш: тот же текст под другим названием — другая книга.
                    Text(
                        "Без названия оно возьмётся из первых слов текста.",
                        fontSize = 12.sp,
                        color = colors.textMuted
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::importPasted,
                    enabled = state.pasteText.isNotBlank() && !state.isImporting
                ) {
                    Text("Добавить", color = colors.secondary)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showPaste(false) }) {
                    Text("Отмена", color = colors.textMuted)
                }
            }
        )
    }

    state.confirmDelete?.let { book ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            shape = RoundedCornerShape(24.dp),
            containerColor = colors.surface,
            title = {
                Text(
                    "Удалить книгу?",
                    fontFamily = Comfortaa,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    "«${book.title}» исчезнет с полки вместе с местом, где вы остановились. " +
                        "Сохранённые из неё слова и её папка останутся.",
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::askDeleteAgain) {
                    Text("Удалить", color = colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDelete) {
                    Text("Отмена", color = colors.textMuted)
                }
            }
        )
    }

    // Второй вопрос — про то же, но другими словами и с другим ответом: «удалить» здесь стоит
    // там, где у первого диалога стояла «отмена», поэтому дважды промахнуться одним движением
    // нельзя. Корзина живёт в строке книги, и промах пальцем стоил бы книги целиком.
    state.confirmDeleteAgain?.let { book ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            shape = RoundedCornerShape(24.dp),
            containerColor = colors.surface,
            title = {
                Text(
                    "Точно удалить?",
                    fontFamily = Comfortaa,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = colors.error
                )
            },
            text = {
                Text(
                    "Это последнее подтверждение. Вернуть «${book.title}» можно будет только " +
                        "новой загрузкой файла, и читать её придётся с начала.",
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::cancelDelete) {
                    Text("Оставить", color = colors.secondary)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text("Удалить навсегда", color = colors.error)
                }
            }
        )
    }
}

@Composable
private fun BookCard(book: BookDto, onOpen: () -> Unit, onDelete: () -> Unit) {
    val colors = WaveTheme.colors
    val percent = ((book.position?.progress ?: 0.0) * 100).roundToInt().coerceIn(0, 100)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onOpen)
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    fontFamily = Comfortaa,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildString {
                        book.author?.let { append(it).append(" · ") }
                        append("${book.wordCount} слов")
                    },
                    fontSize = 12.sp,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(colors.tag)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(
                                    if (book.position == null) 0f
                                    else (percent / 100f).coerceAtLeast(0.02f)
                                )
                                .clip(CircleShape)
                                .background(colors.secondary)
                        )
                    }
                    Text(
                        text = if (book.position == null) "не начата" else "$percent%",
                        fontSize = 11.sp,
                        color = colors.textMuted
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Удалить книгу",
                    tint = colors.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyShelf() {
    val colors = WaveTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(colors.tag),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoStories,
                contentDescription = null,
                tint = colors.secondary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            "Полка пока пуста",
            fontFamily = Comfortaa,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = colors.textPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Загрузите книгу или вставьте любой английский текст — статью, письмо, главу. " +
                "В нём можно будет нажать на любое слово и разобрать его прямо в предложении.",
            fontSize = 14.sp,
            color = colors.textSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
