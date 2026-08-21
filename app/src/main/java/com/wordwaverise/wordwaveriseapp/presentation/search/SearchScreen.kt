package com.wordwaverise.wordwaveriseapp.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordwaverise.wordwaveriseapp.data.remote.dto.DefinitionDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.WordDto
import com.wordwaverise.wordwaveriseapp.presentation.search.components.ArticleView
import com.wordwaverise.wordwaveriseapp.presentation.search.components.NoticeBar
import com.wordwaverise.wordwaveriseapp.presentation.search.components.RuEnCandidatesView
import com.wordwaverise.wordwaveriseapp.presentation.search.components.SentenceView
import androidx.compose.foundation.BorderStroke
import com.wordwaverise.wordwaveriseapp.R
import com.wordwaverise.wordwaveriseapp.ui.theme.*
import androidx.compose.ui.text.style.TextOverflow

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
    onSearchOriginal: (String) -> Unit = {},
    onTokenClick: (Int) -> Unit = {},
    pinnedSenseIds: Set<String> = emptySet(),
    canSave: Boolean = true,
    onToggleSense: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .waveSurface()
    ) {
        // ── Search field ──────────────────────────────────────────────────
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            placeholder = {
                Text(stringResource(R.string.nayti_slovo), color = TextPlaceholder)
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
                            contentDescription = stringResource(R.string.ochistit),
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
        if (state.suggestions.isNotEmpty() && !state.isRussianSearch &&
            state.wordData == null && state.entry == null
        ) {
            SuggestionsRow(suggestions = state.suggestions, onSelect = onSelectSuggestion)
        }

        // A correction the server applied — shown above everything, and reversible.
        state.notice?.let { notice ->
            NoticeBar(notice = notice, onSearchOriginal = onSearchOriginal)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── Word header ───────────────────────────────────────────────────
        // The row of dictionary tabs that used to sit above it is gone: every one of them
        // said what the article says, only unsorted, untranslated and five times over.
        val isWordResult = !state.isRussianSearch && !state.isSentenceMode
        if (state.wordData != null && isWordResult) {
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

        // ── Content ───────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryCyan)
                    }
                }

                state.isRussianSearch -> {
                    RuEnCandidatesView(
                        query = state.russianQuery,
                        candidates = state.ruEnCandidates,
                        note = state.ruEnNote,
                        isAmbiguous = state.ruEnAmbiguous,
                        isLoading = state.isFetchingSuggestions,
                        onSelect = onSelectSuggestion
                    )
                }

                state.isSentenceMode -> {
                    SentenceView(
                        tokens = state.sentenceTokens,
                        selectedIndex = state.selectedTokenIndex,
                        analysis = state.contextAnalysis,
                        isAnalyzing = state.isAnalyzingContext,
                        onTokenClick = onTokenClick,
                        onOpenArticle = onSelectSuggestion
                    )
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
                                stringResource(R.string.vvedite_slovo_i_nazhmite_poisk),
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

                state.entry != null -> {
                    ArticleView(
                        entry = state.entry,
                        onWordClick = onWordClick,
                        pinnedSenseIds = pinnedSenseIds,
                        canSave = canSave,
                        onToggleSense = onToggleSense
                    )
                }

                else -> {
                    // No article yet — the raw dictionary stands in for it.
                    RawDefinitions(
                        wordData = state.wordData,
                        annotationPending = state.annotationPending
                    )
                }
            }
        }
    }
}

// ── Raw definitions: what stands in until the article exists ─────────────────

/**
 * FreeDictionary only, when it has anything.
 *
 * The other sources repeat the same meanings without senses, translations or examples, and
 * stacking all of them was the old «Источники» page — five spellings of one meaning, which
 * reads as a worse dictionary rather than a fuller one. A bare spinner is worse still: it
 * hides definitions already in hand while the annotation runs for minutes.
 */
@Composable
private fun RawDefinitions(wordData: WordDto?, annotationPending: Boolean) {
    val all = wordData?.definitions.orEmpty()
    val defs = all.filter { it.source?.uppercase() == "FREEDICTIONARY" }.ifEmpty { all }

    if (defs.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = PrimaryCyan,
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                stringResource(R.string.sobiraem_statyu),
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (annotationPending) {
                CircularProgressIndicator(
                    color = PrimaryCyan,
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            }
            Text(
                text = stringResource(
                    if (annotationPending) R.string.statya_sobiraetsya_pokazyvaem_slovar
                    else R.string.opredeleniya_iz_slovarya
                ),
                fontSize = 12.sp,
                color = TextTertiary
            )
        }
        defs.forEach { def ->
            DefinitionCard(def = def)
            Spacer(modifier = Modifier.height(8.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ── Word header ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
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

    val colors = WaveTheme.colors

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
        border = BorderStroke(1.dp, WaveTheme.colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        // A brass-to-tide rule down the leading edge: the article reads as a
        // page torn from a dictionary rather than as a coloured box.
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(listOf(colors.secondary, colors.primary))
                    )
            )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 15.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
        ) {
            // Word title gets almost the full card width — only a star button shares the row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = wordData.word,
                    fontFamily = Comfortaa,
                    fontSize = if (wordData.word.length > 12) 22.sp else 30.sp,
                    lineHeight = if (wordData.word.length > 12) 27.sp else 36.sp,
                    letterSpacing = (-0.6).sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        // wrapContentWidth first, so the wave is drawn under the
                        // word itself rather than across the whole header row.
                        .wrapContentWidth(Alignment.Start)
                        .padding(bottom = 6.dp)
                        .waveUnderline(color = colors.secondary.copy(alpha = 0.65f))
                )

                IconButton(onClick = if (isSaved) onUnsave else onSave) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = if (isSaved) stringResource(R.string.ubrat_iz_sohranennyh) else stringResource(R.string.sohranit),
                        tint = if (isSaved) Warning else TextTertiary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // UK / US IPA + audio buttons — full width, wraps to a new line instead of
            // squeezing/fragmenting when both regions and a long word don't fit on one line
            val ukIpa = ukPron?.ipa
            val usIpa = usPron?.ipa
            if (ukIpa != null || usIpa != null || ukAudio != null || usAudio != null) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ukIpa?.let {
                        Text("UK $it", style = ApparatusStyle, color = TextSecondary, maxLines = 1, softWrap = false)
                    }
                    ukAudio?.let { url ->
                        PronAudioButton(
                            flag = null,
                            url = url,
                            isPlaying = isPlayingAudio && playingAudioUrl == url,
                            onPlay = { onPlayAudio(url) },
                            onStop = onStopAudio
                        )
                    }
                    usIpa?.let {
                        Text("US $it", style = ApparatusStyle, color = TextSecondary, maxLines = 1, softWrap = false)
                    }
                    usAudio?.let { url ->
                        PronAudioButton(
                            flag = null,
                            url = url,
                            isPlaying = isPlayingAudio && playingAudioUrl == url,
                            onPlay = { onPlayAudio(url) },
                            onStop = onStopAudio
                        )
                    }
                }
            } else {
                wordData.phonetic?.let {
                    Text(text = it, style = ApparatusStyle, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))
                }
            }

            wordData.translation?.let {
                Text(
                    text = it,
                    fontSize = 14.sp,
                    color = PrimaryCyan,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 6.dp)
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
                contentDescription = if (isPlaying) stringResource(R.string.ostanovit_proiznoshenie) else stringResource(R.string.proslushat_proiznoshenie),
                tint = if (isPlaying) PrimaryCyan else TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ── Definition card ───────────────────────────────────────────────────────────

@Composable
private fun DefinitionCard(def: DefinitionDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
        border = BorderStroke(1.dp, WaveTheme.colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                    Text(text = item, fontSize = 12.sp, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
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
        border = BorderStroke(1.dp, WaveTheme.colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                Text(stringResource(R.string.perevod_ne_nayden), color = TextTertiary, fontSize = 14.sp)
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
                            colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
                            border = BorderStroke(1.dp, WaveTheme.colors.border),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                    text = stringResource(R.string.nazhmite_na_slovo_chtoby_posmotret_statyu),
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
            text = stringResource(R.string.vozmozhno_vy_imeli_v_vidu),
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
