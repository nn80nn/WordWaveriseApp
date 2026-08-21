package com.wordwaverise.wordwaveriseapp.data.local.dao

import androidx.room.*
import com.wordwaverise.wordwaveriseapp.data.local.entity.SavedWordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedWordDao {
    @Query("SELECT * FROM saved_words ORDER BY savedAt DESC")
    fun getAllSavedWords(): Flow<List<SavedWordEntity>>

    /**
     * Любая запись этого слова.
     *
     * ⚠️ «Слово сохранено?» и «это значение сохранено?» — разные вопросы, и с тех пор как у
     * слова может быть несколько записей, первый отвечает только на закладку у заголовка.
     * Для закладки на значении есть [getEntry].
     */
    @Query("SELECT * FROM saved_words WHERE word = :word LIMIT 1")
    suspend fun getSavedWord(word: String): SavedWordEntity?

    /** Все записи этого слова — по одной на сохранённое значение. */
    @Query("SELECT * FROM saved_words WHERE word = :word")
    suspend fun getEntries(word: String): List<SavedWordEntity>

    /** Ровно эта запись: слово плюс значение. */
    @Query(
        """
        SELECT * FROM saved_words
        WHERE word = :word AND ((:senseId IS NULL AND senseId IS NULL) OR senseId = :senseId)
        LIMIT 1
        """
    )
    suspend fun getEntry(word: String, senseId: String?): SavedWordEntity?

    @Query("SELECT * FROM saved_words WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Int): SavedWordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: SavedWordEntity): Long

    @Update
    suspend fun updateWord(word: SavedWordEntity)

    @Delete
    suspend fun deleteWord(word: SavedWordEntity)

    /** Всё написание целиком — все его значения. */
    @Query("DELETE FROM saved_words WHERE word = :word")
    suspend fun deleteWordByName(word: String)

    /** Одна запись — одно значение; остальные значения того же слова остаются. */
    @Query("DELETE FROM saved_words WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE saved_words SET isSynced = :isSynced, serverId = :serverId WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, isSynced: Boolean, serverId: Int?)

    @Query("SELECT COUNT(*) FROM saved_words")
    fun getCount(): Flow<Int>

    @Query("SELECT * FROM saved_words WHERE isSynced = 0")
    suspend fun getUnsyncedWords(): List<SavedWordEntity>

    @Query("DELETE FROM saved_words")
    suspend fun deleteAll()

    /**
     * Записи, которых сервер больше не отдаёт.
     *
     * ⚠️ `isSynced = 1` в условии обязателен: слово, сохранённое офлайн, сервер ещё не видел —
     * удалить его здесь значит потерять то, что человек только что добавил. Сверка идёт по
     * серверному id, а не по написанию: одно написание теперь живёт в нескольких записях, и
     * «сервер прислал это слово» больше не значит «сервер прислал именно эту запись».
     */
    @Query("DELETE FROM saved_words WHERE isSynced = 1 AND (serverId IS NULL OR serverId NOT IN (:serverIds))")
    suspend fun deleteMissingFromServer(serverIds: List<Int>)

    @Query("DELETE FROM saved_words WHERE isSynced = 1")
    suspend fun deleteAllSynced()

    @Query("UPDATE saved_words SET categoryIds = :categoryIds WHERE id = :id")
    suspend fun updateCategories(id: Long, categoryIds: List<Long>)
}
