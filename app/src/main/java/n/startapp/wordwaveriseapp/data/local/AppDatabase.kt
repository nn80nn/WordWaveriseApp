package n.startapp.wordwaveriseapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import n.startapp.wordwaveriseapp.data.local.dao.SavedWordDao
import n.startapp.wordwaveriseapp.data.local.entity.SavedWordEntity

@Database(
    entities = [SavedWordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedWordDao(): SavedWordDao
}
