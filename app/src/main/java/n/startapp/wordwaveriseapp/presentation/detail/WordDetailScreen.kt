package n.startapp.wordwaveriseapp.presentation.detail

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import n.startapp.wordwaveriseapp.data.remote.dto.Definition
import n.startapp.wordwaveriseapp.data.remote.dto.WordDetailResponse
import n.startapp.wordwaveriseapp.ui.theme.*

private data class DetailTab(val label: String, val sourceFilter: String?)

private val DETAIL_TABS = listOf(
    DetailTab("All", null),
    DetailTab("Longman", "LDOCE"),
    DetailTab("Cambridge", "CAMBRIDGE"),
    DetailTab("Oxford", "OXFORD"),
    DetailTab("Подробнее", "DETAILS")
)

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
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState { DETAIL_TABS.size }
    val scope = rememberCoroutineScope()

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
                    imageVector = Icons.Default.ArrowBack,
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
                    modifier = Modifier.fillMaxSize().padding(32.dp),
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
                // ── Tab pills ─────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DETAIL_TABS.forEachIndexed { idx, tab ->
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

                // ── Word header ───────────────────────────────────────────
                WordHeaderCard(
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

                // ── Pager ─────────────────────────────────────────────────
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val tab = DETAIL_TABS[page]
                    when {
                        tab.sourceFilter == "DETAILS" -> {
                            DetailsPage(wordDetail = wordDetail)
                        }
                        else -> {
                            val defs = wordDetail.definitions.let { all ->
                                if (tab.sourceFilter == null) all
                                else all.filter { it.source?.uppercase() == tab.sourceFilter }
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
                                    defs.forEach { def ->
                                        DefinitionCard(def = def)
                                        Spacer(modifier = Modifier.height(8.dp))
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

// ── Word header card ──────────────────────────────────────────────────────────

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
    val ukPron = wordDetail.pronunciations.firstOrNull { it.region == "uk" }
    val usPron = wordDetail.pronunciations.firstOrNull { it.region == "us" }
    val ukAudio = ukPron?.audioMp3Url
        ?: wordDetail.audioUrl.takeIf { wordDetail.pronunciations.isEmpty() }
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
                    wordDetail.phonetic?.let {
                        Text(text = it, fontSize = 13.sp, color = TextSecondary)
                    }
                }

                wordDetail.translation?.let {
                    Text(
                        text = it,
                        fontSize = 14.sp,
                        color = PrimaryCyan,
                        fontWeight = FontWeight.Medium
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
private fun DefinitionCard(def: Definition) {
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

// ── Details page ─────────────────────────────────────────────────────────────

@Composable
private fun DetailsPage(wordDetail: WordDetailResponse) {
    val synonyms = wordDetail.synonyms.filter { it.isNotBlank() }
    val antonyms = wordDetail.antonyms.filter { it.isNotBlank() }
    val examples = wordDetail.examples.filter { it.isNotBlank() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        if (synonyms.isNotEmpty()) {
            SectionCard(title = "Синонимы") {
                FlowChips(items = synonyms, color = PrimaryBlue)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (antonyms.isNotEmpty()) {
            SectionCard(title = "Антонимы") {
                FlowChips(items = antonyms, color = Error)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

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

        if (synonyms.isEmpty() && antonyms.isEmpty() && examples.isEmpty()) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowChips(items: List<String>, color: Color) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.forEach { item ->
            Box(
                modifier = Modifier
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(text = item, fontSize = 13.sp, color = color)
            }
        }
    }
}
