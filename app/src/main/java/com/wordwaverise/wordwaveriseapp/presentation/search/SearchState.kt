package com.wordwaverise.wordwaveriseapp.presentation.search

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
    val suggestions: List<String> = emptyList(),
    val isFetchingSuggestions: Boolean = false,

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
}
