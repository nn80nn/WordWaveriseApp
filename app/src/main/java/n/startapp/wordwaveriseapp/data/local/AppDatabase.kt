package n.startapp.wordwaveriseapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import n.startapp.wordwaveriseapp.data.local.dao.CategoryDao
import n.startapp.wordwaveriseapp.data.local.dao.FlashcardDao
import n.startapp.wordwaveriseapp.data.local.dao.SavedWordDao
import n.startapp.wordwaveriseapp.data.local.entity.CategoryEntity
import n.startapp.wordwaveriseapp.data.local.entity.FlashcardEntity
import n.startapp.wordwaveriseapp.data.local.entity.SavedWordEntity

@Database(
    entities = [SavedWordEntity::class, FlashcardEntity::class, CategoryEntity::class],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedWordDao(): SavedWordDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun categoryDao(): CategoryDao
}
