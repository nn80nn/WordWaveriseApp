package com.wordwaverise.wordwaveriseapp.presentation.detail

import com.wordwaverise.wordwaveriseapp.data.local.entity.CategoryEntity
import com.wordwaverise.wordwaveriseapp.data.remote.dto.WordDetailResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.LexicalEntryDto

data class WordDetailState(
    val word: String = "",
    val isLoading: Boolean = false,
    val isLoadingFull: Boolean = false,  // true while full scraper data is loading in background
    val wordDetail: WordDetailResponse? = null,
    // The annotated article — the primary view, same as on the search screen.
    val entry: LexicalEntryDto? = null,
    val annotationPending: Boolean = false,
    val annotationDegraded: Boolean = false,
    val error: String? = null,
    // «Сохранено ли слово» больше не спрашивается: сохраняется значение, и отмечено оно
    // закладкой на самом значении — pinnedSenseIds ниже.
    /** Значение статьи, к которому привязано слово — статья открывает его первым. */
    /** Значения, сохранённые человеком: каждое — отдельное слово в его словаре. */
    val pinnedSenseIds: Set<String> = emptySet(),
    /**
     * Серверный id записи для каждого сохранённого значения.
     *
     * ⚠️ Без него снять закладку с одного значения нечем: удаление по написанию забрало бы
     * заодно и остальные значения того же слова.
     */
    val savedEntryIds: Map<String?, Int> = emptyMap(),
    // ── Куда положить сохраняемое значение ───────────────────────────────────
    // Тот же вопрос и та же форма, что на экране поиска: закладка в двух местах приложения
    // не может означать разное.
    val pendingSenseId: String? = null,
    val pendingSenseSummary: String? = null,
    /** Свои папки, о которых знает сервер: этот экран пишет напрямую в API, без Room. */
    val ownFolders: List<CategoryEntity> = emptyList(),
    val chosenFolders: List<Long> = emptyList(),
    val isSavingSense: Boolean = false,
    val isPlayingAudio: Boolean = false,
    val playingAudioUrl: String? = null,
    val audioError: String? = null,
    // AI section
    val aiExplanation: String? = null,
    val isAiExplanationLoading: Boolean = false,
    val aiExamples: String? = null,
    val isAiExamplesLoading: Boolean = false,
    val aiError: String? = null
)
