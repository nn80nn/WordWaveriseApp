package com.wordwaverise.wordwaveriseapp.presentation.saved

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordwaverise.wordwaveriseapp.data.local.entity.SavedWordEntity
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
    onNewCategoryNameChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
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
                Text("Нет интернета — показаны сохранённые данные", fontSize = 12.sp, color = Warning)
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
                        label = { Text("Все", fontSize = 13.sp) },
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
                Icon(Icons.Default.Folder, contentDescription = "Категории", tint = TextTertiary)
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
                        Text("Без категории", color = TextSecondary, modifier = Modifier.fillMaxWidth())
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
                        "Категории",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    state.categories.forEach { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(cat.name, fontSize = 15.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onDeleteCategory(cat.id, cat.serverId) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = TextTertiary.copy(alpha = 0.6f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = BackgroundLight)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Create new category
                    Text("Новая категория", fontSize = 14.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = state.newCategoryName,
                            onValueChange = onNewCategoryNameChange,
                            placeholder = { Text("Название", color = TextTertiary) },
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
                            Icon(Icons.Default.Add, contentDescription = "Создать", tint = if (state.newCategoryName.isNotBlank()) PrimaryCyan else TextTertiary)
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
                Text(word.word, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(formatDate(word.savedAt), fontSize = 12.sp, color = TextTertiary)
                    if (word.isSynced) {
                        Text("·", fontSize = 12.sp, color = TextTertiary)
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Success, modifier = Modifier.size(11.dp))
                        Text("синхр.", fontSize = 11.sp, color = Success)
                    }
                    if (categoryName != null) {
                        Text("·", fontSize = 12.sp, color = TextTertiary)
                        Text(categoryName, fontSize = 11.sp, color = PrimaryCyan)
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = TextTertiary.copy(alpha = 0.6f))
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
            Text("Нет сохранённых слов", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(
                "Найдите слово и нажмите на звезду, чтобы сохранить",
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
