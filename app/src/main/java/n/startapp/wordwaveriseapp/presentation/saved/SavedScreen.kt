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
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryCyan)
                }
            }

            state.words.isEmpty() -> {
                EmptyState()
            }

            else -> {
                // Word count header
                Text(
                    text = "${state.words.size} ${wordCountLabel(state.words.size)}",
                    fontSize = 13.sp,
                    color = TextTertiary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.words, key = { it.word }) { word ->
                        WordCard(
                            word = word,
                            onDelete = { onDeleteWord(word.word) },
                            onClick = { onWordClick(word.word) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordCard(
    word: SavedWordEntity,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
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
                Text(
                    text = word.word,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDate(word.savedAt),
                        fontSize = 12.sp,
                        color = TextTertiary
                    )
                    if (word.isSynced) {
                        Text("·", fontSize = 12.sp, color = TextTertiary)
                        Text(
                            text = "✓ синхр.",
                            fontSize = 11.sp,
                            color = Success
                        )
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = TextTertiary.copy(alpha = 0.6f)
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(text = "📚", fontSize = 64.sp)
            Text(
                text = "Нет сохранённых слов",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = "Найдите слово и нажмите ☆ чтобы сохранить",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
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
