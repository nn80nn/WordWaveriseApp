package com.wordwaverise.wordwaveriseapp.data.remote.dto.flashcard

import kotlinx.serialization.Serializable

@Serializable
data class FlashcardDto(
    val id: Int,
    val word: String,
    val translation: String? = null,
    val definition: String? = null,
    val example: String? = null,
    val nextReview: String,
    val daysUntilReview: Int = 0
)

@Serializable
data class CreateFlashcardRequest(
    val word: String,
    val translation: String? = null,
    val definition: String? = null,
    val example: String? = null
)

@Serializable
data class CreateFlashcardFromSavedRequest(
    val savedWordId: Int
)

@Serializable
data class UpdateFlashcardRequest(
    val difficulty: String  // "AGAIN", "HARD", "GOOD", "EASY"
)

// Response for GET /api/flashcards (data is array)
@Serializable
data class FlashcardsListResponse(
    val status: String,
    val data: List<FlashcardDto>? = null,
    val message: String? = null
)

// Response for GET /api/flashcards/due (data is object)
@Serializable
data class DueFlashcardsData(
    val cards: List<FlashcardDto> = emptyList(),
    val totalDue: Int = 0
)

@Serializable
data class DueFlashcardsResponse(
    val status: String,
    val data: DueFlashcardsData? = null,
    val message: String? = null
)

// Response for single flashcard operations (POST, DELETE, POST /create)
@Serializable
data class FlashcardResponse(
    val status: String,
    val data: FlashcardDto? = null,
    val message: String? = null
)

// Response for PUT /api/flashcards/{id}
@Serializable
data class UpdateProgressData(
    val cardId: Int,
    val nextReview: String,
    val interval: Int,
    val message: String? = null
)

@Serializable
data class UpdateFlashcardResponse(
    val status: String,
    val data: UpdateProgressData? = null,
    val message: String? = null
)

// Response for GET /api/flashcards/statistics
@Serializable
data class FlashcardStatistics(
    val totalCards: Int = 0,
    val dueCards: Int = 0,
    val learnedCards: Int = 0,
    val newCards: Int = 0,
    val reviewingCards: Int = 0
)

@Serializable
data class FlashcardStatisticsResponse(
    val status: String,
    val data: FlashcardStatistics? = null,
    val message: String? = null
)
