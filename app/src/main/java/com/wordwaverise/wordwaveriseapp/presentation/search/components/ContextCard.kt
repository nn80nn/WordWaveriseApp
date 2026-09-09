package com.wordwaverise.wordwaveriseapp.presentation.search.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
@OptIn(ExperimentalLayoutApi::class)
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
    onChooseFolders: () -> Unit = {},
    /** Прослушать слово. Без него ряд произношения показывает одну транскрипцию. */
    onPlayAudio: ((String) -> Unit)? = null
) {
    val colors = WaveTheme.colors

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
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

            // ⚠️ Подсказка идёт одной попыткой без ретраев, поэтому занятый шлюз означает
            // пустой ответ. Полный разбор пишет другая модель и с ретраями, а поля у них
            // называются одинаково — значит он заменяет подсказку целиком, а не дополняет её.
            val lemma = hint?.lemma ?: analysis?.lemma
            if (lemma == null) {
                // Пустая карточка без единого слова — худший ответ: читается как поломка, а не
                // как «не вышло». И это не тупик: полному разбору есть что попробовать.
                Text(
                    "Быстрый разбор не получился — так бывает, когда словарь занят.",
                    fontSize = 13.sp,
                    color = colors.textMuted
                )
                if (onDetails != null) {
                    TextButton(onClick = onDetails, enabled = !isAnalyzing) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                color = colors.secondary,
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            if (isAnalyzing) "Разбираем…" else "Разобрать подробно",
                            color = colors.secondary,
                            fontSize = 14.sp
                        )
                    }
                }
                return@Column
            }

            val translation = hint?.translationRu ?: analysis?.translationRu
            // Форма, по которой нажали: «popholes», а не «pophole». Словарную форму она не
            // заменяет — читателю нужны обе, иначе слово в тексте и слово в словаре выглядят
            // разными словами.
            val surface = (hint?.target?.surface ?: analysis?.target?.surface)
                ?.takeIf { !it.equals(lemma, ignoreCase = true) }
            val ipa = (hint?.phonetic ?: analysis?.phonetic)?.takeIf { it.isNotBlank() }
            val audio = (hint?.audioUrl ?: analysis?.audioUrl)?.takeIf { it.isNotBlank() }
            val neighbours = hint?.translationsRu.orEmpty()
            val partOfSpeech = hint?.pos ?: analysis?.pos
            val matched = hint?.senseMatched ?: analysis?.senseMatched ?: false
            val hasEntry = hint?.entryAvailable ?: analysis?.entryAvailable ?: false

            /**
             * Ответ сначала, аппарат потом.
             *
             * ⚠️ Порядок не косметика: человек нажал слово, чтобы узнать, что оно здесь значит,
             * и перевод обязан стоять там, куда падает взгляд. Ниже тонкой линии — то же слово
             * по-английски со всем, что о нём известно: форма, словарная форма, часть речи,
             * произношение и пометы. Раньше эти строки шли вперемешку, и карточка читалась как
             * набор обрывков, а не как один ответ.
             */
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    translation?.let { text ->
                        Text(
                            text = text,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            lineHeight = 26.sp
                        )
                    }
                    // Соседние переводы значения — той же строкой мысли, что и главный: одного
                    // слова часто мало, «вести» не ложится в «lead the horse» как «провожать».
                    if (neighbours.isNotEmpty()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = neighbours.joinToString(", "),
                            fontSize = 14.sp,
                            color = colors.textSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }

                if (canSave) BookmarkButton(saved, saving, saveHint, onSave, onChooseFolders)
            }

            Spacer(Modifier.height(10.dp))
            RuleFade()
            Spacer(Modifier.height(8.dp))

            // Аппарат слова одной лентой: она переносится, а не рвётся на отдельные ряды.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (surface != null) "$surface ($lemma)" else lemma,
                    fontSize = 15.sp,
                    color = colors.secondary,
                    fontWeight = FontWeight.Medium
                )
                // ⚠️ Слот транскрипции занят всегда: прочерк говорит «произношения у нас нет»,
                // а исчезнувшая строка — «его не бывает». Это разные утверждения.
                Text(
                    text = ipa ?: "—",
                    style = ApparatusStyle,
                    fontSize = 13.sp,
                    color = colors.textMuted
                )
                if (audio != null && onPlayAudio != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Прослушать произношение",
                        tint = colors.secondary,
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .clickable { onPlayAudio(audio) }
                    )
                }
                partOfSpeech?.let { pos -> Badge(pos, colors.textMuted) }
                // Пометы значения — из корпуса, ни одна не спрашивается у модели.
                hint?.cefr?.let { Badge(it, colors.brass) }
                hint?.register?.let { Badge(registerLabel(it), colors.textMuted) }
                hint?.countability?.let { Badge(countabilityLabel(it), colors.textMuted) }
                if (matched) {
                    Badge(stringResource(R.string.znachenie_iz_stati), colors.secondary)
                }
            }

            analysis?.translationLemmaRu?.takeIf { it != translation }?.let { lemmaRu ->
                Spacer(Modifier.height(4.dp))
                Text("словарная форма: $lemmaRu", fontSize = 12.sp, color = colors.textMuted)
            }

            /**
             * Одна и та же строка на этом месте у каждого слова.
             *
             * ⚠️ У карточки обязан быть постоянный набор строк. Раньше слово со статьёй
             * показывало определение и пометы, а слово без статьи — ничего, и разница читалась
             * как «приложение то показывает, то нет». Пропуск теперь **назван**: человек видит,
             * чего именно не хватает и почему, а не гадает, куда делась половина карточки.
             */
            val definition = (hint?.senseDefinitionEn ?: analysis?.senseDefinitionEn)
                ?.takeIf { it.isNotBlank() }
            Spacer(Modifier.height(10.dp))
            when {
                definition != null -> Text(
                    definition,
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                    lineHeight = 19.sp
                )
                !hasEntry -> Text(
                    "Статьи в словаре пока нет — перевод по этому предложению.",
                    fontSize = 13.sp,
                    color = colors.textMuted,
                    lineHeight = 18.sp
                )
                else -> Text(
                    "В статье это значение не нашлось — перевод по этому предложению.",
                    fontSize = 13.sp,
                    color = colors.textMuted,
                    lineHeight = 18.sp
                )
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

            /**
             * Два предложения, а не одно действие. Подсказка ответила на вопрос, который был;
             * остальное — то, что человек может захотеть дальше, и захотеть по-разному.
             *
             * ⚠️ Строки, а не `TextButton`: у кнопки минимальная высота в 48 dp и свои поля, и
             * ряд из двух таких занимал под карточкой больше места, чем сам разбор. Область
             * нажатия остаётся пальцевой за счёт вертикального отступа строки.
             */
            if (hasEntry || (analysis == null && onDetails != null)) {
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    if (hasEntry) {
                        Text(
                            text = "Открыть статью «$lemma»",
                            color = colors.secondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onOpenArticle(lemma) }
                                .padding(vertical = 4.dp)
                        )
                    }
                    if (analysis == null && onDetails != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(enabled = !isAnalyzing) { onDetails() }
                                .padding(vertical = 4.dp)
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(
                                    color = colors.textMuted,
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            Text(
                                if (isAnalyzing) "Разбираем…" else "Подробнее",
                                color = colors.textMuted,
                                fontSize = 14.sp
                            )
                        }
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

/** Регистр значения по-русски: помета для читателя, а не имя enum'а. */
private fun registerLabel(register: String): String = when (register) {
    "FORMAL" -> "офиц."
    "INFORMAL" -> "разг."
    "SLANG" -> "сленг"
    "VULGAR" -> "груб."
    "DATED" -> "устар."
    "LITERARY" -> "книжн."
    "TECHNICAL" -> "спец."
    else -> register.lowercase()
}

/** ⚠️ Исчисляемость — свойство значения, а не слова: `paper`-материал и `paper`-документ разные. */
private fun countabilityLabel(countability: String): String = when (countability) {
    "COUNTABLE" -> "исчисл."
    "UNCOUNTABLE" -> "неисчисл."
    "BOTH" -> "исчисл. и неисчисл."
    else -> countability.lowercase()
}
