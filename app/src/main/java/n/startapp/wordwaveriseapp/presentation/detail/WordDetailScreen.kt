package n.startapp.wordwaveriseapp.presentation.detail

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import n.startapp.wordwaveriseapp.data.remote.dto.Definition
import n.startapp.wordwaveriseapp.data.remote.dto.PronunciationEntry
import n.startapp.wordwaveriseapp.data.remote.dto.WordDetailResponse
import n.startapp.wordwaveriseapp.ui.theme.*

@Composable
fun WordDetailScreen(
    wordDetail: WordDetailResponse?,
    isLoading: Boolean,
    error: String?,
    isSaved: Boolean,
    isSavedLoading: Boolean = false,
    onSaveWord: () -> Unit,
    onUnsaveWord: () -> Unit,
    isPlayingAudio: Boolean = false,
    playingAudioUrl: String? = null,
    onPlayAudio: (String) -> Unit = {},
    onStopAudio: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
            .padding(16.dp)
    ) {
        when {
            isLoading -> {
                LoadingSection()
            }
            error != null -> {
                ErrorSection(error = error)
            }
            wordDetail != null -> {
                WordDetailContent(
                    wordDetail = wordDetail,
                    isSaved = isSaved,
                    isSavedLoading = isSavedLoading,
                    onSave = onSaveWord,
                    onUnsave = onUnsaveWord,
                    isPlayingAudio = isPlayingAudio,
                    playingAudioUrl = playingAudioUrl,
                    onPlayAudio = onPlayAudio,
                    onStopAudio = onStopAudio
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
            // Animated loading indicator
            val infiniteTransition = rememberInfiniteTransition(label = "loading")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.8f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            CircularProgressIndicator(
                color = PrimaryCyan,
                modifier = Modifier
                    .size((48 * scale).dp)
                    .padding(8.dp),
                strokeWidth = 4.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Загрузка...",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordDetailContent(
    wordDetail: WordDetailResponse,
    isSaved: Boolean,
    isSavedLoading: Boolean = false,
    onSave: () -> Unit,
    onUnsave: () -> Unit,
    isPlayingAudio: Boolean = false,
    playingAudioUrl: String? = null,
    onPlayAudio: (String) -> Unit = {},
    onStopAudio: () -> Unit = {}
) {
    var showSynonymsAntonymsSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Action Buttons Row (Save & Synonyms/Antonyms)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Save/Unsave Button
            Box(modifier = Modifier.weight(1f)) {
                AnimatedSaveButton(
                    isSaved = isSaved,
                    isSavedLoading = isSavedLoading,
                    onSave = onSave,
                    onUnsave = onUnsave
                )
            }

            // Synonyms & Antonyms Button
            if (wordDetail.synonyms.isNotEmpty() || wordDetail.antonyms.isNotEmpty()) {
                Box(modifier = Modifier.weight(1f)) {
                    SynonymsAntonymsButton(
                        synonymsCount = wordDetail.synonyms.size,
                        antonymsCount = wordDetail.antonyms.size,
                        onClick = { showSynonymsAntonymsSheet = true }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Word Header Card
        WordHeaderCard(
            wordDetail = wordDetail,
            isPlayingAudio = isPlayingAudio,
            playingAudioUrl = playingAudioUrl,
            onPlayAudio = onPlayAudio,
            onStopAudio = onStopAudio
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Definitions Section (Collapsible)
        if (wordDetail.definitions.isNotEmpty()) {
            CollapsibleSection(
                title = "Определения",
                itemCount = wordDetail.definitions.size,
                defaultExpanded = true
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    wordDetail.definitions.forEachIndexed { index, definition ->
                        DefinitionItem(
                            definition = definition,
                            index = index + 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Examples Section (Collapsible with "Show More")
        if (wordDetail.examples.isNotEmpty()) {
            ExamplesSection(examples = wordDetail.examples)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Synonyms & Antonyms Modal Bottom Sheet
    if (showSynonymsAntonymsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSynonymsAntonymsSheet = false },
            containerColor = BackgroundPrimary,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            SynonymsAntonymsSheet(
                synonyms = wordDetail.synonyms,
                antonyms = wordDetail.antonyms,
                onDismiss = { showSynonymsAntonymsSheet = false }
            )
        }
    }
}

@Composable
private fun AnimatedSaveButton(
    isSaved: Boolean,
    isSavedLoading: Boolean = false,
    onSave: () -> Unit,
    onUnsave: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSavedLoading -> BorderLight
            isSaved -> Error
            else -> Success
        },
        animationSpec = tween(300),
        label = "backgroundColor"
    )

    Button(
        onClick = if (isSaved) onUnsave else onSave,
        enabled = !isSavedLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            disabledContainerColor = BorderLight
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
    ) {
        if (isSavedLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = TextSecondary,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = if (isSaved) "❌ Удалить" else "💾 Сохранить",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordHeaderCard(
    wordDetail: WordDetailResponse,
    isPlayingAudio: Boolean = false,
    playingAudioUrl: String? = null,
    onPlayAudio: (String) -> Unit = {},
    onStopAudio: () -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { isVisible = true }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(600)) +
                slideInVertically(animationSpec = tween(600), initialOffsetY = { -40 })
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = wordDetail.word.uppercase(),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 1.sp
                )

                // Pronunciation rows (UK / US from scraper or fallback to legacy fields)
                val pronunciations = wordDetail.pronunciations.ifEmpty {
                    listOfNotNull(
                        if (wordDetail.phonetic != null || wordDetail.audioUrl != null)
                            PronunciationEntry(
                                region = null,
                                ipa = wordDetail.phonetic,
                                audioMp3Url = wordDetail.audioUrl
                            ) else null
                    )
                }

                pronunciations.forEach { pron ->
                    Spacer(modifier = Modifier.height(10.dp))
                    PronunciationRow(
                        pronunciation = pron,
                        isPlaying = isPlayingAudio && playingAudioUrl == pron.audioMp3Url,
                        onPlay = { url -> onPlayAudio(url) },
                        onStop = onStopAudio
                    )
                }

                // Translation variants
                wordDetail.translation?.let { translation ->
                    val variants = translation
                        .split(Regex("[,;]"))
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .distinct()
                    if (variants.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Перевод",
                            fontSize = 12.sp,
                            color = TextTertiary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
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
}

@Composable
private fun PronunciationRow(
    pronunciation: PronunciationEntry,
    isPlaying: Boolean,
    onPlay: (String) -> Unit,
    onStop: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Region flag / label
        val regionLabel = when (pronunciation.region?.lowercase()) {
            "uk" -> "🇬🇧"
            "us" -> "🇺🇸"
            else -> "🔊"
        }
        Text(text = regionLabel, fontSize = 16.sp)

        // IPA text
        pronunciation.ipa?.let { ipa ->
            Text(
                text = ipa,
                fontSize = 16.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        } ?: Spacer(modifier = Modifier.weight(1f))

        // Play / Stop button
        pronunciation.audioMp3Url?.let { url ->
            val tint = if (isPlaying) PrimaryCyan else TextSecondary
            IconButton(
                onClick = { if (isPlaying) onStop() else onPlay(url) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Пауза" else "Воспроизвести",
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun CollapsibleSection(
    title: String,
    itemCount: Int,
    defaultExpanded: Boolean = true,
    content: @Composable () -> Unit
) {
    var isExpanded by remember { mutableStateOf(defaultExpanded) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(300),
        label = "rotation"
    )

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
                .padding(16.dp)
        ) {
            // Section Header (Clickable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PrimaryCyan.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "$itemCount",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                    tint = PrimaryCyan,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotationAngle)
                )
            }

            // Content with animation
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(300)) +
                        expandVertically(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300)) +
                       shrinkVertically(animationSpec = tween(300))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    content()
                }
            }
        }
    }
}

@Composable
private fun DefinitionItem(
    definition: Definition,
    index: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundLight
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Index Badge
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

            // Definition Content
            Column(modifier = Modifier.weight(1f)) {
                // Part of Speech
                Text(
                    text = definition.partOfSpeech,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryCyan
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Definition Text
                Text(
                    text = definition.definition,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )

                // Example
                definition.example?.let { example ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\"$example\"",
                        fontSize = 13.sp,
                        color = TextTertiary,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagCloud(
    tags: List<String>,
    color: Color
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEach { tag ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = color.copy(alpha = 0.15f),
            ) {
                Text(
                    text = tag,
                    fontSize = 14.sp,
                    color = color,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ExamplesSection(examples: List<String>) {
    var showAll by remember { mutableStateOf(false) }
    val displayedExamples = if (showAll) examples else examples.take(3)

    CollapsibleSection(
        title = "Примеры использования",
        itemCount = examples.size,
        defaultExpanded = true
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            displayedExamples.forEachIndexed { index, example ->
                ExampleItem(example = example, index = index + 1)
            }

            // Show More Button
            if (examples.size > 3) {
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = { showAll = !showAll },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = PrimaryCyan
                    )
                ) {
                    Text(
                        text = if (showAll) "Показать меньше" else "Показать больше примеров (${examples.size - 3})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (showAll) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExampleItem(example: String, index: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundLight
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = PrimaryCyan.copy(alpha = 0.2f)
            ) {
                Text(
                    text = "$index",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryCyan,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "\"$example\"",
                fontSize = 14.sp,
                color = TextSecondary,
                fontStyle = FontStyle.Italic,
                lineHeight = 20.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SynonymsAntonymsButton(
    synonymsCount: Int,
    antonymsCount: Int,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryCyan
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (synonymsCount > 0) {
                    Text(
                        text = "✓",
                        color = Success,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$synonymsCount",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (synonymsCount > 0 && antonymsCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (antonymsCount > 0) {
                    Text(
                        text = "✕",
                        color = Error,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$antonymsCount",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
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
                    imageVector = Icons.Default.KeyboardArrowDown,
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
