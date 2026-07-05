package com.wordwaverise.wordwaveriseapp.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.wordwaverise.wordwaveriseapp.data.remote.dto.DefinitionDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.WordDto
import com.wordwaverise.wordwaveriseapp.ui.theme.*
import androidx.compose.ui.text.style.TextOverflow

// sourceFilter == null → all defs; "DETAILS" → synonyms/antonyms page
private data class DictTab(val label: String, val sourceFilter: String?)

private val SOURCE_LABELS = mapOf(
    "WIKTIONARY" to "Wiktionary",
    "CAMBRIDGE" to "Cambridge",
    "OXFORD" to "Oxford",
    "OED" to "Oxford",
    "FREE_DICTIONARY" to "FreeDictionary",
    "WORDSAPI" to "WordsAPI",
    "DATAMUSE" to "DataMuse"
)

@Composable
fun SearchScreen(
    state: SearchState,
    isSaved: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onSaveWord: () -> Unit,
    onUnsaveWord: () -> Unit,
    isPlayingAudio: Boolean,
    playingAudioUrl: String?,
    onPlayAudio: (String) -> Unit,
    onStopAudio: () -> Unit,
    onWordClick: (String) -> Unit,
    onSelectSuggestion: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Build tabs dynamically based on available sources in the current word data
    val tabs = remember(state.wordData?.word) {
        val sources = state.wordData?.definitions?.mapNotNull { it.source?.uppercase() }?.toSet().orEmpty()
        buildList {
            add(DictTab("All", null))
            if ("WIKTIONARY" in sources) add(DictTab("Wiktionary", "WIKTIONARY"))
            if ("CAMBRIDGE" in sources) add(DictTab("Cambridge", "CAMBRIDGE"))
            if ("OXFORD" in sources || "OED" in sources) add(DictTab("Oxford", "OXFORD"))
            add(DictTab("Подробнее", "DETAILS"))
        }
    }

    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()

    // Reset to first tab when word changes
    LaunchedEffect(state.wordData?.word) { pagerState.scrollToPage(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
    ) {
        // ── Search field ──────────────────────────────────────────────────
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            placeholder = {
                Text("Найти слово...", color = TextPlaceholder)
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = TextTertiary
                )
            },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Очистить",
                            tint = TextTertiary
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryCyan,
                unfocusedBorderColor = BorderLight,
                focusedContainerColor = BackgroundSecondary,
                unfocusedContainerColor = BackgroundSecondary
            )
        )

        // ── Suggestions strip (English spelling/autocomplete only) ────────
        // Russian candidates are shown in RuTranslationPanel below, not here
        // Hide suggestions after a successful search (word found)
        if (state.suggestions.isNotEmpty() && !state.isRussianSearch && state.wordData == null) {
            SuggestionsRow(suggestions = state.suggestions, onSelect = onSelectSuggestion)
        }

        // ── Small dictionary tabs (hidden when showing Russian panel) ─────
        if (!state.isRussianSearch) Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tabs.forEachIndexed { idx, tab ->
                val selected = pagerState.currentPage == idx
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (selected) PrimaryCyan.copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                        .clickable { scope.launch { pagerState.animateScrollToPage(idx) } }
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.label,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) PrimaryCyan else TextTertiary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── Word header (outside pager — visible on all tabs) ─────────────
        if (state.wordData != null) {
            WordHeader(
                wordData = state.wordData,
                isSaved = isSaved,
                isPlayingAudio = isPlayingAudio,
                playingAudioUrl = playingAudioUrl,
                onSave = onSaveWord,
                onUnsave = onUnsaveWord,
                onPlayAudio = onPlayAudio,
                onStopAudio = onStopAudio
            )
        }

        // ── Pager ─────────────────────────────────────────────────────────
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val tab = tabs.getOrNull(page) ?: return@HorizontalPager

            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryCyan)
                    }
                }

                state.isRussianSearch -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        RuTranslationPanel(
                            query = state.russianQuery,
                            candidates = state.suggestions,
                            isLoading = state.isFetchingSuggestions,
                            onWordClick = onSelectSuggestion
                        )
                    }
                }

                !state.hasSearched -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                "Введите слово и нажмите поиск",
                                fontSize = 15.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                state.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Error,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                state.error,
                                fontSize = 15.sp,
                                color = Error,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                tab.sourceFilter == "DETAILS" -> {
                    DetailsPage(wordData = state.wordData, onWordClick = onWordClick)
                }

                else -> {
                    val allDefs = state.wordData?.definitions.orEmpty()
                    val defs = if (tab.sourceFilter == null) allDefs
                               else allDefs.filter { def ->
                                   val src = def.source?.uppercase() ?: ""
                                   when (tab.sourceFilter) {
                                       "OXFORD" -> src == "OXFORD" || src == "OED"
                                       else -> src == tab.sourceFilter
                                   }
                               }

                    if (defs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Нет данных от ${tab.label}",
                                fontSize = 14.sp,
                                color = TextTertiary,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp)
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))
                            if (tab.sourceFilter == null) {
                                // "All" tab — compact rows grouped by source
                                val grouped = defs.groupBy { it.source?.uppercase() ?: "API" }
                                grouped.forEach { (sourceKey, sourceDefs) ->
                                    val label = SOURCE_LABELS[sourceKey] ?: sourceKey
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        HorizontalDivider(modifier = Modifier.weight(1f), color = BackgroundLight)
                                        Text(label, fontSize = 11.sp, color = TextTertiary, fontWeight = FontWeight.Medium)
                                        HorizontalDivider(modifier = Modifier.weight(1f), color = BackgroundLight)
                                    }
                                    sourceDefs.forEach { def ->
                                        CompactDefinitionRow(def = def)
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                // Thesaurus section — collapsible at bottom
                                val syns = defs.flatMap { it.synonyms }.distinct().filter { it.isNotBlank() }
                                val ants = defs.flatMap { it.antonyms }.distinct().filter { it.isNotBlank() }
                                if (syns.isNotEmpty() || ants.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    CollapsibleThesaurus(synonyms = syns, antonyms = ants, onWordClick = onWordClick)
                                }
                                // AI summary card — appears in All tab after definitions
                                when {
                                    state.isLoadingAiSummary -> {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                color = PrimaryCyan,
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                        }
                                    }
                                    state.aiSummary != null -> {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        AiSummaryCard(state.aiSummary)
                                    }
                                }
                            } else {
                                // Source-specific tab — full cards (no synonyms inline)
                                defs.forEach { def ->
                                    DefinitionCard(def = def)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// ── Word header ───────────────────────────────────────────────────────────────

@Composable
private fun WordHeader(
    wordData: WordDto,
    isSaved: Boolean,
    isPlayingAudio: Boolean,
    playingAudioUrl: String?,
    onSave: () -> Unit,
    onUnsave: () -> Unit,
    onPlayAudio: (String) -> Unit,
    onStopAudio: () -> Unit
) {
    val ukPron = wordData.pronunciations.firstOrNull { it.region == "uk" }
    val usPron = wordData.pronunciations.firstOrNull { it.region == "us" }
    // Audio URLs: prefer pronunciations, fall back to legacy audioUrl
    val ukAudio = ukPron?.audioMp3Url
        ?: wordData.audioUrl.takeIf { wordData.pronunciations.isEmpty() }
    val usAudio = usPron?.audioMp3Url

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = wordData.word,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                // UK / US IPA row
                val ukIpa = ukPron?.ipa
                val usIpa = usPron?.ipa
                if (ukIpa != null || usIpa != null) {
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ukIpa?.let { Text("UK $it", fontSize = 13.sp, color = TextSecondary) }
                        usIpa?.let { Text("US $it", fontSize = 13.sp, color = TextSecondary) }
                    }
                } else {
                    wordData.phonetic?.let {
                        Text(text = it, fontSize = 13.sp, color = TextSecondary)
                    }
                }

                wordData.translation?.let {
                    Text(
                        text = it,
                        fontSize = 14.sp,
                        color = PrimaryCyan,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // UK audio button
                ukAudio?.let { url ->
                    PronAudioButton(
                        flag = if (usAudio != null) "UK" else null,
                        url = url,
                        isPlaying = isPlayingAudio && playingAudioUrl == url,
                        onPlay = { onPlayAudio(url) },
                        onStop = onStopAudio
                    )
                }
                // US audio button
                usAudio?.let { url ->
                    PronAudioButton(
                        flag = "US",
                        url = url,
                        isPlaying = isPlayingAudio && playingAudioUrl == url,
                        onPlay = { onPlayAudio(url) },
                        onStop = onStopAudio
                    )
                }

                IconButton(onClick = if (isSaved) onUnsave else onSave) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = if (isSaved) "Убрать из сохранённых" else "Сохранить",
                        tint = if (isSaved) Warning else TextTertiary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// ── Pronunciation audio button ────────────────────────────────────────────────

@Composable
private fun PronAudioButton(
    flag: String?,
    url: String,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isPlaying) PrimaryCyan.copy(alpha = 0.2f)
                else BackgroundLight
            )
            .clickable { if (isPlaying) onStop() else onPlay() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (flag != null) {
                Text(text = flag, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            }
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (isPlaying) PrimaryCyan else TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ── Compact definition row (All tab) ─────────────────────────────────────────

@Composable
private fun CompactDefinitionRow(def: DefinitionDto) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundSecondary, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (def.partOfSpeech.isNotBlank()) {
            Text(
                text = def.partOfSpeech,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = PrimaryCyan,
                modifier = Modifier
                    .background(PrimaryCyan.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        Text(
            text = def.definition,
            fontSize = 14.sp,
            color = TextPrimary,
            lineHeight = 19.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        def.source?.let { src ->
            Text(
                text = src.take(3).uppercase(),
                fontSize = 9.sp,
                color = TextTertiary,
                modifier = Modifier
                    .background(BackgroundLight, RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

// ── Collapsible thesaurus (All tab) ──────────────────────────────────────────

@Composable
private fun CollapsibleThesaurus(
    synonyms: List<String>,
    antonyms: List<String>,
    onWordClick: (String) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Тезаурус", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextTertiary)
                Text(if (expanded) "▲" else "▼", fontSize = 11.sp, color = TextTertiary)
            }
            if (expanded) {
                Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (synonyms.isNotEmpty()) {
                        Text("Синонимы", fontSize = 11.sp, color = TextTertiary)
                        FlowChips(items = synonyms, color = PrimaryBlue, onItemClick = onWordClick)
                    }
                    if (antonyms.isNotEmpty()) {
                        Text("Антонимы", fontSize = 11.sp, color = TextTertiary)
                        FlowChips(items = antonyms, color = Error, onItemClick = onWordClick)
                    }
                }
            }
        }
    }
}

// ── Definition card ───────────────────────────────────────────────────────────

@Composable
private fun DefinitionCard(def: DefinitionDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (def.partOfSpeech.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .background(PrimaryCyan.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = def.partOfSpeech,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryCyan
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = def.definition,
                fontSize = 15.sp,
                color = TextPrimary,
                lineHeight = 22.sp
            )

            def.example?.let { ex ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\"$ex\"",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

// ── Details page (synonyms / antonyms / all examples) ────────────────────────

@Composable
private fun DetailsPage(wordData: WordDto?, onWordClick: (String) -> Unit = {}) {
    if (wordData == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Нет данных", fontSize = 14.sp, color = TextTertiary)
        }
        return
    }

    val allSynonyms = wordData.definitions
        .flatMap { it.synonyms }
        .distinct()
        .filter { it.isNotBlank() }

    val allAntonyms = wordData.definitions
        .flatMap { it.antonyms }
        .distinct()
        .filter { it.isNotBlank() }

    val allExamples = wordData.definitions
        .mapNotNull { it.example }
        .distinct()
        .filter { it.isNotBlank() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Definitions grouped by source
        val defsBySource = wordData.definitions.groupBy { it.source?.uppercase() ?: "API" }
        defsBySource.forEach { (sourceKey, defs) ->
            val sourceLabel = SOURCE_LABELS[sourceKey] ?: sourceKey
            SectionCard(title = sourceLabel) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    defs.forEach { def ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (def.partOfSpeech.isNotBlank()) {
                                Text(
                                    text = def.partOfSpeech,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PrimaryCyan,
                                    modifier = Modifier
                                        .background(PrimaryCyan.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(text = def.definition, fontSize = 14.sp, color = TextPrimary, lineHeight = 20.sp)
                            def.example?.let { ex ->
                                Text("\"$ex\"", fontSize = 13.sp, color = TextSecondary, fontStyle = FontStyle.Italic, lineHeight = 18.sp)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (allSynonyms.isNotEmpty() || allAntonyms.isNotEmpty()) {
            SectionCard(title = "Тезаурус") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (allSynonyms.isNotEmpty()) {
                        Text("Синонимы", fontSize = 11.sp, color = TextTertiary)
                        FlowChips(items = allSynonyms, color = PrimaryBlue, onItemClick = onWordClick)
                    }
                    if (allAntonyms.isNotEmpty()) {
                        Text("Антонимы", fontSize = 11.sp, color = TextTertiary)
                        FlowChips(items = allAntonyms, color = Error, onItemClick = onWordClick)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (allExamples.isNotEmpty()) {
            SectionCard(title = "Примеры использования") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    allExamples.forEach { ex ->
                        Text(
                            text = "• $ex",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (allSynonyms.isEmpty() && allAntonyms.isEmpty() && allExamples.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Дополнительные данные недоступны",
                    fontSize = 14.sp,
                    color = TextTertiary,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextTertiary
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun ChipGroup(label: String, items: List<String>, color: Color) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "$label:",
            fontSize = 12.sp,
            color = TextTertiary,
            modifier = Modifier.padding(top = 3.dp)
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.take(8).forEach { item ->
                Box(
                    modifier = Modifier
                        .background(color.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(text = item, fontSize = 12.sp, color = color)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowChips(
    items: List<String>,
    color: Color,
    onItemClick: ((String) -> Unit)? = null
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.forEach { item ->
            Box(
                modifier = Modifier
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .then(if (onItemClick != null) Modifier.clickable { onItemClick(item) } else Modifier)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(text = item, fontSize = 13.sp, color = color)
            }
        }
    }
}

// ── Russian translation panel ─────────────────────────────────────────────────

@Composable
private fun AiSummaryCard(summary: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = PrimaryCyan,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = summary,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 19.sp
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RuTranslationPanel(
    query: String,
    candidates: List<String>,
    isLoading: Boolean,
    onWordClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Translate,
                contentDescription = null,
                tint = PrimaryCyan,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Переводы для «$query»",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryCyan, modifier = Modifier.size(28.dp))
                }
            }
            candidates.isEmpty() -> {
                Text("Перевод не найден", color = TextTertiary, fontSize = 14.sp)
            }
            else -> {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    candidates.forEach { word ->
                        Card(
                            onClick = { onWordClick(word) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BackgroundSecondary)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = word,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
                Text(
                    text = "Нажмите на слово чтобы посмотреть статью",
                    fontSize = 11.sp,
                    color = TextTertiary
                )
            }
        }
    }
}

// ── Suggestions strip ─────────────────────────────────────────────────────────

@Composable
private fun SuggestionsRow(suggestions: List<String>, onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 2.dp, bottom = 6.dp)
    ) {
        Text(
            text = "Возможно, вы имели в виду:",
            fontSize = 12.sp,
            color = TextTertiary
        )
        Spacer(modifier = Modifier.height(5.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            suggestions.forEach { suggestion ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(PrimaryBlue.copy(alpha = 0.12f))
                        .clickable { onSelect(suggestion) }
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = suggestion,
                        fontSize = 13.sp,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
