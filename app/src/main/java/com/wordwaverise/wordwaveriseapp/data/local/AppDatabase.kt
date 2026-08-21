package com.wordwaverise.wordwaveriseapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.wordwaverise.wordwaveriseapp.data.local.dao.ArticleCacheDao
import com.wordwaverise.wordwaveriseapp.data.local.dao.CategoryDao
import com.wordwaverise.wordwaveriseapp.data.local.dao.FlashcardDao
import com.wordwaverise.wordwaveriseapp.data.local.dao.SavedWordDao
import com.wordwaverise.wordwaveriseapp.data.local.entity.ArticleCacheEntity
import com.wordwaverise.wordwaveriseapp.data.local.entity.CategoryEntity
import com.wordwaverise.wordwaveriseapp.data.local.entity.FlashcardEntity
import com.wordwaverise.wordwaveriseapp.data.local.entity.SavedWordEntity

@Database(
    entities = [
        SavedWordEntity::class,
        FlashcardEntity::class,
        CategoryEntity::class,
        ArticleCacheEntity::class
    ],
    version = 11,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedWordDao(): SavedWordDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun categoryDao(): CategoryDao
    abstract fun articleCacheDao(): ArticleCacheDao
}
