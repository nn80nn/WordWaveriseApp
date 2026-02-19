package n.startapp.wordwaveriseapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val word: String,
    val definition: String,
    val example: String? = null,
    val translation: String? = null,
    val phonetic: String? = null,
    val partOfSpeech: String? = null,

    // Spaced repetition fields
    val repetitionLevel: Int = 0, // 0-5 (0 = новая, 5 = освоена)
    val lastReviewed: Long? = null,
    val nextReviewDate: Long = System.currentTimeMillis(),
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
