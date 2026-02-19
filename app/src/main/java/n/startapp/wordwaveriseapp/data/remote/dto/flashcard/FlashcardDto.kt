package n.startapp.wordwaveriseapp.data.remote.dto.flashcard

import kotlinx.serialization.Serializable

@Serializable
data class FlashcardDto(
    val id: Int,
    val word: String,
    val definition: String,
    val example: String? = null,
    val translation: String? = null,
    val phonetic: String? = null,
    val partOfSpeech: String? = null,
    val repetitionLevel: Int = 0,
    val lastReviewed: String? = null,
    val nextReviewDate: String,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreateFlashcardRequest(
    val word: String,
    val definition: String,
    val example: String? = null,
    val translation: String? = null,
    val phonetic: String? = null,
    val partOfSpeech: String? = null
)

@Serializable
data class UpdateFlashcardRequest(
    val repetitionLevel: Int,
    val lastReviewed: String,
    val nextReviewDate: String,
    val correctCount: Int,
    val incorrectCount: Int
)

@Serializable
data class FlashcardResponse(
    val status: String,
    val data: FlashcardDto? = null,
    val message: String? = null
)

@Serializable
data class FlashcardsResponse(
    val status: String,
    val data: FlashcardsData? = null,
    val message: String? = null
)

@Serializable
data class FlashcardsData(
    val flashcards: List<FlashcardDto> = emptyList(),
    val total: Int = 0,
    val dueCount: Int = 0
)
