package com.wordwaverise.wordwaveriseapp.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.wordwaverise.wordwaveriseapp.data.local.AppDatabase
import com.wordwaverise.wordwaveriseapp.data.local.MIGRATION_1_2
import com.wordwaverise.wordwaveriseapp.data.local.MIGRATION_2_3
import com.wordwaverise.wordwaveriseapp.data.local.MIGRATION_3_4
import com.wordwaverise.wordwaveriseapp.data.local.TokenDataStore
import com.wordwaverise.wordwaveriseapp.data.local.dao.CategoryDao
import com.wordwaverise.wordwaveriseapp.data.local.dao.FlashcardDao
import com.wordwaverise.wordwaveriseapp.data.local.dao.SavedWordDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "wordwaverise_database"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()
    }

    @Provides
    @Singleton
    fun provideSavedWordDao(database: AppDatabase): SavedWordDao {
        return database.savedWordDao()
    }

    @Provides
    @Singleton
    fun provideFlashcardDao(database: AppDatabase): FlashcardDao {
        return database.flashcardDao()
    }

    @Provides
    @Singleton
    fun provideCategoryDao(database: AppDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    @Singleton
    fun provideTokenDataStore(@ApplicationContext context: Context): TokenDataStore {
        return TokenDataStore(context)
    }
}
