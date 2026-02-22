package n.startapp.wordwaveriseapp.presentation.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import n.startapp.wordwaveriseapp.data.local.entity.SavedWordEntity
import n.startapp.wordwaveriseapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SavedScreen(
    state: SavedWordsState,
    onDeleteWord: (String) -> Unit,
    onWordClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
            .padding(16.dp)
    ) {
        // Title
        Text(
            text = "Сохранённые слова",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (state.words.isEmpty()) "Список пуст" else "${state.words.size} ${getWordCount(state.words.size)}",
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Content
        when {
            state.words.isEmpty() -> {
                EmptyState()
            }
            else -> {
                WordsList(
                    words = state.words,
                    onDeleteWord = onDeleteWord,
                    onWordClick = onWordClick
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "📚",
                fontSize = 64.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Нет сохранённых слов",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Найдите интересное слово и нажмите кнопку сохранения",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun WordsList(
    words: List<SavedWordEntity>,
    onDeleteWord: (String) -> Unit,
    onWordClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(words, key = { it.word }) { word ->
            WordItem(
                word = word,
                onDelete = { onDeleteWord(word.word) },
                onClick = { onWordClick(word.word) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordItem(
    word: SavedWordEntity,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundSecondary
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = word.word.capitalize(Locale.getDefault()),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDate(word.savedAt),
                        fontSize = 12.sp,
                        color = TextTertiary
                    )
                    if (word.isSynced) {
                        Text(
                            text = "✓ Синхронизировано",
                            fontSize = 11.sp,
                            color = Success
                        )
                    } else {
                        Text(
                            text = "○ Локально",
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = Error
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale("ru"))
    return sdf.format(Date(timestamp))
}

private fun getWordCount(count: Int): String {
    return when {
        count % 10 == 1 && count % 100 != 11 -> "слово"
        count % 10 in 2..4 && count % 100 !in 12..14 -> "слова"
        else -> "слов"
    }
}
