package com.wordwaverise.wordwaveriseapp.presentation.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.LexicalEntryDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.PosGroupDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.SenseDto
import com.wordwaverise.wordwaveriseapp.ui.theme.*

/**
 * The annotated article — the primary view of a word.
 *
 * Organised the way a dictionary entry is: by part of speech, then by sense, most common first.
 * Each sense leads with its own Russian, because a translation only means anything next to the
 * sense it belongs to.
 */
@Composable
fun ArticleView(
    entry: LexicalEntryDto,
    onWordClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        if (entry.aiGenerated) {
            AiGeneratedBanner()
            Spacer(Modifier.height(12.dp))
        }

        if (entry.degraded) {
            DegradedBanner()
            Spacer(Modifier.height(12.dp))
        }

        entry.posGroups.forEach { group ->
            PosGroupSection(group = group, entry = entry, onWordClick = onWordClick)
            Spacer(Modifier.height(20.dp))
        }

        if (entry.posGroups.isEmpty() && !entry.degraded) {
            Text(
                "Статья ещё не готова — откройте вкладку «Источники».",
                fontSize = 14.sp,
                color = TextTertiary,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        }

        entry.etymology?.takeIf { it.isNotBlank() }?.let { etymology ->
            InfoBlock(title = "Происхождение", body = etymology)
            Spacer(Modifier.height(12.dp))
        }

        entry.usageNotes.filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }?.let { notes ->
            InfoBlock(title = "Как употребляется", body = notes.joinToString("\n\n"))
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PosGroupSection(
    group: PosGroupDto,
    entry: LexicalEntryDto,
    onWordClick: (String) -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = group.posRu.ifBlank { group.pos },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryCyan
            )
            // Homographs differ by pronunciation, not just by sense — show it per part of speech.
            group.pronunciations.firstOrNull { !it.ipa.isNullOrBlank() }?.ipa?.let { ipa ->
                Text(ipa, fontSize = 13.sp, color = TextTertiary)
            }
        }

        group.forms?.labelled()?.takeIf { it.isNotEmpty() }?.let { forms ->
            Spacer(Modifier.height(6.dp))
            Text(
                text = forms.joinToString("  ·  ") { (label, value) -> "$label $value" },
                fontSize = 12.sp,
                color = TextTertiary
            )
        }

        Spacer(Modifier.height(10.dp))

        group.senses.forEachIndexed { index, sense ->
            SenseCard(
                sense = sense,
                ordinal = index + 1,
                entry = entry,
                onWordClick = onWordClick
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun InfoBlock(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundSecondary)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextTertiary)
            Spacer(Modifier.height(6.dp))
            Text(body, fontSize = 14.sp, color = TextSecondary, lineHeight = 20.sp)
        }
    }
}

/**
 * An entry no dictionary carried. Said plainly: an unlabelled invented article is worse than
 * no article, because the user cannot tell which parts to trust.
 */
@Composable
private fun AiGeneratedBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryCyan.copy(alpha = 0.10f))
    ) {
        Text(
            text = "Статья написана ИИ — в словарях-источниках этого выражения нет.",
            fontSize = 13.sp,
            color = TextSecondary,
            lineHeight = 18.sp,
            modifier = Modifier.padding(14.dp)
        )
    }
}

@Composable
private fun DegradedBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Error.copy(alpha = 0.10f))
    ) {
        Text(
            text = "Разбор пока недоступен — показаны определения из источников без перевода. " +
                "Попробуйте открыть слово ещё раз чуть позже.",
            fontSize = 13.sp,
            color = TextSecondary,
            lineHeight = 18.sp,
            modifier = Modifier.padding(14.dp)
        )
    }
}

/** Small rounded label used for CEFR, register and source chips. */
@Composable
fun Badge(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = color)
    }
}

/**
 * One sense: its Russian first, then the English definition, then evidence.
 */
@Composable
fun SenseCard(
    sense: SenseDto,
    ordinal: Int,
    entry: LexicalEntryDto,
    onWordClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundSecondary)
    ) {
        Column(Modifier.padding(14.dp)) {

            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = "$ordinal.",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextTertiary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column(Modifier.weight(1f)) {
                    if (sense.translationsRu.isNotEmpty()) {
                        Text(
                            text = sense.translationsRu.joinToString(", "),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(6.dp))
                    }

                    if (sense.definitionRu.isNotBlank()) {
                        Text(sense.definitionRu, fontSize = 14.sp, color = TextSecondary, lineHeight = 20.sp)
                        Spacer(Modifier.height(6.dp))
                    }

                    if (sense.definitionEn.isNotBlank()) {
                        Text(sense.definitionEn, fontSize = 13.sp, color = TextTertiary, lineHeight = 18.sp)
                    }
                }
            }

            // ── Labels ────────────────────────────────────────────────────
            val labels = buildList {
                sense.cefr?.let { add(it to PrimaryCyan) }
                registerLabel(sense.register)?.let { add(it to TextTertiary) }
                sense.domain?.takeIf { it.isNotBlank() }?.let { add(it to TextTertiary) }
                if (sense.generated) add("ИИ" to Error)
            }
            if (labels.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    labels.forEach { (text, color) -> Badge(text, color) }
                }
            }

            // ── Examples ──────────────────────────────────────────────────
            sense.examples.filter { it.en.isNotBlank() }.takeIf { it.isNotEmpty() }?.let { examples ->
                Spacer(Modifier.height(12.dp))
                examples.forEach { example ->
                    Column(Modifier.padding(bottom = 8.dp)) {
                        Text(
                            text = example.en,
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic,
                            color = TextPrimary,
                            lineHeight = 19.sp
                        )
                        if (example.ru.isNotBlank()) {
                            Text(example.ru, fontSize = 13.sp, color = TextTertiary, lineHeight = 18.sp)
                        }
                    }
                }
            }

            sense.collocations.filter { it.pattern.isNotBlank() }.takeIf { it.isNotEmpty() }?.let { collocations ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = collocations.joinToString("  ·  ") { it.pattern },
                    fontSize = 12.sp,
                    color = PrimaryCyan
                )
            }

            sense.usageNote?.takeIf { it.isNotBlank() }?.let { note ->
                Spacer(Modifier.height(8.dp))
                Text(note, fontSize = 12.sp, color = TextTertiary, lineHeight = 17.sp)
            }

            // ── Thesaurus ─────────────────────────────────────────────────
            val related = sense.synonyms.map { it to PrimaryCyan } + sense.antonyms.map { it to Error }
            if (related.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    related.take(6).forEach { (word, color) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(color.copy(alpha = 0.12f))
                                .clickable { onWordClick(word) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(word, fontSize = 12.sp, color = color)
                        }
                    }
                }
            }

            // ── Provenance ────────────────────────────────────────────────
            // Which dictionaries backed this sense, so the article stays checkable.
            val sources = sense.sourceRefs
                .mapNotNull { ref -> entry.sources.firstOrNull { it.index == ref }?.source }
                .distinct()
            if (sources.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = sources.joinToString(" · ") { SOURCE_LABELS[it.uppercase()] ?: it },
                    fontSize = 10.sp,
                    color = TextTertiary
                )
            }
        }
    }
}

private fun registerLabel(register: String): String? = when (register.uppercase()) {
    "FORMAL" -> "формальное"
    "INFORMAL" -> "разговорное"
    "SLANG" -> "сленг"
    "VULGAR" -> "грубое"
    "DATED" -> "устаревшее"
    "LITERARY" -> "книжное"
    "TECHNICAL" -> "спец."
    else -> null
}

internal val SOURCE_LABELS = mapOf(
    "CAMBRIDGE" to "Cambridge",
    "OXFORD" to "Oxford",
    "OED" to "OED",
    "WIKTIONARY" to "Wiktionary",
    "LDOCE" to "Longman",
    "FREEDICTIONARY" to "FreeDictionary",
    "FREEDICT" to "FreeDictionary",
    "WORDSAPI" to "WordsAPI",
    "DATAMUSE" to "DataMuse"
)
