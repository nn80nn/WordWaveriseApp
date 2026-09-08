package com.wordwaverise.wordwaveriseapp.presentation.saved

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordwaverise.wordwaveriseapp.data.local.entity.SavedWordEntity
import com.wordwaverise.wordwaveriseapp.R
import com.wordwaverise.wordwaveriseapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(
    state: SavedWordsState,
    onDeleteWord: (SavedWordEntity) -> Unit,
    onWordClick: (String) -> Unit,
    onSelectCategory: (Long?) -> Unit,
    onShowCategorySheet: () -> Unit,
    onHideCategorySheet: () -> Unit,
    onSetWordToFile: (SavedWordEntity) -> Unit,
    onToggleFolder: (Long) -> Unit,
    onSaveFolders: () -> Unit,
    onCreateCategory: () -> Unit,
    onDeleteCategory: (id: Long, serverId: Int?) -> Unit,
    onRenameCategory: (id: Long, serverId: Int?, name: String) -> Unit,
    onShareCategory: (serverId: Int?) -> Unit,
    onImportLinkChange: (String) -> Unit,
    onImportFolder: () -> Unit,
    onNewCategoryNameChange: (String) -> Unit,
    /** Поиск по своим словам. */
    onSearchChange: (String) -> Unit,
    onSortChange: (WordSort) -> Unit,
    /** Поиск по папкам — только пока открыт лист папок. */
    onFolderQueryChange: (String) -> Unit,
    onFolderSortChange: (FolderSort) -> Unit,
    /** Папка-группа, внутрь которой создаётся следующая папка (её серверный id, или null). */
    onNewCategoryParentChange: (Int?) -> Unit,
    /** Вложить папку в папку-группу или вынуть обратно. */
    onSetCategoryParent: (id: Long, serverId: Int?, parentServerId: Int?) -> Unit,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .waveSurface()
    ) {
        if (state.isOffline) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .background(Warning.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.CloudOff, contentDescription = null, tint = Warning, modifier = Modifier.size(16.dp))
                Text(stringResource(R.string.net_interneta_pokazany_sohranennye_dannye), fontSize = 12.sp, color = Warning)
            }
        }

        // Поиск и порядок. Словарь растёт без потолка, и на пятой сотне слов «найти слово»
        // перестаёт быть прокруткой — это и есть причина, по которой строка стоит выше папок.
        WordSearchRow(
            query = state.searchQuery,
            sort = state.sortBy,
            onQueryChange = onSearchChange,
            onSortChange = onSortChange
        )

        // Первый уровень: только корневые папки. Содержимое выбранной группы раскрывается
        // рядом ниже — одна лента со всем сразу уезжала на два экрана вбок, и по ней не было
        // видно ни вложенности, ни того, что у группы вообще есть содержимое.
        val counts = state.wordCounts
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FolderChip(
                        label = stringResource(R.string.vse),
                        count = state.words.count { !it.readOnly },
                        selected = state.selectedCategoryId == null,
                        onClick = { onSelectCategory(null) }
                    )
                }
                items(state.rootFolders, key = { it.folder.id }) { node ->
                    val cat = node.folder
                    FolderChip(
                        label = cat.name,
                        count = counts[cat.id] ?: 0,
                        selected = state.selectedCategoryId == cat.id,
                        fromGroup = cat.groupServerId != null,
                        // Значок папки стоит у той, внутри которой лежат другие: без него
                        // единственным признаком группы был бы счётчик, а он у обычной папки
                        // выглядит точно так же.
                        hasChildren = node.children.isNotEmpty(),
                        fromBook = cat.bookServerId != null,
                        onClick = { onSelectCategory(cat.id) }
                    )
                }
                if (state.looseCount > 0) {
                    item {
                        FolderChip(
                            label = stringResource(R.string.bez_kategorii),
                            count = state.looseCount,
                            selected = state.selectedCategoryId == SavedWordsState.UNCATEGORIZED,
                            onClick = { onSelectCategory(SavedWordsState.UNCATEGORIZED) }
                        )
                    }
                }
            }
            IconButton(onClick = onShowCategorySheet) {
                Icon(Icons.Default.Folder, contentDescription = stringResource(R.string.kategorii), tint = TextTertiary)
            }
        }

        // Второй уровень. Черта слева и имя родителя — это и есть «папка лежит в папке»:
        // связь видна и тогда, когда ряд прокручен на середину, где родителя уже не видно.
        val openGroup = state.openGroup
        val openChildren = state.openChildren
        if (openGroup != null && openChildren.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .padding(vertical = 6.dp)
                        .background(PrimaryCyan.copy(alpha = 0.4f))
                )
                Text(
                    text = openGroup.name,
                    fontSize = 11.sp,
                    color = TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .widthIn(max = 88.dp)
                        .padding(horizontal = 8.dp)
                )
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FolderChip(
                            label = "Всё вместе",
                            count = counts[openGroup.id] ?: 0,
                            selected = state.selectedCategoryId == openGroup.id,
                            onClick = { onSelectCategory(openGroup.id) }
                        )
                    }
                    items(openChildren, key = { it.id }) { cat ->
                        FolderChip(
                            label = cat.name,
                            count = counts[cat.id] ?: 0,
                            selected = state.selectedCategoryId == cat.id,
                            fromGroup = cat.groupServerId != null,
                            fromBook = cat.bookServerId != null,
                            onClick = { onSelectCategory(cat.id) }
                        )
                    }
                }
            }
        }
        // Отбор и сортировка считаются один раз на композицию, а не на каждое обращение:
        // это геттер, а список — весь словарь человека.
        val shown = state.filteredWords
        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryCyan)
                }
            }
            shown.isEmpty() -> EmptyState(
                searching = state.searchQuery.isNotBlank() || state.selectedCategoryId != null
            )
            else -> {
                Text(
                    text = "${shown.size} ${wordCountLabel(shown.size)}",
                    fontSize = 13.sp,
                    color = TextTertiary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // ⚠️ Ключ — id записи, а не написание: одно слово встречается столько
                        // раз, сколько значений человек отметил, и по написанию Compose счёл
                        // бы их одной строкой.
                        items(shown, key = { it.id }) { word ->
                            WordCard(
                                word = word,
                                // Первая папка: на карточке место ровно на одну подпись, а
                                // остальные видно по чипам сверху.
                                categoryName = state.categories.find { it.id == word.categoryId }?.name,
                                // Слово из папки класса не наше: строка принадлежит учителю,
                                // и переставлять чужой словарь этот экран не вправе.
                                onDelete = if (word.readOnly) null else {
                                    { onDeleteWord(word) }
                                },
                                onClick = { onWordClick(word.word) },
                                onLongClick = if (word.readOnly) null else {
                                    { onSetWordToFile(word) }
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }

    // Bottom sheet: manage categories OR move word to category
    if (state.showCategorySheet) {
        ModalBottomSheet(
            onDismissRequest = onHideCategorySheet,
            containerColor = BackgroundSecondary,
            // ⚠️ Лист должен уступать клавиатуре, а не оставаться под ней. Поля ввода здесь
            // внизу — без этого человек печатал вслепую: клавиатура закрывала и само поле, и
            // кнопку рядом с ним, а понять, что лист вообще прокручивается, было неоткуда.
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            val filing = state.entryToFile
            if (filing != null) {
                // Флажки, а не выбор одного: слово лежит в стольких папках, в скольких оно и
                // правда нужно. ⚠️ Папок класса в списке нет — писать в чужой словарь нельзя.
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        "Папки для «${filing.word}»",
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    if (state.ownCategories.size >= 6) {
                        FolderSearchRow(
                            query = state.folderQuery,
                            sort = state.folderSort,
                            onQueryChange = onFolderQueryChange,
                            onSortChange = onFolderSortChange
                        )
                    }
                    state.ownFolderRows.forEach { row ->
                        val cat = row.folder
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleFolder(cat.id) }
                                .padding(start = if (row.nested) 20.dp else 0.dp)
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = cat.id in state.chosenFolders,
                                onCheckedChange = { onToggleFolder(cat.id) }
                            )
                            Text(cat.name, color = TextPrimary, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                    if (state.ownCategories.isNotEmpty() && state.ownFolderRows.isEmpty()) {
                        Text(
                            "Ничего не нашлось",
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    if (state.ownCategories.isEmpty()) {
                        Text(
                            "Сначала создайте папку",
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    Text(
                        if (state.chosenFolders.isEmpty()) stringResource(R.string.bez_kategorii)
                        else "Слово будет в ${state.chosenFolders.size} папк(ах)",
                        fontSize = 12.sp, color = TextSecondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Button(
                        onClick = onSaveFolders,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text("Сохранить")
                    }
                }
            } else {
                // Manage categories mode
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        stringResource(R.string.kategorii),
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    // Имя правится на месте: папку переименовывают, глядя на соседние, и
                    // уводить ради одной строки в отдельный диалог незачем.
                    var renamingId by remember { mutableStateOf<Long?>(null) }
                    var renameDraft by remember { mutableStateOf("") }

                    if (state.categories.size >= 6) {
                        FolderSearchRow(
                            query = state.folderQuery,
                            sort = state.folderSort,
                            onQueryChange = onFolderQueryChange,
                            onSortChange = onFolderSortChange
                        )
                    }
                    state.folderRows.forEach { row ->
                        val cat = row.folder
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = if (row.nested) 20.dp else 0.dp)
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (renamingId == cat.id) {
                                OutlinedTextField(
                                    value = renameDraft,
                                    onValueChange = { renameDraft = it },
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    onRenameCategory(cat.id, cat.serverId, renameDraft)
                                    renamingId = null
                                }) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = stringResource(R.string.sohranit),
                                        tint = PrimaryCyan
                                    )
                                }
                                IconButton(onClick = { renamingId = null }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(R.string.otmena),
                                        tint = TextTertiary
                                    )
                                }
                            } else {
                                Text(
                                    cat.name,
                                    fontSize = 15.sp,
                                    color = TextPrimary,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { renamingId = cat.id; renameDraft = cat.name }
                                )
                                // Кнопка появляется только там, где есть из чего выбирать: вложенность
                                // ровно одна, и у папки, в которой уже лежат другие, родителя быть не
                                // может — предложить его значило бы вести к отказу, который нечем объяснить.
                                if (!cat.readOnly && state.childCount(cat) == 0 &&
                                    state.groupCandidates.any { it.id != cat.id }
                                ) {
                                    var menuOpen by remember(cat.id) { mutableStateOf(false) }
                                    Box {
                                        IconButton(onClick = { menuOpen = true }) {
                                            Icon(
                                                Icons.Default.DriveFileMove,
                                                contentDescription = "В какой папке-группе лежит",
                                                tint = if (cat.parentServerId != null) WaveTheme.colors.brass
                                                       else TextTertiary.copy(alpha = 0.6f)
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = menuOpen,
                                            onDismissRequest = { menuOpen = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Вне группы") },
                                                onClick = {
                                                    menuOpen = false
                                                    onSetCategoryParent(cat.id, cat.serverId, null)
                                                }
                                            )
                                            state.groupCandidates
                                                .filter { it.id != cat.id }
                                                .forEach { group ->
                                                    DropdownMenuItem(
                                                        text = { Text(group.name) },
                                                        onClick = {
                                                            menuOpen = false
                                                            onSetCategoryParent(cat.id, cat.serverId, group.serverId)
                                                        }
                                                    )
                                                }
                                        }
                                    }
                                }
                                IconButton(onClick = { renamingId = cat.id; renameDraft = cat.name }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = stringResource(R.string.pereimenovat_papku),
                                        tint = TextTertiary.copy(alpha = 0.6f)
                                    )
                                }
                                IconButton(onClick = { onShareCategory(cat.serverId) }) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = stringResource(R.string.podelitsya_papkoy),
                                        tint = TextTertiary.copy(alpha = 0.6f)
                                    )
                                }
                                IconButton(onClick = { onDeleteCategory(cat.id, cat.serverId) }) {
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.udalit), tint = TextTertiary.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = BackgroundLight)
                    Spacer(modifier = Modifier.height(12.dp))

                    // ── Чужая папка по ссылке ────────────────────────────
                    // Принимается и ссылка целиком, и один код: человек вставляет то, что ему
                    // прислали, а не то, что удобно разобрать нам.
                    Text(
                        stringResource(R.string.dobavit_papku_po_ssylke),
                        fontSize = 14.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = state.importLink,
                            onValueChange = onImportLinkChange,
                            placeholder = { Text(stringResource(R.string.ssylka_na_papku), color = TextTertiary) },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = onImportFolder,
                            enabled = state.importLink.isNotBlank() && !state.importing,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)
                        ) {
                            Text(stringResource(R.string.dobavit), color = WaveTheme.colors.onAccent)
                        }
                    }
                    state.importMessage?.let { message ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(message, fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = BackgroundLight)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Create new category
                    Text(stringResource(R.string.novaya_kategoriya), fontSize = 14.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = state.newCategoryName,
                            onValueChange = onNewCategoryNameChange,
                            placeholder = { Text(stringResource(R.string.nazvanie), color = TextTertiary) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryCyan,
                                unfocusedBorderColor = BackgroundLight,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        if (state.groupCandidates.isNotEmpty()) {
                            var parentMenuOpen by remember { mutableStateOf(false) }
                            val chosenGroup = state.groupCandidates
                                .firstOrNull { it.serverId == state.newCategoryParentServerId }
                            Box {
                                TextButton(onClick = { parentMenuOpen = true }) {
                                    Text(
                                        chosenGroup?.name ?: "вне группы",
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (chosenGroup != null) WaveTheme.colors.brass else TextTertiary
                                    )
                                }
                                DropdownMenu(
                                    expanded = parentMenuOpen,
                                    onDismissRequest = { parentMenuOpen = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Вне группы") },
                                        onClick = { parentMenuOpen = false; onNewCategoryParentChange(null) }
                                    )
                                    state.groupCandidates.forEach { group ->
                                        DropdownMenuItem(
                                            text = { Text(group.name) },
                                            onClick = {
                                                parentMenuOpen = false
                                                onNewCategoryParentChange(group.serverId)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        IconButton(
                            onClick = onCreateCategory,
                            enabled = state.newCategoryName.isNotBlank()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.sozdat), tint = if (state.newCategoryName.isNotBlank()) PrimaryCyan else TextTertiary)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Поиск и порядок папок — там же, где список, к которому относятся.
 *
 * ⚠️ Порядок общий на все места, где папки видно: одна и та же папка не должна стоять третьей
 * в фильтре и седьмой в этом листе. Поиск, наоборот, живёт только пока лист открыт — он про
 * «сейчас найти», и оставленный включённым прятал бы папки на экране без строки поиска.
 */
@Composable
private fun FolderSearchRow(
    query: String,
    sort: FolderSort,
    onQueryChange: (String) -> Unit,
    onSortChange: (FolderSort) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Поиск папки", color = TextTertiary, fontSize = 14.sp) },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryCyan,
                unfocusedBorderColor = BackgroundLight,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )
        var menuOpen by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "Порядок папок",
                    tint = if (sort == FolderSort.CUSTOM) TextTertiary else PrimaryCyan
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                listOf(
                    FolderSort.CUSTOM to "Как заведены",
                    FolderSort.A_Z to "А–Я",
                    FolderSort.Z_A to "Я–А",
                    FolderSort.COUNT to "По числу слов"
                ).forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label, color = if (value == sort) PrimaryCyan else TextPrimary) },
                        onClick = { menuOpen = false; onSortChange(value) }
                    )
                }
            }
        }
    }
}

/**
 * Строка поиска по словам и порядок списка.
 *
 * ⚠️ Поиск и сортировка — одно поле ввода и одна кнопка, а не отдельный экран настроек: они
 * относятся к списку под ними и должны быть видны вместе с ним. Порядок живёт в меню, потому
 * что вариантов четыре, а места в строке — на один контрол.
 */
@Composable
private fun WordSearchRow(
    query: String,
    sort: WordSort,
    onQueryChange: (String) -> Unit,
    onSortChange: (WordSort) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Поиск по словам", color = TextTertiary, fontSize = 14.sp) },
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(18.dp))
            },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Очистить", tint = TextTertiary, modifier = Modifier.size(18.dp))
                    }
                }
            } else null,
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryCyan,
                unfocusedBorderColor = BackgroundLight,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )
        var menuOpen by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "Порядок слов",
                    tint = if (sort == WordSort.NEWEST) TextTertiary else PrimaryCyan
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                listOf(
                    WordSort.NEWEST to "Сначала новые",
                    WordSort.OLDEST to "Сначала старые",
                    WordSort.A_Z to "A–Z",
                    WordSort.Z_A to "Z–A"
                ).forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label, color = if (value == sort) PrimaryCyan else TextPrimary) },
                        onClick = { menuOpen = false; onSortChange(value) }
                    )
                }
            }
        }
    }
}

/**
 * Чип папки: имя, число слов и — у папки-группы — значок того, что внутри лежат другие.
 *
 * ⚠️ Число здесь не украшение: до того как в папку зашли, оно единственное отличает полную
 * от пустой. У группы оно считается вместе с вложенными — ровно так же, как их достаёт фильтр.
 */
@Composable
private fun FolderChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    fromGroup: Boolean = false,
    hasChildren: Boolean = false,
    fromBook: Boolean = false
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = if (count > 0) "$label · $count" else label,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        // Папка от класса подписана значком: цветом в чипе уже сказано, выбран он или нет,
        // и второго смысла та же краска не выдержит.
        leadingIcon = when {
            fromGroup -> {
                {
                    Icon(
                        imageVector = Icons.Outlined.Group,
                        contentDescription = null,
                        tint = WaveTheme.colors.brass,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            // Папка книги узнаётся значком, а не названием: и книгу, и папку человек волен
            // переименовать, а «слова из The Hobbit» и «Урок 5» — разные вещи в одном ряду.
            fromBook -> {
                {
                    Icon(
                        imageVector = Icons.Outlined.AutoStories,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            hasChildren -> {
                {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            else -> null
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = PrimaryCyan.copy(alpha = 0.2f),
            selectedLabelColor = PrimaryCyan
        ),
        modifier = Modifier.widthIn(max = 220.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WordCard(
    word: SavedWordEntity,
    categoryName: String?,
    onDelete: (() -> Unit)?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
        border = BorderStroke(1.dp, WaveTheme.colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = word.word,
                    fontFamily = Comfortaa,
                    fontSize = 19.sp,
                    letterSpacing = (-0.4).sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                // Перевод — то, чем одна запись отличается от другой: у `resolve` их столько,
                // сколько значений человек отметил, и без этой строки они неразличимы.
                //
                // Одна строка с многоточием, как у чипа: перевод бывает длинным («решимость,
                // твёрдость духа, воля»), а карточка в списке не должна расти под него — тогда
                // соседние слова уезжают с экрана ради подробности, за которой открывают статью.
                word.translation?.takeIf { it.isNotBlank() }?.let { translation ->
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = translation,
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Medium,
                        color = WaveTheme.colors.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(formatDate(word.savedAt), fontSize = 12.sp, color = TextTertiary)
                    // Only the exception is worth a mark. Confirming the normal
                    // state on every row just adds noise to the list.
                    if (!word.isSynced) {
                        Text("·", fontSize = 12.sp, color = TextTertiary)
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = stringResource(R.string.ne_sinhronizirovano),
                            tint = TextTertiary,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                    if (categoryName != null) {
                        Text("·", fontSize = 12.sp, color = TextTertiary)
                        // Слово из папки класса подписано другим цветом: бирюзовым уже названа
                        // своя папка, и одной краской это читалось бы как «две папки».
                        Text(
                            text = categoryName,
                            fontSize = 11.sp,
                            color = if (word.readOnly) WaveTheme.colors.brass else PrimaryCyan
                        )
                    }
                    // Пометки о выбранном значении здесь больше нет: значение теперь есть у
                    // каждой записи, и значок, стоящий на всех строках без исключения, не
                    // отличает ни одну из них — он просто занимает место в каждой.
                }
            }
            // Слово из папки класса удалить нельзя — см. вызов выше.
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.udalit),
                        tint = TextTertiary.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(searching: Boolean = false) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = if (searching) Icons.Default.Search else Icons.Default.Book,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(64.dp)
            )
            // Пустой поиск и пустой словарь — разные состояния, и совет «найдите слово и
            // нажмите на звезду» человеку, который только что искал в своём словаре, отвечает
            // не на его вопрос.
            Text(
                if (searching) "Ничего не нашлось" else stringResource(R.string.net_sohranennyh_slov),
                fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary
            )
            Text(
                if (searching) "Попробуйте другое написание или снимите фильтр папки"
                else stringResource(R.string.naydite_slovo_i_nazhmite_na_zvezdu_chtoby),
                fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center, lineHeight = 20.sp
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale("ru"))
    return sdf.format(Date(timestamp))
}

private fun wordCountLabel(count: Int): String = when {
    count % 10 == 1 && count % 100 != 11 -> "слово"
    count % 10 in 2..4 && count % 100 !in 12..14 -> "слова"
    else -> "слов"
}
