package com.wordwaverise.wordwaveriseapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wordwaverise.wordwaveriseapp.data.local.entity.OfflineHintEntity

@Dao
interface OfflineHintDao {

    @Query(
        "SELECT * FROM offline_hints WHERE bookId = :bookId AND blockOrdinal = :blockOrdinal " +
            "AND sentenceIndex = :sentenceIndex AND tokenIndex = :tokenIndex LIMIT 1"
    )
    suspend fun get(bookId: Int, blockOrdinal: Int, sentenceIndex: Int, tokenIndex: Int): OfflineHintEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putAll(entities: List<OfflineHintEntity>)

    @Query("DELETE FROM offline_hints WHERE bookId = :bookId")
    suspend fun deleteForBook(bookId: Int)
}
