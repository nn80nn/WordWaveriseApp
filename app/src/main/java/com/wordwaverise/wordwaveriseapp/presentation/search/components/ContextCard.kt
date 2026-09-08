package com.wordwaverise.wordwaveriseapp.presentation.search.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordwaverise.wordwaveriseapp.R
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.ContextAnalysisDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.ContextHintDto
import com.wordwaverise.wordwaveriseapp.ui.theme.*

/**
 * Одно слово, объяснённое там, где оно стоит.
 *
 * Словарь отвечает «что это слово может значить», эта карточка — «что оно значит здесь», то есть
 * ровно на тот вопрос, который есть у читающего по-английски. Общая для вставленного предложения
 * и для книги: два разных ответа на один вопрос разошлись бы на первой же правке.
 *
 * Закладка ведёт себя как в читалке: короткое нажатие сохраняет молча (в книге — в её папку),
 * долгое открывает выбор папок. Прерывать чтение вопросом на каждом слове — самый быстрый способ
 * отучить от сохранения вообще.
 */
@Composable
fun ContextCard(
    /** Быстрый ответ: что слово значит здесь. Приезжает за пару секунд. */
    hint: ContextHintDto?,
    isHinting: Boolean,
    onOpenArticle: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** Полный разбор — только если его попросили: «почему так» и перевод предложения. */
    analysis: ContextAnalysisDto? = null,
    isAnalyzing: Boolean = false,
    onDetails: (() -> Unit)? = null,
    canSave: Boolean = false,
    saved: Boolean = false,
    saving: Boolean = false,
    /** Куда слово ляжет по короткому нажатию — название папки книги. */
    saveHint: String? = null,
    onSave: () -> Unit = {},
    onChooseFolders: () -> Unit = {}
) {
    val colors = WaveTheme.colors

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            if (isHinting) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        color = colors.secondary,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        "Смотрим слово в этом предложении…",
                        fontSize = 13.sp,
                        color = colors.textMuted
                    )
                }
                return@Column
            }

            // ⚠️ Сказать нечего: модель не ответила или запрос не дошёл. Пустая карточка без
            // единого слова — худший ответ: она читается как поломка, а не как «не вышло».
            val lemma = hint?.lemma
            if (lemma == null) {
                Text(
                    "Разбор не получился. Слово можно открыть в словаре — там оно со всеми значениями.",
                    fontSize = 13.sp,
                    color = colors.textMuted
                )
                return@Column
            }

            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    hint.translationRu?.let { translation ->
                        Text(
                            text = translation,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            lemma,
                            fontSize = 14.sp,
                            color = colors.secondary,
                            fontWeight = FontWeight.Medium
                        )
                        hint.pos?.let { pos -> Badge(pos, colors.textMuted) }
                        if (hint.senseMatched) {
                            Badge(stringResource(R.string.znachenie_iz_stati), colors.secondary)
                        }
                    }
                }

                if (canSave) BookmarkButton(saved, saving, saveHint, onSave, onChooseFolders)
            }

            analysis?.translationLemmaRu?.takeIf { it != hint.translationRu }?.let { lemmaRu ->
                Spacer(Modifier.height(4.dp))
                Text("словарная форма: $lemmaRu", fontSize = 12.sp, color = colors.textMuted)
            }

            (hint.senseDefinitionEn ?: analysis?.senseDefinitionEn)?.takeIf { it.isNotBlank() }?.let { gloss ->
                Spacer(Modifier.height(10.dp))
                Text(gloss, fontSize = 14.sp, color = colors.textSecondary, lineHeight = 19.sp)
            }

            analysis?.whyRu?.takeIf { it.isNotBlank() }?.let { why ->
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.tag)
                ) {
                    Text(
                        text = why,
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            analysis?.sentenceRu?.takeIf { it.isNotBlank() }?.let { sentenceRu ->
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.perevod_predlozheniya),
                    fontSize = 11.sp,
                    color = colors.textMuted
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = sentenceRu,
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    color = colors.textSecondary,
                    lineHeight = 19.sp
                )
            }

            // Два предложения, а не одно действие. Подсказка ответила на вопрос, который был;
            // остальное — то, что человек может захотеть дальше, и захотеть по-разному.
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hint.entryAvailable) {
                    TextButton(onClick = { onOpenArticle(lemma) }) {
                        Text("Открыть статью «$lemma»", color = colors.secondary, fontSize = 14.sp)
                    }
                }
                if (analysis == null && onDetails != null) {
                    TextButton(onClick = onDetails, enabled = !isAnalyzing) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                color = colors.textMuted,
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            if (isAnalyzing) "Разбираем…" else "Почему так",
                            color = colors.textMuted,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Закладка: короткое нажатие — молча, долгое — выбор папок.
 *
 * Жест разнесён потому, что вопрос «в какую папку» уместен один раз на книгу, а не один раз на
 * слово: на десятом слове подряд он перестаёт быть выбором и становится работой.
 */
@Composable
private fun BookmarkButton(
    saved: Boolean,
    saving: Boolean,
    saveHint: String?,
    onSave: () -> Unit,
    onChooseFolders: () -> Unit
) {
    val colors = WaveTheme.colors
    val description = when {
        saved -> "Убрать из сохранённых"
        saveHint != null -> "Сохранить в «$saveHint», удерживайте для выбора папок"
        else -> "Сохранить"
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (saved) colors.secondary.copy(alpha = 0.16f) else colors.surfaceElevated)
            .pointerInput(saved, saving) {
                if (saving) return@pointerInput
                detectTapGestures(
                    onTap = { onSave() },
                    // Уже сохранённому выбирать папки незачем: это делается в списке слов.
                    onLongPress = { if (!saved) onChooseFolders() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (saving) {
            CircularProgressIndicator(
                color = colors.secondary,
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = description,
                tint = if (saved) colors.secondary else colors.textMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
