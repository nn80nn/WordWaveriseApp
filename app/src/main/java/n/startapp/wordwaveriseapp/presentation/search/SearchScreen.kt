package n.startapp.wordwaveriseapp.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import n.startapp.wordwaveriseapp.data.remote.dto.DefinitionDto
import n.startapp.wordwaveriseapp.data.remote.dto.WordDto
import n.startapp.wordwaveriseapp.ui.theme.*

@Composable
fun SearchScreen(
    state: SearchState,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
            .padding(16.dp)
    ) {
        // Search Input Section
        SearchInputSection(
            searchQuery = state.searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            onSearch = onSearch,
            onClear = onClear,
            isLoading = state.isLoading
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Content Section
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when {
                state.isLoading -> {
                    LoadingSection()
                }
                state.error != null -> {
                    ErrorSection(error = state.error)
                }
                state.wordData != null -> {
                    WordDataSection(wordData = state.wordData)
                }
                !state.hasSearched -> {
                    EmptyStateSection()
                }
            }
        }
    }
}

@Composable
private fun SearchInputSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Title
        Text(
            text = "Поиск слова",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Введите слово для поиска определения",
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Search TextField
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(12.dp)),
            placeholder = {
                Text(
                    text = "Например: hello",
                    color = TextPlaceholder
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Поиск",
                    tint = PrimaryCyan
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        onSearchQueryChange("")
                        onClear()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Очистить",
                            tint = TextTertiary
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = BackgroundSecondary,
                unfocusedContainerColor = BackgroundSecondary,
                focusedBorderColor = PrimaryCyan,
                unfocusedBorderColor = BorderLight,
                cursorColor = PrimaryCyan,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search Button
        Button(
            onClick = onSearch,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .shadow(4.dp, RoundedCornerShape(12.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent
            ),
            contentPadding = PaddingValues(),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading && searchQuery.isNotEmpty()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(PrimaryBright, PrimaryCyan)
                        ),
                        alpha = if (isLoading || searchQuery.isEmpty()) 0.5f else 1f
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isLoading) "Поиск..." else "Найти слово",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun LoadingSection() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = PrimaryCyan,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Поиск слова...",
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ErrorSection(error: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundSecondary
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "❌",
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Ошибка",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                fontSize = 14.sp,
                color = Error,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptyStateSection() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🔍",
                fontSize = 64.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Начните поиск",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Введите слово и нажмите кнопку поиска",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun WordDataSection(wordData: WordDto) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Word Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(
                containerColor = BackgroundSecondary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Word Title
                Text(
                    text = wordData.word.uppercase(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                // Phonetic
                wordData.phonetic?.let { phonetic ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = phonetic,
                        fontSize = 16.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Translation
                wordData.translation?.let { translation ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BackgroundLight, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Перевод",
                                fontSize = 12.sp,
                                color = TextTertiary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = translation,
                                fontSize = 18.sp,
                                color = PrimaryCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Audio URL indicator (if exists)
                wordData.audioUrl?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔊",
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Произношение доступно",
                            fontSize = 12.sp,
                            color = TextTertiary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Definitions Section
        if (wordData.definitions.isNotEmpty()) {
            Text(
                text = "Определения",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            wordData.definitions.forEachIndexed { index, definition ->
                DefinitionCard(
                    definition = definition,
                    index = index + 1
                )
                if (index < wordData.definitions.size - 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DefinitionCard(
    definition: DefinitionDto,
    index: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundSecondary
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Definition Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(PrimaryBright, PrimaryCyan)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$index",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = definition.partOfSpeech,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Definition Text
            Text(
                text = definition.definition,
                fontSize = 14.sp,
                color = TextSecondary,
                lineHeight = 20.sp
            )

            // Example
            definition.example?.let { example ->
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundLight, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Пример:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextTertiary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "\"$example\"",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }

            // Synonyms
            if (definition.synonyms.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                TagSection(
                    title = "Синонимы:",
                    tags = definition.synonyms,
                    color = Success
                )
            }

            // Antonyms
            if (definition.antonyms.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                TagSection(
                    title = "Антонимы:",
                    tags = definition.antonyms,
                    color = Error
                )
            }
        }
    }
}

@Composable
private fun TagSection(
    title: String,
    tags: List<String>,
    color: Color
) {
    Column {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextTertiary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.take(5).forEach { tag ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = color.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = tag,
                        fontSize = 12.sp,
                        color = color,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
