package n.startapp.wordwaveriseapp.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import n.startapp.wordwaveriseapp.data.local.AppDatabase
import n.startapp.wordwaveriseapp.data.local.TokenDataStore
import n.startapp.wordwaveriseapp.data.local.dao.FlashcardDao
import n.startapp.wordwaveriseapp.data.local.dao.SavedWordDao
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
            .fallbackToDestructiveMigration() // Для разработки - удалить для production
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
    fun provideTokenDataStore(@ApplicationContext context: Context): TokenDataStore {
        return TokenDataStore(context)
    }
}
