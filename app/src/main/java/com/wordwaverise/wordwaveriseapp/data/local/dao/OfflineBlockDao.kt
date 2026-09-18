package com.wordwaverise.wordwaveriseapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wordwaverise.wordwaveriseapp.data.local.entity.OfflineBlockEntity

@Dao
interface OfflineBlockDao {

    @Query(
        "SELECT * FROM offline_blocks WHERE bookId = :bookId AND ordinal >= :from " +
            "ORDER BY ordinal ASC LIMIT :limit"
    )
    suspend fun page(bookId: Int, from: Int, limit: Int): List<OfflineBlockEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putAll(entities: List<OfflineBlockEntity>)

    @Query("DELETE FROM offline_blocks WHERE bookId = :bookId")
    suspend fun deleteForBook(bookId: Int)

    @Query("SELECT COUNT(*) FROM offline_blocks WHERE bookId = :bookId")
    suspend fun count(bookId: Int): Int
}
