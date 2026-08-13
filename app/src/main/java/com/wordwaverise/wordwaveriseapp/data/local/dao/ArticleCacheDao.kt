package com.wordwaverise.wordwaveriseapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wordwaverise.wordwaveriseapp.data.local.entity.ArticleCacheEntity

@Dao
interface ArticleCacheDao {

    @Query("SELECT * FROM article_cache WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): ArticleCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: ArticleCacheEntity)

    @Query("DELETE FROM article_cache")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM article_cache")
    suspend fun count(): Int
}
