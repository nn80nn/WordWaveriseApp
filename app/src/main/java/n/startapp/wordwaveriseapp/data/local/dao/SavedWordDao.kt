package n.startapp.wordwaveriseapp.data.local.dao

import androidx.room.*
import n.startapp.wordwaveriseapp.data.local.entity.SavedWordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedWordDao {
    @Query("SELECT * FROM saved_words ORDER BY savedAt DESC")
    fun getAllSavedWords(): Flow<List<SavedWordEntity>>

    @Query("SELECT * FROM saved_words WHERE word = :word LIMIT 1")
    suspend fun getSavedWord(word: String): SavedWordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: SavedWordEntity)

    @Delete
    suspend fun deleteWord(word: SavedWordEntity)

    @Query("DELETE FROM saved_words WHERE word = :word")
    suspend fun deleteWordByName(word: String)

    @Query("UPDATE saved_words SET isSynced = :isSynced, serverId = :serverId WHERE word = :word")
    suspend fun updateSyncStatus(word: String, isSynced: Boolean, serverId: Int?)

    @Query("SELECT COUNT(*) FROM saved_words")
    fun getCount(): Flow<Int>

    @Query("SELECT * FROM saved_words WHERE isSynced = 0")
    suspend fun getUnsyncedWords(): List<SavedWordEntity>

    @Query("DELETE FROM saved_words")
    suspend fun deleteAll()
}
