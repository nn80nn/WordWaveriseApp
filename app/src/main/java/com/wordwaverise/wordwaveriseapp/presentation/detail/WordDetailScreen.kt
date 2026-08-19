package com.wordwaverise.wordwaveriseapp.presentation.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordwaverise.wordwaveriseapp.data.remote.dto.WordDetailResponse
import com.wordwaverise.wordwaveriseapp.presentation.search.components.ArticleView
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.LexicalEntryDto
import com.wordwaverise.wordwaveriseapp.R
import com.wordwaverise.wordwaveriseapp.ui.theme.*

/** Lightweight display model for the raw definitions — avoids coupling to DTO type. */
private data class DisplayDef(
    val partOfSpeech: String,
    val definition: String,
    val example: String?,
    val source: String?
)

@Composable
fun WordDetailScreen(
    wordDetail: WordDetailResponse?,
    entry: LexicalEntryDto? = null,
    annotationPending: Boolean = false,
    annotationDegraded: Boolean = false,
    isLoading: Boolean,
    error: String?,
    isSaved: Boolean,
    isSavedLoading: Boolean = false,
    isLoadingFull: Boolean = false,
    onSaveWord: () -> Unit,
    onUnsaveWord: () -> Unit,
    pinnedSenseId: String? = null,
    onToggleSense: (String) -> Unit = {},
    isPlayingAudio: Boolean = false,
    playingAudioUrl: String? = null,
    onPlayAudio: (String) -> Unit = {},
    onStopAudio: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    onWordClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .waveSurface()
    ) {
        // ── Back button ───────────────────────────────────────────────────
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.nazad),
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
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            error,
                            fontSize = 15.sp,
                            color = Error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            wordDetail != null || entry != null -> {
                // ── Word header ────────────────────────────────────────────
                if (wordDetail != null) WordHeaderCard(
                    wordDetail = wordDetail,
                    isSaved = isSaved,
                    isSavedLoading = isSavedLoading,
                    isPlayingAudio = isPlayingAudio,
                    playingAudioUrl = playingAudioUrl,
                    onSave = onSaveWord,
                    onUnsave = onUnsaveWord,
                    onPlayAudio = onPlayAudio,
                    onStopAudio = onStopAudio
                )

                // ── Full-data loading indicator ────────────────────────────
                // Also runs while the article is still being written server-side.
                if (isLoadingFull || annotationPending) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = PrimaryCyan,
                        trackColor = PrimaryCyan.copy(alpha = 0.15f)
                    )
                }

                // ── The article, and nothing beside it ─────────────────────
                // The tabs that used to stand here — one per dictionary, plus «Подробнее»
                // and «AI» — all restated the article: the same meanings, unsorted and
                // untranslated, or written again by a model with nothing to check it
                // against. Two descriptions of one word on one screen eventually
                // contradict each other, which is corrosive in a dictionary.
                if (entry != null) {
                    ArticleView(
                        entry = entry,
                        onWordClick = onWordClick,
                        pinnedSenseId = pinnedSenseId,
                        canSave = true,
                        onToggleSense = onToggleSense
                    )
                } else {
                    RawDefinitions(
                        wordDetail = wordDetail,
                        annotationPending = annotationPending
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
private fun RawDefinitions(wordDetail: WordDetailResponse?, annotationPending: Boolean) {
    val all = wordDetail?.definitions.orEmpty().map { def ->
        DisplayDef(
            partOfSpeech = def.partOfSpeech,
            definition = def.definition,
            example = def.example,
            source = def.source
        )
    }
    val defs = all.filter { it.source?.uppercase() == "FREEDICTIONARY" }.ifEmpty { all }

    if (defs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.sobiraem_statyu),
                fontSize = 14.sp,
                color = TextTertiary,
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
        Text(
            text = stringResource(
                if (annotationPending) R.string.statya_sobiraetsya_pokazyvaem_slovar
                else R.string.opredeleniya_iz_slovarya
            ),
            fontSize = 12.sp,
            color = TextTertiary,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        defs.forEach { def ->
            DefinitionCard(def = def)
            Spacer(modifier = Modifier.height(8.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ── Word header card ──────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordHeaderCard(
    wordDetail: WordDetailResponse,
    isSaved: Boolean,
    isSavedLoading: Boolean,
    isPlayingAudio: Boolean,
    playingAudioUrl: String?,
    onSave: () -> Unit,
    onUnsave: () -> Unit,
    onPlayAudio: (String) -> Unit,
    onStopAudio: () -> Unit
) {
    val prons = wordDetail.pronunciations
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
        border = BorderStroke(1.dp, WaveTheme.colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Word title gets almost the full card width — only the save control shares the row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = wordDetail.word,
                    fontFamily = Comfortaa,
                    letterSpacing = (-0.6).sp,
                    fontSize = if (wordDetail.word.length > 12) 22.sp else 30.sp,
                    lineHeight = if (wordDetail.word.length > 12) 27.sp else 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (isSavedLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = PrimaryCyan,
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(onClick = if (isSaved) onUnsave else onSave) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (isSaved) stringResource(R.string.ubrat_iz_sohranennyh) else stringResource(R.string.sohranit),
                            tint = if (isSaved) Warning else TextTertiary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
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
                wordDetail.phonetic?.let {
                    Text(text = it, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))
                }
            }

            // All distinct translations the entries carry, falling back to the word's own
            val entryTranslations = wordDetail.entries.mapNotNull { it.translation }.distinct().take(3)
            val translationText = entryTranslations.joinToString(" | ").ifBlank { null }
                ?: wordDetail.translation
            translationText?.let {
                Text(
                    text = it,
                    fontSize = 14.sp,
                    color = PrimaryCyan,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
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
            if (flag != null) Text(text = flag, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
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
private fun DefinitionCard(def: DisplayDef) {
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
