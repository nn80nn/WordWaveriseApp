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

    @Query("DELETE FROM flashcards")
    suspend fun deleteAll()

    @Query("DELETE FROM flashcards WHERE word = :word")
    suspend fun deleteByWord(word: String)

    @Query("SELECT * FROM flashcards WHERE repetitionLevel < 5 ORDER BY nextReviewDate ASC LIMIT :limit")
    fun getFlashcardsForSession(limit: Int = 10): Flow<List<FlashcardEntity>>

    // ── Folders ───────────────────────────────────────────────────────────────
    //
    // `folderId` reads the same way as it does in the API: null = every folder,
    // -1 = the cards in no folder, anything else = that folder's *server* id.
    // Keeping the convention identical is what lets the phone and the browser
    // agree on what "эта папка" contains.

    @Query(
        """
        SELECT * FROM flashcards
        WHERE repetitionLevel < 5
          AND (:folderId IS NULL
               OR (:folderId = -1 AND serverCategoryId IS NULL)
               OR serverCategoryId = :folderId)
        ORDER BY nextReviewDate ASC LIMIT :limit
        """
    )
    fun getFlashcardsForSessionInFolder(folderId: Int?, limit: Int = 10): Flow<List<FlashcardEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM flashcards
        WHERE nextReviewDate <= :currentTime
          AND (:folderId IS NULL
               OR (:folderId = -1 AND serverCategoryId IS NULL)
               OR serverCategoryId = :folderId)
        """
    )
    fun getDueCountInFolder(folderId: Int?, currentTime: Long = System.currentTimeMillis()): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM flashcards
        WHERE :folderId IS NULL
           OR (:folderId = -1 AND serverCategoryId IS NULL)
           OR serverCategoryId = :folderId
        """
    )
    fun getTotalCountInFolder(folderId: Int?): Flow<Int>

    @Query("SELECT * FROM flashcards WHERE serverId = :serverId LIMIT 1")
    suspend fun getFlashcardByServerId(serverId: Int): FlashcardEntity?
}
