package com.wordwaverise.wordwaveriseapp.data.local.dao

import androidx.room.*
import com.wordwaverise.wordwaveriseapp.data.local.entity.SavedWordEntity
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

    @Query("UPDATE saved_words SET senseId = :senseId WHERE word = :word")
    suspend fun updateSenseId(word: String, senseId: String?)

    /** То, что о слове знает сервер и не знает телефон: значение и происхождение из группы. */
    @Query(
        """
        UPDATE saved_words
        SET senseId = :senseId, groupServerId = :groupServerId, readOnly = :readOnly,
            categoryId = :categoryId
        WHERE word = :word
        """
    )
    suspend fun updateFromServer(
        word: String,
        senseId: String?,
        groupServerId: Int?,
        readOnly: Boolean,
        categoryId: Long?
    )

    @Query("SELECT COUNT(*) FROM saved_words")
    fun getCount(): Flow<Int>

    @Query("SELECT * FROM saved_words WHERE isSynced = 0")
    suspend fun getUnsyncedWords(): List<SavedWordEntity>

    @Query("DELETE FROM saved_words")
    suspend fun deleteAll()

    /**
     * Слова, которые сервер больше не отдаёт.
     *
     * ⚠️ `isSynced = 1` в условии обязателен: слово, сохранённое офлайн, сервер ещё не видел —
     * удалить его здесь значит потерять то, что человек только что добавил.
     */
    @Query("DELETE FROM saved_words WHERE isSynced = 1 AND word NOT IN (:words)")
    suspend fun deleteMissingFromServer(words: List<String>)

    @Query("DELETE FROM saved_words WHERE isSynced = 1")
    suspend fun deleteAllSynced()

    @Query("UPDATE saved_words SET categoryId = :categoryId WHERE word = :word")
    suspend fun updateCategory(word: String, categoryId: Long?)

    @Query("SELECT * FROM saved_words WHERE categoryId = :categoryId ORDER BY savedAt DESC")
    fun getWordsByCategory(categoryId: Long): Flow<List<SavedWordEntity>>
}
