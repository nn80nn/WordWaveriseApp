package n.startapp.wordwaveriseapp.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    isSaved: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onSaveWord: () -> Unit,
    onUnsaveWord: () -> Unit,
    isPlayingAudio: Boolean = false,
    playingAudioUrl: String? = null,
    onPlayAudio: (String) -> Unit = {},
    onStopAudio: () -> Unit = {},
    onWordClick: (String) -> Unit = {},
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
                    WordDataSection(
                        wordData = state.wordData,
                        isSaved = isSaved,
                        isPlayingAudio = isPlayingAudio,
                        playingAudioUrl = playingAudioUrl,
                        onSave = onSaveWord,
                        onUnsave = onUnsaveWord,
                        onPlayAudio = onPlayAudio,
                        onStopAudio = onStopAudio,
                        onWordClick = { onWordClick(state.wordData.word) }
                    )
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
                .fillMaxWidth(),
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
        val searchEnabled = !isLoading && searchQuery.isNotEmpty()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = if (searchEnabled) listOf(PrimaryBright, PrimaryCyan)
                                 else listOf(PrimaryBright.copy(alpha = 0.5f), PrimaryCyan.copy(alpha = 0.5f))
                    )
                )
                .clickable(enabled = searchEnabled) { onSearch() },
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
            .fillMaxWidth(),
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun WordDataSection(
    wordData: WordDto,
    isSaved: Boolean,
    isPlayingAudio: Boolean = false,
    playingAudioUrl: String? = null,
    onSave: () -> Unit,
    onUnsave: () -> Unit,
    onPlayAudio: (String) -> Unit = {},
    onStopAudio: () -> Unit = {},
    onWordClick: () -> Unit = {}
) {
    var showSynonymsAntonymsSheet by remember { mutableStateOf(false) }

    // Aggregate synonyms and antonyms from all definitions
    val allSynonyms = wordData.definitions.flatMap { it.synonyms }.distinct()
    val allAntonyms = wordData.definitions.flatMap { it.antonyms }.distinct()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Details Button
            Button(
                onClick = onWordClick,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryCyan
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Подробнее",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }

            // Save/Unsave Button
            Button(
                onClick = if (isSaved) onUnsave else onSave,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSaved) Error else Success
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (isSaved) "❌ Удалить" else "💾 Сохранить",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }

            // Synonyms & Antonyms Button
            if (allSynonyms.isNotEmpty() || allAntonyms.isNotEmpty()) {
                Button(
                    onClick = { showSynonymsAntonymsSheet = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryCyan
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (allSynonyms.isNotEmpty()) {
                            Text(
                                text = "✓",
                                color = Success,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${allSynonyms.size}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (allSynonyms.isNotEmpty() && allAntonyms.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "•",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        if (allAntonyms.isNotEmpty()) {
                            Text(
                                text = "✕",
                                color = Error,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${allAntonyms.size}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Word Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth(),
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

                // Translation variants
                wordData.translation?.let { translation ->
                    val variants = translation
                        .split(Regex("[,;]"))
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .distinct()
                    if (variants.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BackgroundLight, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Перевод",
                                    fontSize = 12.sp,
                                    color = TextTertiary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                if (variants.size == 1) {
                                    Text(
                                        text = variants[0],
                                        fontSize = 18.sp,
                                        color = PrimaryCyan,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        variants.forEach { variant ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = PrimaryCyan.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = variant,
                                                    fontSize = 15.sp,
                                                    color = PrimaryCyan,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Audio playback button
                wordData.audioUrl?.let { url ->
                    Spacer(modifier = Modifier.height(12.dp))
                    val isPlaying = isPlayingAudio && playingAudioUrl == url
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🔊", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Произношение",
                            fontSize = 12.sp,
                            color = TextTertiary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { if (isPlaying) onStopAudio() else onPlayAudio(url) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Стоп" else "Воспроизвести",
                                tint = if (isPlaying) PrimaryCyan else TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
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

    // Synonyms & Antonyms Modal Bottom Sheet
    if (showSynonymsAntonymsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSynonymsAntonymsSheet = false },
            containerColor = BackgroundPrimary,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            SynonymsAntonymsSheet(
                synonyms = allSynonyms,
                antonyms = allAntonyms,
                onDismiss = { showSynonymsAntonymsSheet = false }
            )
        }
    }
}

@Composable
private fun DefinitionCard(
    definition: DefinitionDto,
    index: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
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
        }
    }
}

@Composable
private fun SynonymsAntonymsSheet(
    synonyms: List<String>,
    antonyms: List<String>,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .background(BackgroundPrimary)
            .padding(horizontal = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Синонимы и Антонимы",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Закрыть",
                    tint = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Synonyms Section
            if (synonyms.isNotEmpty()) {
                SynonymsAntonymsSection(
                    title = "Синонимы",
                    items = synonyms,
                    color = Success,
                    icon = "✓"
                )
            }

            // Antonyms Section
            if (antonyms.isNotEmpty()) {
                SynonymsAntonymsSection(
                    title = "Антонимы",
                    items = antonyms,
                    color = Error,
                    icon = "✕"
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SynonymsAntonymsSection(
    title: String,
    items: List<String>,
    color: Color,
    icon: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundSecondary
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = color.copy(alpha = 0.2f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = icon,
                            fontSize = 20.sp,
                            color = color
                        )
                    }
                }

                Column {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${items.size} слов",
                        fontSize = 13.sp,
                        color = TextTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tags
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items.forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = color.copy(alpha = 0.15f),
                    ) {
                        Text(
                            text = item,
                            fontSize = 15.sp,
                            color = color,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}
