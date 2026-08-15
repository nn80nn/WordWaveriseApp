package com.wordwaverise.wordwaveriseapp.data.remote.dto.exercise

import kotlinx.serialization.Serializable

/**
 * The exercise contract, mirrored from the backend
 * (`WordWaveriseBackend/.../models/exercise/Exercise.kt`).
 *
 * The server decides what a question is, what counts as an answer and what the explanation says
 * — including the Russian instruction text — so nothing here has an opinion of its own. Anything
 * this file invents is a place where the phone and the browser can start showing different
 * practice for the same folder.
 */

@Serializable
enum class ExerciseKind {
    MEANING_CHOICE,
    WORD_CHOICE,
    TRANSLATE_RU_EN,
    TRANSLATE_EN_RU,
    FILL_BLANK,
    CONTEXT_CHOICE,
    COLLOCATION,
    WORD_FORM,
    SPELLING,
    LISTENING,

    /** A kind this build predates. Rendered by `format`, never by name. */
    UNKNOWN
}

@Serializable
enum class ExerciseFormat { CHOICE, INPUT }

@Serializable
enum class ExerciseSource { CORPUS, CARD, AI }

@Serializable
enum class ExerciseScope { SAVED, FLASHCARDS, DUE }

@Serializable
data class ExerciseDto(
    val id: String = "",
    val kind: ExerciseKind = ExerciseKind.UNKNOWN,
    val format: ExerciseFormat = ExerciseFormat.CHOICE,
    val word: String = "",
    val promptRu: String = "",
    val question: String = "",
    val questionIsSentence: Boolean = false,
    val options: List<String> = emptyList(),
    val correctIndex: Int? = null,
    /** A recording of the word. Present only when the exercise needs one. */
    val audioUrl: String? = null,
    val answer: String = "",
    val acceptedAnswers: List<String> = emptyList(),
    val hintRu: String? = null,
    val explanationRu: String? = null,
    val cardId: Int? = null,
    val savedWordId: Int? = null,
    val source: ExerciseSource = ExerciseSource.CARD
) {
    companion object {
        /** The gap marker inside [question]. Highlighted verbatim by both clients. */
        const val BLANK = "_____"
    }
}

@Serializable
data class ExerciseBatchDto(
    val exercises: List<ExerciseDto> = emptyList(),
    val wordsAvailable: Int = 0,
    val kindsUsed: List<ExerciseKind> = emptyList(),
    val noticeRu: String? = null
)

@Serializable
data class ExerciseKindInfoDto(
    val kind: ExerciseKind = ExerciseKind.UNKNOWN,
    val titleRu: String = "",
    val descriptionRu: String = "",
    val format: ExerciseFormat = ExerciseFormat.CHOICE,
    val available: Int = 0
)

@Serializable
data class ExerciseKindsDto(
    val kinds: List<ExerciseKindInfoDto> = emptyList(),
    val wordsAvailable: Int = 0
)

@Serializable
data class ExerciseRequest(
    /** null = every folder; -1 = only the words in no folder. */
    val categoryId: Int? = null,
    val scope: ExerciseScope = ExerciseScope.SAVED,
    val kinds: List<ExerciseKind> = emptyList(),
    val count: Int = 10,
    val useAi: Boolean = true
)

@Serializable
data class ExerciseBatchResponse(
    val status: String = "",
    val data: ExerciseBatchDto? = null,
    val message: String? = null
)

@Serializable
data class ExerciseKindsResponse(
    val status: String = "",
    val data: ExerciseKindsDto? = null,
    val message: String? = null
)
