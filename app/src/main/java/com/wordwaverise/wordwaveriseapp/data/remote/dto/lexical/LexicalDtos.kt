package com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical

import com.wordwaverise.wordwaveriseapp.data.remote.dto.PronunciationDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.WordDetailResponse
import kotlinx.serialization.Serializable

/**
 * Mirrors the backend v2 lookup contract.
 *
 * Every field carries a default: the backend omits nulls and may add fields, and a missing one
 * should degrade a single card rather than fail the whole parse.
 */

@Serializable
data class BilingualExampleDto(
    val en: String = "",
    val ru: String = "",
    val sourceRef: Int? = null
)

@Serializable
data class CollocationDto(
    val pattern: String = "",
    val ru: String? = null
)

@Serializable
data class SenseDto(
    val id: String = "",
    val definitionEn: String = "",
    /** Full Russian explanation — not a word-for-word rendering of [definitionEn]. */
    val definitionRu: String = "",
    /** Short Russian equivalents for this sense specifically. */
    val translationsRu: List<String> = emptyList(),
    val register: String = "NEUTRAL",
    val cefr: String? = null,
    val domain: String? = null,
    val examples: List<BilingualExampleDto> = emptyList(),
    val collocations: List<CollocationDto> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    /** 1-based indices into [LexicalEntryDto.sources]; empty when the sense is model-written. */
    val sourceRefs: List<Int> = emptyList(),
    val generated: Boolean = false,
    val usageNote: String? = null
)

@Serializable
data class InflectedFormsDto(
    val plural: String? = null,
    val past: String? = null,
    val pastParticiple: String? = null,
    val presentParticiple: String? = null,
    val thirdPerson: String? = null,
    val comparative: String? = null,
    val superlative: String? = null
) {
    /** Label/value pairs worth rendering, in a sensible reading order. */
    fun labelled(): List<Pair<String, String>> = listOfNotNull(
        plural?.let { "мн. ч." to it },
        past?.let { "прош. вр." to it },
        pastParticiple?.let { "прич. II" to it },
        presentParticiple?.let { "прич. I" to it },
        thirdPerson?.let { "3 л. ед. ч." to it },
        comparative?.let { "сравн." to it },
        superlative?.let { "превосх." to it }
    )
}

@Serializable
data class PosGroupDto(
    val pos: String = "",
    val posRu: String = "",
    val pronunciations: List<PronunciationDto> = emptyList(),
    val forms: InflectedFormsDto? = null,
    val senses: List<SenseDto> = emptyList()
)

@Serializable
data class SourceRefDto(
    val index: Int = 0,
    val source: String = "",
    val partOfSpeech: String? = null,
    val definition: String = "",
    val example: String? = null
)

@Serializable
data class LexicalEntryDto(
    val lemma: String = "",
    val queryForm: String = "",
    val kind: String = "WORD",
    val language: String = "en",
    val pronunciations: List<PronunciationDto> = emptyList(),
    val phonetic: String? = null,
    val audioUrl: String? = null,
    val posGroups: List<PosGroupDto> = emptyList(),
    val etymology: String? = null,
    val usageNotes: List<String> = emptyList(),
    val frequencyBand: String? = null,
    val sources: List<SourceRefDto> = emptyList(),
    /** No dictionary had this headword; the whole article is model-written. */
    val aiGenerated: Boolean = false,
    /** Annotation failed — the article has structure but no Russian. */
    val degraded: Boolean = false,
    val model: String = ""
)

@Serializable
data class QueryAlternativeDto(
    val form: String = "",
    val kind: String = "WORD",
    val hintRu: String? = null
)

@Serializable
data class ResolvedQueryDto(
    val raw: String = "",
    val normalized: String = "",
    val language: String = "en",
    /** WORD | INFLECTION | MISSPELLING | PHRASE | SENTENCE | RU_* | UNKNOWN */
    val kind: String = "WORD",
    val lemma: String? = null,
    val surface: String = "",
    val correctionApplied: Boolean = false,
    val correctedFrom: String? = null,
    val alternatives: List<QueryAlternativeDto> = emptyList(),
    val confidence: Double = 1.0,
    val resolvedBy: String = "heuristic"
)

@Serializable
data class LookupNoticeDto(
    /** spelling_corrected | layout_corrected | lemma_resolved */
    val type: String = "",
    val textRu: String = "",
    val originalQuery: String? = null
)

@Serializable
data class TokenDto(
    val index: Int = 0,
    val text: String = "",
    val start: Int = 0,
    val end: Int = 0,
    val tappable: Boolean = false,
    val groupWith: List<Int> = emptyList()
)

@Serializable
data class TokenizedTextDto(
    val text: String = "",
    val tokens: List<TokenDto> = emptyList()
)

@Serializable
data class RuEnCandidateDto(
    val en: String = "",
    val pos: String = "",
    /** Which sense of the Russian word this option covers. */
    val ruGloss: String = "",
    /** When to use this one rather than its neighbours. */
    val whenToUse: String = "",
    val example: String = "",
    val exampleRu: String = "",
    val cefr: String? = null,
    val register: String = "NEUTRAL"
)

@Serializable
data class RuEnCandidatesDto(
    val query: String = "",
    val isAmbiguous: Boolean = false,
    val candidates: List<RuEnCandidateDto> = emptyList(),
    val note: String? = null,
    val degraded: Boolean = false
)

@Serializable
data class LookupResponseDto(
    val resolution: ResolvedQueryDto = ResolvedQueryDto(),
    val notice: LookupNoticeDto? = null,
    val entry: LexicalEntryDto? = null,
    /** READY | PENDING | DEGRADED | UNAVAILABLE */
    val annotationStatus: String = "UNAVAILABLE",
    val annotationNote: String? = null,
    val retryAfterMs: Int? = null,
    /** The raw multi-source aggregate — powers the sources view. */
    val raw: WordDetailResponse? = null,
    val ruEn: RuEnCandidatesDto? = null,
    val tokenized: TokenizedTextDto? = null
)

@Serializable
data class LookupApiResponse(
    val status: String = "",
    val data: LookupResponseDto? = null,
    val message: String? = null
)

// ── Context analysis ────────────────────────────────────────────────────────

@Serializable
data class ContextTargetDto(val index: Int = 0, val surface: String = "")

@Serializable
data class ContextAnalysisDto(
    val text: String = "",
    val tokens: List<TokenDto> = emptyList(),
    val target: ContextTargetDto? = null,
    val lemma: String? = null,
    /** Part of speech as used in this sentence. */
    val pos: String? = null,
    val senseId: String? = null,
    val senseMatched: Boolean = false,
    val senseDefinitionEn: String? = null,
    /** Russian for the word as it appears here, in the right grammatical form. */
    val translationRu: String? = null,
    val translationLemmaRu: String? = null,
    val sentenceRu: String? = null,
    /** Why this sense and not another. */
    val whyRu: String? = null,
    val entryAvailable: Boolean = false
)

@Serializable
data class ContextAnalysisApiResponse(
    val status: String = "",
    val data: ContextAnalysisDto? = null,
    val message: String? = null
)

@Serializable
data class RuEnApiResponse(
    val status: String = "",
    val data: RuEnCandidatesDto? = null,
    val message: String? = null
)

@Serializable
data class TokenizeRequest(val text: String)

@Serializable
data class ContextAnalyzeRequest(
    val text: String,
    val tokenIndex: Int? = null,
    val token: String? = null
)

@Serializable
data class TokenizedApiResponse(
    val status: String = "",
    val data: TokenizedTextDto? = null,
    val message: String? = null
)
