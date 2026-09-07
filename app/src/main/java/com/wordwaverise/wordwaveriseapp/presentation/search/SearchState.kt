package com.wordwaverise.wordwaveriseapp.presentation.search

import com.wordwaverise.wordwaveriseapp.data.local.entity.CategoryEntity
import com.wordwaverise.wordwaveriseapp.data.remote.dto.SuggestItemDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.WordDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.ContextAnalysisDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.LexicalEntryDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.LookupNoticeDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.RuEnCandidateDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.TokenDto

data class SearchState(
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val wordData: WordDto? = null,
    val error: String? = null,
    val hasSearched: Boolean = false,
    val isPlayingAudio: Boolean = false,
    val playingAudioUrl: String? = null,
    val suggestions: List<SuggestItemDto> = emptyList(),
    val isFetchingSuggestions: Boolean = false,

    // ── Куда положить сохраняемое значение ───────────────────────────────────
    /**
     * Значение, которое сохранят, как только человек назовёт папки.
     *
     * Пока оно не null, на экране стоит диалог выбора: слово, отправленное «никуда»,
     * находится потом только через «Без папки», и человек, собирающий урок, замечает это,
     * когда собрал уже двадцать слов.
     */
    val pendingSenseId: String? = null,
    /** Что именно сохранится — перевод выбранного значения, а не только написание. */
    val pendingSenseSummary: String? = null,
    /** Свои папки: в папку класса писать нельзя, и сервер такой запрос отклоняет. */
    val ownFolders: List<CategoryEntity> = emptyList(),
    /** Отмеченные папки. Пустой список — «без папки». */
    val chosenFolders: List<Long> = emptyList(),
    val isSavingSense: Boolean = false,

    // ── The annotated article: the primary view ──────────────────────────────
    val entry: LexicalEntryDto? = null,
    /** True while the article is still being written; the sources view is usable meanwhile. */
    val annotationPending: Boolean = false,
    /** Annotation failed — the article has structure but no Russian. */
    val annotationDegraded: Boolean = false,

    /**
     * A silent substitution the user should know about (typo fixed, form resolved).
     * Distinct from [error]: this accompanies a successful result.
     */
    val notice: LookupNoticeDto? = null,

    // ── Sentence input: tap a word to ask about it ───────────────────────────
    val sentenceText: String = "",
    val sentenceTokens: List<TokenDto> = emptyList(),
    val selectedTokenIndex: Int? = null,
    val contextAnalysis: ContextAnalysisDto? = null,
    val isAnalyzingContext: Boolean = false,

    // ── Russian input: English options to choose between ─────────────────────
    val isRussianSearch: Boolean = false,
    val russianQuery: String = "",
    val ruEnCandidates: List<RuEnCandidateDto> = emptyList(),
    val ruEnNote: String? = null,
    val ruEnAmbiguous: Boolean = false
) {
    val isSentenceMode: Boolean get() = sentenceTokens.isNotEmpty()

    /**
     * Экран после крестика: от поиска не остаётся ничего — кроме того, что поиску никогда и
     * не принадлежало.
     *
     * ⚠️ [ownFolders] приезжает из базы **один раз**, при создании модели, и повторно не
     * приедет, пока сами папки не изменятся. Поэтому сброс состояния целиком не «очищал
     * экран», а стирал список папок до конца жизни модели: диалог сохранения следующего
     * слова предлагал только «Без папки» и «создать новую» — то есть выглядел так, будто
     * папок у человека нет вовсе. Первое сохранение при этом работало, и починить это можно
     * было только заведя лишнюю папку.
     */
    fun cleared(): SearchState = SearchState(ownFolders = ownFolders)
}
