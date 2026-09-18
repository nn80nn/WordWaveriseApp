package com.wordwaverise.wordwaveriseapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wordwaverise.wordwaveriseapp.data.local.entity.OfflineBookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineBookDao {

    /** Живой прогресс для экрана: download-иконка перерисовывается сама, пока джоб идёт. */
    @Query("SELECT * FROM offline_books WHERE bookId = :bookId")
    fun observe(bookId: Int): Flow<OfflineBookEntity?>

    @Query("SELECT * FROM offline_books")
    fun observeAll(): Flow<List<OfflineBookEntity>>

    @Query("SELECT * FROM offline_books WHERE bookId = :bookId")
    suspend fun get(bookId: Int): OfflineBookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: OfflineBookEntity)

    @Query("DELETE FROM offline_books WHERE bookId = :bookId")
    suspend fun delete(bookId: Int)
}
