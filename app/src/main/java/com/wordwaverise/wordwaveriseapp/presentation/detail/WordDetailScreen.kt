package com.wordwaverise.wordwaveriseapp.presentation.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.wordwaverise.wordwaveriseapp.data.remote.dto.WordDetailResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.WordEntry
import com.wordwaverise.wordwaveriseapp.ui.theme.*

private data class DetailTab(val label: String, val sourceFilter: String?)

/** Lightweight display model used inside the pager — avoids coupling to DTO type. */
private data class DisplayDef(
    val partOfSpeech: String,
    val definition: String,
    val example: String?,
    val source: String?
)

@Composable
fun WordDetailScreen(
    wordDetail: WordDetailResponse?,
    isLoading: Boolean,
    error: String?,
    isSaved: Boolean,
    isSavedLoading: Boolean = false,
    isLoadingFull: Boolean = false,
    onSaveWord: () -> Unit,
    onUnsaveWord: () -> Unit,
    isPlayingAudio: Boolean = false,
    playingAudioUrl: String? = null,
    onPlayAudio: (String) -> Unit = {},
    onStopAudio: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    aiExplanation: String? = null,
    isAiExplanationLoading: Boolean = false,
    aiExamples: String? = null,
    isAiExamplesLoading: Boolean = false,
    aiError: String? = null,
    onLoadAiExplanation: () -> Unit = {},
    onLoadAiExamples: () -> Unit = {},
    onWordClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Build tabs dynamically — only show dict tabs when that source has data
    val tabs = remember(wordDetail?.word) {
        val sources = wordDetail?.definitions?.mapNotNull { it.source?.uppercase() }?.toSet().orEmpty()
        buildList {
            add(DetailTab("All", null))
            if ("WIKTIONARY" in sources) add(DetailTab("Wiktionary", "WIKTIONARY"))
            if ("CAMBRIDGE" in sources) add(DetailTab("Cambridge", "CAMBRIDGE"))
            if ("OXFORD" in sources || "OED" in sources) add(DetailTab("Oxford", "OXFORD"))
            add(DetailTab("Подробнее", "DETAILS"))
            add(DetailTab("AI ✨", "AI"))
        }
    }
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    // -1 = "All entries"; ≥0 = index into wordDetail.entries
    var selectedEntryIdx by remember { mutableIntStateOf(-1) }
    LaunchedEffect(wordDetail?.word) { selectedEntryIdx = -1 }

    val selectedEntry: WordEntry? = wordDetail?.entries?.getOrNull(selectedEntryIdx)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
    ) {
        // ── Back button ───────────────────────────────────────────────────
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = TextPrimary
                )
            }
        }

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryCyan)
                }
            }

            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("❌", fontSize = 48.sp)
                        Text(
                            error,
                            fontSize = 15.sp,
                            color = Error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            wordDetail != null -> {
                // ── Word header ────────────────────────────────────────────
                WordHeaderCard(
                    wordDetail = wordDetail,
                    selectedEntry = selectedEntry,
                    isSaved = isSaved,
                    isSavedLoading = isSavedLoading,
                    isPlayingAudio = isPlayingAudio,
                    playingAudioUrl = playingAudioUrl,
                    onSave = onSaveWord,
                    onUnsave = onUnsaveWord,
                    onPlayAudio = onPlayAudio,
                    onStopAudio = onStopAudio
                )

                // ── Entry chips (only when word has 2+ entries / POS groups) ──
                if (wordDetail.entries.size > 1) {
                    EntrySelector(
                        entries = wordDetail.entries,
                        selectedIdx = selectedEntryIdx,
                        onSelect = { selectedEntryIdx = it }
                    )
                }

                // ── Full-data loading indicator ────────────────────────────
                if (isLoadingFull) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = PrimaryCyan,
                        trackColor = PrimaryCyan.copy(alpha = 0.15f)
                    )
                }

                // ── Source tab row ─────────────────────────────────────────
                DictTabRow(
                    tabs = tabs,
                    selectedIndex = pagerState.currentPage,
                    onTabClick = { idx -> scope.launch { pagerState.animateScrollToPage(idx) } }
                )

                // ── Pager ─────────────────────────────────────────────────
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val tab = tabs[page]
                    when {
                        tab.sourceFilter == "AI" -> {
                            AiPage(
                                aiExplanation = aiExplanation,
                                isAiExplanationLoading = isAiExplanationLoading,
                                aiExamples = aiExamples,
                                isAiExamplesLoading = isAiExamplesLoading,
                                aiError = aiError,
                                onLoadExplanation = onLoadAiExplanation,
                                onLoadExamples = onLoadAiExamples
                            )
                        }
                        tab.sourceFilter == "DETAILS" -> {
                            DetailsPage(
                                wordDetail = wordDetail,
                                selectedEntry = selectedEntry,
                                aiExplanation = aiExplanation,
                                isAiExplanationLoading = isAiExplanationLoading,
                                aiExamples = aiExamples,
                                isAiExamplesLoading = isAiExamplesLoading,
                                aiError = aiError,
                                onLoadExplanation = onLoadAiExplanation,
                                onLoadExamples = onLoadAiExamples,
                                onWordClick = onWordClick
                            )
                        }
                        else -> {
                            fun matchesTab(source: String?): Boolean {
                                val s = source?.uppercase() ?: return tab.sourceFilter == null
                                return when (tab.sourceFilter) {
                                    null -> true
                                    "OXFORD" -> s == "OXFORD" || s == "OED"
                                    else -> s == tab.sourceFilter
                                }
                            }
                            val defs: List<DisplayDef> = if (selectedEntry != null) {
                                // Entry selected: show only that entry's meanings
                                selectedEntry.meanings
                                    .filter { tab.sourceFilter == null || matchesTab(it.source) }
                                    .map { m ->
                                        DisplayDef(
                                            partOfSpeech = selectedEntry.partOfSpeech ?: "",
                                            definition = m.definition,
                                            example = m.example,
                                            source = m.source
                                        )
                                    }
                            } else {
                                // All entries: use flat definitions list
                                wordDetail.definitions
                                    .filter { tab.sourceFilter == null || matchesTab(it.source) }
                                    .map { def ->
                                        DisplayDef(
                                            partOfSpeech = def.partOfSpeech,
                                            definition = def.definition,
                                            example = def.example,
                                            source = def.source
                                        )
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
                                                HorizontalDivider(
                                                    modifier = Modifier.weight(1f),
                                                    color = BackgroundLight
                                                )
                                                Text(
                                                    text = label,
                                                    fontSize = 11.sp,
                                                    color = TextTertiary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                HorizontalDivider(
                                                    modifier = Modifier.weight(1f),
                                                    color = BackgroundLight
                                                )
                                            }
                                            sourceDefs.forEach { def ->
                                                CompactDefinitionRow(def = def)
                                                Spacer(modifier = Modifier.height(4.dp))
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }
                                        // AI section at bottom of All tab
                                        Spacer(modifier = Modifier.height(8.dp))
                                        AiInlineSection(
                                            aiExplanation = aiExplanation,
                                            isAiExplanationLoading = isAiExplanationLoading,
                                            aiExamples = aiExamples,
                                            isAiExamplesLoading = isAiExamplesLoading,
                                            aiError = aiError,
                                            onLoadExplanation = onLoadAiExplanation,
                                            onLoadExamples = onLoadAiExamples
                                        )
                                        // Thesaurus — collapsible at bottom of All tab
                                        val allSyns = wordDetail.synonyms.filter { it.isNotBlank() }
                                        val allAnts = wordDetail.antonyms.filter { it.isNotBlank() }
                                        if (allSyns.isNotEmpty() || allAnts.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            ThesaurusSection(
                                                synonyms = allSyns,
                                                antonyms = allAnts,
                                                collapsible = true,
                                                onWordClick = onWordClick
                                            )
                                        }
                                    } else {
                                        // Dictionary-specific tabs — full cards with examples, no synonyms
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
    }
}

// ── Dictionary tab row ───────────────────────────────────────────────────────

@Composable
private fun DictTabRow(
    tabs: List<DetailTab>,
    selectedIndex: Int,
    onTabClick: (Int) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        containerColor = BackgroundPrimary,
        contentColor = PrimaryCyan,
        edgePadding = 8.dp,
        indicator = { tabPositions ->
            if (selectedIndex < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                    height = 2.dp,
                    color = PrimaryCyan
                )
            }
        },
        divider = { HorizontalDivider(color = BackgroundLight.copy(alpha = 0.6f)) }
    ) {
        tabs.forEachIndexed { idx, tab ->
            Tab(
                selected = idx == selectedIndex,
                onClick = { onTabClick(idx) },
                selectedContentColor = PrimaryCyan,
                unselectedContentColor = TextTertiary
            ) {
                Text(
                    text = tab.label,
                    fontSize = 13.sp,
                    fontWeight = if (idx == selectedIndex) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp)
                )
            }
        }
    }
}

// ── Entry selector row ────────────────────────────────────────────────────────

@Composable
private fun EntrySelector(
    entries: List<WordEntry>,
    selectedIdx: Int,
    onSelect: (Int) -> Unit   // -1 = All
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        EntryChip(label = "Все", selected = selectedIdx == -1, onClick = { onSelect(-1) })
        entries.forEachIndexed { idx, entry ->
            val label = buildString {
                entry.partOfSpeech?.let { append(it) }
                entry.phonetic?.let { ipa ->
                    if (isNotEmpty()) append(" ")
                    append(ipa)
                }
                if (isEmpty()) append("entry ${idx + 1}")
            }
            EntryChip(
                label = label,
                selected = selectedIdx == idx,
                onClick = { onSelect(idx) }
            )
        }
    }
}

@Composable
private fun EntryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) PrimaryCyan.copy(alpha = 0.18f) else BackgroundSecondary
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) PrimaryCyan else TextSecondary
        )
    }
}

// ── Word header card ──────────────────────────────────────────────────────────

@Composable
private fun WordHeaderCard(
    wordDetail: WordDetailResponse,
    selectedEntry: WordEntry?,
    isSaved: Boolean,
    isSavedLoading: Boolean,
    isPlayingAudio: Boolean,
    playingAudioUrl: String?,
    onSave: () -> Unit,
    onUnsave: () -> Unit,
    onPlayAudio: (String) -> Unit,
    onStopAudio: () -> Unit
) {
    val prons = selectedEntry?.pronunciations?.takeIf { it.isNotEmpty() }
        ?: wordDetail.pronunciations
    val ukPron = prons.firstOrNull { it.region == "uk" }
    val usPron = prons.firstOrNull { it.region == "us" }
    val ukAudio = ukPron?.audioMp3Url
        ?: wordDetail.audioUrl.takeIf { prons.isEmpty() }
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
                    text = wordDetail.word,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                val ukIpa = ukPron?.ipa
                val usIpa = usPron?.ipa
                if (ukIpa != null || usIpa != null) {
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ukIpa?.let { Text("🇬🇧 $it", fontSize = 13.sp, color = TextSecondary) }
                        usIpa?.let { Text("🇺🇸 $it", fontSize = 13.sp, color = TextSecondary) }
                    }
                } else {
                    val phonetic = selectedEntry?.phonetic ?: wordDetail.phonetic
                    phonetic?.let {
                        Text(text = it, fontSize = 13.sp, color = TextSecondary)
                    }
                }

                // Show per-entry translation when one is selected; otherwise all distinct translations
                val translationText = if (selectedEntry != null) {
                    selectedEntry.translation ?: wordDetail.translation
                } else {
                    val entryTranslations = wordDetail.entries.mapNotNull { it.translation }.distinct().take(3)
                    entryTranslations.joinToString(" | ").ifBlank { null } ?: wordDetail.translation
                }
                translationText?.let {
                    Text(
                        text = it,
                        fontSize = 14.sp,
                        color = PrimaryCyan,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = true
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ukAudio?.let { url ->
                    PronAudioButton(
                        flag = if (usAudio != null) "🇬🇧" else null,
                        url = url,
                        isPlaying = isPlayingAudio && playingAudioUrl == url,
                        onPlay = { onPlayAudio(url) },
                        onStop = onStopAudio
                    )
                }
                usAudio?.let { url ->
                    PronAudioButton(
                        flag = "🇺🇸",
                        url = url,
                        isPlaying = isPlayingAudio && playingAudioUrl == url,
                        onPlay = { onPlayAudio(url) },
                        onStop = onStopAudio
                    )
                }

                if (isSavedLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = PrimaryCyan,
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(onClick = if (isSaved) onUnsave else onSave) {
                        Text(
                            text = if (isSaved) "★" else "☆",
                            fontSize = 24.sp,
                            color = if (isSaved) Warning else TextTertiary
                        )
                    }
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
                if (isPlaying) PrimaryCyan.copy(alpha = 0.2f) else BackgroundLight
            )
            .clickable { if (isPlaying) onStop() else onPlay() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (flag != null) Text(text = flag, fontSize = 14.sp)
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (isPlaying) PrimaryCyan else TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ── Definition card ───────────────────────────────────────────────────────────

@Composable
private fun DefinitionCard(def: DisplayDef) {
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

// ── Compact definition row (used in "All" tab) ────────────────────────────────

@Composable
private fun CompactDefinitionRow(def: DisplayDef) {
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

// ── Details page ─────────────────────────────────────────────────────────────

private val SOURCE_LABELS = mapOf(
    "LDOCE" to "Longman",
    "WIKTIONARY" to "Wiktionary",
    "CAMBRIDGE" to "Cambridge",
    "OXFORD" to "Oxford",
    "OED" to "Oxford"
)

@Composable
private fun DetailsPage(
    wordDetail: WordDetailResponse,
    selectedEntry: WordEntry?,
    aiExplanation: String? = null,
    isAiExplanationLoading: Boolean = false,
    aiExamples: String? = null,
    isAiExamplesLoading: Boolean = false,
    aiError: String? = null,
    onLoadExplanation: () -> Unit = {},
    onLoadExamples: () -> Unit = {},
    onWordClick: (String) -> Unit = {}
) {
    // Always use global synonyms/antonyms — not per-entry duplicates
    val synonyms = wordDetail.synonyms.filter { it.isNotBlank() }
    val antonyms = wordDetail.antonyms.filter { it.isNotBlank() }

    // Group definitions by source for this tab
    val defsBySource = wordDetail.definitions
        .filter { if (selectedEntry != null) selectedEntry.meanings.any { m -> m.definition == it.definition } else true }
        .groupBy { it.source?.uppercase() ?: "API" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Definitions grouped by source
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
                            Text(
                                text = def.definition,
                                fontSize = 14.sp,
                                color = TextPrimary,
                                lineHeight = 20.sp
                            )
                            def.example?.let { ex ->
                                Text(
                                    text = "\"$ex\"",
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    fontStyle = FontStyle.Italic,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Synonyms / antonyms — single global section
        if (synonyms.isNotEmpty() || antonyms.isNotEmpty()) {
            ThesaurusSection(
                synonyms = synonyms,
                antonyms = antonyms,
                collapsible = false,
                onWordClick = onWordClick
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // All examples
        val examples = wordDetail.examples.filter { it.isNotBlank() }
        if (examples.isNotEmpty()) {
            SectionCard(title = "Примеры использования") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    examples.forEach { ex ->
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

        if (defsBySource.isEmpty() && synonyms.isEmpty() && antonyms.isEmpty() && examples.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Дополнительные данные недоступны",
                    fontSize = 14.sp,
                    color = TextTertiary,
                    textAlign = TextAlign.Center
                )
            }
        }

        // AI section inline
        AiInlineSection(
            aiExplanation = aiExplanation,
            isAiExplanationLoading = isAiExplanationLoading,
            aiExamples = aiExamples,
            isAiExamplesLoading = isAiExamplesLoading,
            aiError = aiError,
            onLoadExplanation = onLoadExplanation,
            onLoadExamples = onLoadExamples
        )

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

// ── AI page ────────────────────────────────────────────────────────────────────

@Composable
private fun AiPage(
    aiExplanation: String?,
    isAiExplanationLoading: Boolean,
    aiExamples: String?,
    isAiExamplesLoading: Boolean,
    aiError: String?,
    onLoadExplanation: () -> Unit,
    onLoadExamples: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Explanation section
        AiSection(
            title = "Объяснение ИИ",
            content = aiExplanation,
            isLoading = isAiExplanationLoading,
            buttonLabel = "Объяснить",
            error = if (aiExplanation == null && !isAiExplanationLoading) aiError else null,
            onLoad = onLoadExplanation
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Examples section
        AiSection(
            title = "Примеры от ИИ",
            content = aiExamples,
            isLoading = isAiExamplesLoading,
            buttonLabel = "Генерировать примеры",
            error = if (aiExamples == null && !isAiExamplesLoading) aiError else null,
            onLoad = onLoadExamples
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AiSection(
    title: String,
    content: String?,
    isLoading: Boolean,
    buttonLabel: String,
    error: String? = null,
    onLoad: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextTertiary
                )
                if (!isLoading && content == null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryCyan.copy(alpha = 0.12f))
                            .clickable { onLoad() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = buttonLabel,
                            fontSize = 12.sp,
                            color = PrimaryCyan,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            when {
                isLoading -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = PrimaryCyan,
                            strokeWidth = 2.dp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                content != null -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = content,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        lineHeight = 21.sp
                    )
                }
                error != null -> {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = error,
                        fontSize = 12.sp,
                        color = Error
                    )
                }
                else -> {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Нажмите кнопку чтобы загрузить",
                        fontSize = 13.sp,
                        color = TextTertiary,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }
    }
}

/** Compact AI block shown at the bottom of All and Подробнее tabs. */
@Composable
private fun AiInlineSection(
    aiExplanation: String?,
    isAiExplanationLoading: Boolean,
    aiExamples: String?,
    isAiExamplesLoading: Boolean,
    aiError: String?,
    onLoadExplanation: () -> Unit,
    onLoadExamples: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = BackgroundLight)
            Text("AI ✨", fontSize = 11.sp, color = TextTertiary, fontWeight = FontWeight.Medium)
            HorizontalDivider(modifier = Modifier.weight(1f), color = BackgroundLight)
        }
        AiSection(
            title = "Объяснение",
            content = aiExplanation,
            isLoading = isAiExplanationLoading,
            buttonLabel = "Объяснить",
            error = if (aiExplanation == null && !isAiExplanationLoading) aiError else null,
            onLoad = onLoadExplanation
        )
        AiSection(
            title = "Примеры от ИИ",
            content = aiExamples,
            isLoading = isAiExamplesLoading,
            buttonLabel = "Генерировать",
            error = if (aiExamples == null && !isAiExamplesLoading) aiError else null,
            onLoad = onLoadExamples
        )
    }
}

// ── Thesaurus section (synonyms + antonyms, optionally collapsible) ───────────

@Composable
private fun ThesaurusSection(
    synonyms: List<String>,
    antonyms: List<String>,
    collapsible: Boolean = false,
    onWordClick: (String) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(!collapsible) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (collapsible) Modifier.clickable { expanded = !expanded } else Modifier),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Тезаурус",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextTertiary
                )
                if (collapsible) {
                    Text(
                        text = if (expanded) "▲" else "▼",
                        fontSize = 11.sp,
                        color = TextTertiary
                    )
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                    .then(
                        if (onItemClick != null) Modifier.clickable { onItemClick(item) }
                        else Modifier
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(text = item, fontSize = 13.sp, color = color)
            }
        }
    }
}
