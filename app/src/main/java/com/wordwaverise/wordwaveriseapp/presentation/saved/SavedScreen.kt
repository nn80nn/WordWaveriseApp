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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
    onDeleteWord: (String) -> Unit,
    onWordClick: (String) -> Unit,
    onSelectCategory: (Long?) -> Unit,
    onShowCategorySheet: () -> Unit,
    onHideCategorySheet: () -> Unit,
    onSetWordToMove: (String) -> Unit,
    onMoveWordToCategory: (word: String, catLocalId: Long?, catServerId: Int?) -> Unit,
    onCreateCategory: () -> Unit,
    onDeleteCategory: (id: Long, serverId: Int?) -> Unit,
    onRenameCategory: (id: Long, serverId: Int?, name: String) -> Unit,
    onShareCategory: (serverId: Int?) -> Unit,
    onImportLinkChange: (String) -> Unit,
    onImportFolder: () -> Unit,
    onNewCategoryNameChange: (String) -> Unit,
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

        // Category filter row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.selectedCategoryId == null,
                        onClick = { onSelectCategory(null) },
                        label = { Text(stringResource(R.string.vse), fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryCyan.copy(alpha = 0.2f),
                            selectedLabelColor = PrimaryCyan
                        )
                    )
                }
                items(state.categories) { cat ->
                    FilterChip(
                        selected = state.selectedCategoryId == cat.id,
                        onClick = { onSelectCategory(cat.id) },
                        label = { Text(cat.name, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryCyan.copy(alpha = 0.2f),
                            selectedLabelColor = PrimaryCyan
                        )
                    )
                }
            }
            IconButton(onClick = onShowCategorySheet) {
                Icon(Icons.Default.Folder, contentDescription = stringResource(R.string.kategorii), tint = TextTertiary)
            }
        }

        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryCyan)
                }
            }
            state.filteredWords.isEmpty() -> EmptyState()
            else -> {
                Text(
                    text = "${state.filteredWords.size} ${wordCountLabel(state.filteredWords.size)}",
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
                        items(state.filteredWords, key = { it.word }) { word ->
                            WordCard(
                                word = word,
                                categoryName = state.categories.find { it.id == word.categoryId }?.name,
                                onDelete = { onDeleteWord(word.word) },
                                onClick = { onWordClick(word.word) },
                                onLongClick = { onSetWordToMove(word.word) }
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
            containerColor = BackgroundSecondary
        ) {
            if (state.wordToMove != null) {
                // Move word mode
                Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
                    Text(
                        "Переместить «${state.wordToMove}» в категорию",
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    // No category option
                    TextButton(
                        onClick = { onMoveWordToCategory(state.wordToMove, null, null) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.bez_kategorii), color = TextSecondary, modifier = Modifier.fillMaxWidth())
                    }
                    state.categories.forEach { cat ->
                        TextButton(
                            onClick = { onMoveWordToCategory(state.wordToMove, cat.id, cat.serverId) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(cat.name, color = TextPrimary, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            } else {
                // Manage categories mode
                Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
                    Text(
                        stringResource(R.string.kategorii),
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    // Имя правится на месте: папку переименовывают, глядя на соседние, и
                    // уводить ради одной строки в отдельный диалог незачем.
                    var renamingId by remember { mutableStateOf<Long?>(null) }
                    var renameDraft by remember { mutableStateOf("") }

                    state.categories.forEach { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WordCard(
    word: SavedWordEntity,
    categoryName: String?,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
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
                        Text(categoryName, fontSize = 11.sp, color = PrimaryCyan)
                    }
                    // Слово сохранено с выбранным значением: при открытии статья начнётся с него.
                    if (word.senseId != null) {
                        Text("·", fontSize = 12.sp, color = TextTertiary)
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = stringResource(R.string.vashe_znachenie),
                            tint = PrimaryCyan,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.udalit), tint = TextTertiary.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Book,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(64.dp)
            )
            Text(stringResource(R.string.net_sohranennyh_slov), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(
                stringResource(R.string.naydite_slovo_i_nazhmite_na_zvezdu_chtoby),
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
