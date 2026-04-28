package com.wordwaverise.wordwaveriseapp.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.wordwaverise.wordwaveriseapp.data.local.entity.FlashcardEntity

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards ORDER BY nextReviewDate ASC")
    fun getAllFlashcards(): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE nextReviewDate <= :currentTime ORDER BY nextReviewDate ASC")
    fun getDueFlashcards(currentTime: Long = System.currentTimeMillis()): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE word = :word LIMIT 1")
    suspend fun getFlashcardByWord(word: String): FlashcardEntity?

    @Query("SELECT COUNT(*) FROM flashcards WHERE nextReviewDate <= :currentTime")
    fun getDueCount(currentTime: Long = System.currentTimeMillis()): Flow<Int>

    @Query("SELECT COUNT(*) FROM flashcards")
    fun getTotalCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: FlashcardEntity): Long

    @Update
    suspend fun updateFlashcard(flashcard: FlashcardEntity)

    @Delete
    suspend fun deleteFlashcard(flashcard: FlashcardEntity)

    @Query("DELETE FROM flashcards WHERE word = :word")
    suspend fun deleteByWord(word: String)

    @Query("SELECT * FROM flashcards WHERE repetitionLevel < 5 ORDER BY nextReviewDate ASC LIMIT :limit")
    fun getFlashcardsForSession(limit: Int = 10): Flow<List<FlashcardEntity>>
}
