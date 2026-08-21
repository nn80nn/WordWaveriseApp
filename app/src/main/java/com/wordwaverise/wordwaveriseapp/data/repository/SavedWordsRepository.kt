package com.wordwaverise.wordwaveriseapp.data.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import com.wordwaverise.wordwaveriseapp.data.local.TokenDataStore
import com.wordwaverise.wordwaveriseapp.data.local.dao.CategoryDao
import com.wordwaverise.wordwaveriseapp.data.local.dao.SavedWordDao
import com.wordwaverise.wordwaveriseapp.data.local.entity.SavedWordEntity
import com.wordwaverise.wordwaveriseapp.data.remote.ApiService
import com.wordwaverise.wordwaveriseapp.data.remote.dto.saved.SaveWordRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.saved.SetWordFoldersRequest
import com.wordwaverise.wordwaveriseapp.util.NetworkError
import com.wordwaverise.wordwaveriseapp.util.Resource
import com.wordwaverise.wordwaveriseapp.util.SyncResult
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavedWordsRepository @Inject constructor(
    private val apiService: ApiService,
    private val savedWordDao: SavedWordDao,
    private val categoryDao: CategoryDao,
    private val tokenDataStore: TokenDataStore
) {
    companion object {
        private const val TAG = "SavedWordsRepository"
    }

    val savedWords: Flow<List<SavedWordEntity>> = savedWordDao.getAllSavedWords()

    /**
     * Saves a word, optionally pinned to one sense of its article.
     *
     * The same three outcomes the server has, so the phone does not show something the next
     * sync will contradict: this exact sense already saved → nothing changes; the word is saved
     * with *no* sense and one is now given → that row is filled in; otherwise → a new record.
     *
     * ⚠️ Отметить второе значение — это второе слово, а не смена решения о первом. Раньше
     * вторая закладка переставляла привязку, то есть выглядела как добавление и была отменой.
     */
    suspend fun saveWord(
        word: String,
        translation: String? = null,
        definition: String? = null,
        senseId: String? = null
    ): Resource<Boolean> {
        return try {
            Log.d(TAG, "Saving word: $word (sense=${senseId ?: "—"})")

            val exact = savedWordDao.getEntry(word, senseId)
            val unpinned = if (senseId != null) savedWordDao.getEntry(word, null) else null
            val target = exact ?: unpinned

            val localId = if (target != null) {
                savedWordDao.insertWord(
                    target.copy(
                        isSynced = false,
                        senseId = senseId ?: target.senseId,
                        // Перевод с экрана статьи — запасной вариант: пустым он строку не портит,
                        // а сервер всё равно пришлёт свой на ближайшей синхронизации.
                        translation = translation ?: target.translation
                    )
                )
                target.id
            } else {
                savedWordDao.insertWord(
                    SavedWordEntity(
                        word = word,
                        translation = translation,
                        isSynced = false,
                        senseId = senseId
                    )
                )
            }
            Log.d(TAG, "Word saved locally: $word")

            // Try to sync with server if user is logged in
            val token = tokenDataStore.token.firstOrNull()
            if (!token.isNullOrEmpty()) {
                try {
                    Log.d(TAG, "Syncing word to server: $word")
                    val response = apiService.saveWord(
                        "Bearer $token",
                        SaveWordRequest(word, translation, definition, senseId)
                    )

                    if (response.status == "ok" && response.data != null) {
                        val saved = savedWordDao.getEntry(word, senseId ?: response.data.senseId)
                        // Значение приходит обратно от сервера: он мог отказать в привязке,
                        // если статьи ещё нет, и локально это не должно выглядеть иначе.
                        savedWordDao.insertWord(
                            (saved ?: savedWordDao.getEntry(word, senseId))
                                ?.copy(
                                    isSynced = true,
                                    serverId = response.data.id,
                                    senseId = response.data.senseId ?: senseId
                                )
                                ?: SavedWordEntity(
                                    id = localId,
                                    word = word,
                                    isSynced = true,
                                    serverId = response.data.id,
                                    senseId = response.data.senseId ?: senseId
                                )
                        )
                        Log.d(TAG, "Word synced successfully: $word (serverId: ${response.data.id})")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to sync word to server: ${e.message}")
                }
            }

            Resource.Success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving word: ${e.message}", e)
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }

    /** Значение статьи, к которому привязано слово, — статья открывает его первым. */
    suspend fun pinnedSenseId(word: String): String? = savedWordDao.getSavedWord(word)?.senseId

    /**
     * Значения этого слова, которые человек сохранил.
     *
     * Список, а не одно: закладка стоит на каждом сохранённом значении, и показать её только
     * на одном означало бы, что второе сохранение потерялось.
     */
    suspend fun pinnedSenseIds(word: String): List<String> =
        savedWordDao.getEntries(word).mapNotNull { it.senseId }

    /** Ровно эта запись: слово плюс значение. */
    suspend fun entryFor(word: String, senseId: String?): SavedWordEntity? =
        savedWordDao.getEntry(word, senseId)

    /** Убирает всё написание — все его значения. */
    suspend fun deleteWord(word: String): Resource<Boolean> {
        return try {
            Log.d(TAG, "Deleting word: $word")

            val token = tokenDataStore.token.firstOrNull()
            if (!token.isNullOrEmpty()) {
                try {
                    apiService.deleteSavedWord("Bearer $token", word)
                    Log.d(TAG, "Word deleted from server: $word")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete word from server: ${e.message}")
                }
            }

            savedWordDao.deleteWordByName(word)
            Resource.Success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting word: ${e.message}", e)
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }

    /**
     * Убирает одну запись — одно значение.
     *
     * Слово, о котором сервер ещё не знает, удаляется только локально: без серверного id
     * рассказать серверу нечего, а держать строку на телефоне ради этого — значит показывать
     * человеку то, что он уже убрал.
     */
    suspend fun deleteEntry(entry: SavedWordEntity): Resource<Boolean> {
        return try {
            val token = tokenDataStore.token.firstOrNull()
            val serverId = entry.serverId
            if (!token.isNullOrEmpty() && serverId != null) {
                try {
                    apiService.deleteSavedEntry("Bearer $token", serverId)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete entry from server: ${e.message}")
                }
            }
            savedWordDao.deleteById(entry.id)
            Resource.Success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting entry: ${e.message}", e)
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }

    suspend fun syncWords(): SyncResult {
        try {
            val token = tokenDataStore.token.firstOrNull()
            if (token.isNullOrEmpty()) {
                Log.d(TAG, "No token, skipping sync")
                return SyncResult.SUCCESS
            }

            Log.d(TAG, "Starting words synchronization")

            val response = apiService.getSavedWords("Bearer $token")
            if (response.status == "ok" && response.data != null) {
                // ⚠️ Сверка по серверному id, а не по написанию. Одно написание теперь живёт
                // в стольких записях, сколько значений человек отметил, и «то же слово» больше
                // не значит «та же строка»: раньше вторая запись затирала первую вместе с
                // папкой, потому что ключом было само слово.
                val serverWords = response.data.words.distinctBy { it.id }

                serverWords.forEach { serverWord ->
                    // Папку слово тоже приносит с сервера — и только так о ней вообще можно
                    // узнать: локальный id ставился раньше лишь при переносе на этом устройстве.
                    // Папка, до которой синхронизация категорий ещё не дошла, просто пропускается
                    // — следующий проход поправит.
                    val localCategoryIds = serverWord.categoryIds
                        .ifEmpty { listOfNotNull(serverWord.categoryId) }
                        .mapNotNull { categoryDao.getByServerId(it)?.id }

                    val existing = savedWordDao.getByServerId(serverWord.id)
                        // Слово, сохранённое офлайн, сервера ещё не видело: связываем строку,
                        // а не заводим рядом вторую такую же.
                        ?: savedWordDao.getEntry(serverWord.word, serverWord.senseId)
                            ?.takeIf { it.serverId == null }

                    val row = (existing ?: SavedWordEntity(word = serverWord.word)).copy(
                        word = serverWord.word,
                        translation = serverWord.translation,
                        serverId = serverWord.id,
                        isSynced = true,
                        senseId = serverWord.senseId,
                        categoryIds = localCategoryIds,
                        groupServerId = serverWord.groupId,
                        readOnly = serverWord.readOnly
                    )
                    if (row != existing) savedWordDao.insertWord(row)
                }

                // ⚠️ Синхронизация обязана уметь и вычитать. Раньше она только добавляла,
                // поэтому слово, удалённое в браузере, жило на телефоне вечно и возвращалось
                // в списки — выглядело как «удаление не работает».
                val serverIds = serverWords.map { it.id }
                if (serverIds.isEmpty()) savedWordDao.deleteAllSynced()
                else savedWordDao.deleteMissingFromServer(serverIds)

                // Sync local unsynced words to server
                val unsyncedWords = savedWordDao.getUnsyncedWords()
                unsyncedWords.forEach { localWord ->
                    try {
                        val saveResponse = apiService.saveWord(
                            "Bearer $token",
                            SaveWordRequest(localWord.word, senseId = localWord.senseId)
                        )
                        if (saveResponse.status == "ok" && saveResponse.data != null) {
                            savedWordDao.updateSyncStatus(localWord.id, true, saveResponse.data.id)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to sync word ${localWord.word}: ${e.message}")
                    }
                }

                Log.d(TAG, "Synchronization completed")
            }
            return SyncResult.SUCCESS
        } catch (e: IOException) {
            Log.w(TAG, "Sync skipped, network unreachable: ${e.message}")
            return SyncResult.OFFLINE
        } catch (e: Exception) {
            Log.e(TAG, "Sync error: ${e.message}", e)
            return SyncResult.FAILED
        }
    }

    suspend fun isWordSaved(word: String): Boolean {
        return savedWordDao.getSavedWord(word) != null
    }

    /**
     * Раскладывает одну запись по папкам — сразу по всем, которые человек отметил.
     *
     * Локально пишется в любом случае: без сети раскладка всё равно должна быть видна, а
     * следующая синхронизация возьмёт правду с сервера.
     */
    suspend fun setFolders(
        entry: SavedWordEntity,
        localIds: List<Long>,
        serverIds: List<Int>
    ): Resource<Boolean> {
        return try {
            savedWordDao.updateCategories(entry.id, localIds)

            val token = tokenDataStore.token.firstOrNull()
            val serverId = entry.serverId
            if (!token.isNullOrEmpty() && serverId != null) {
                try {
                    apiService.setWordFolders(
                        "Bearer $token",
                        serverId,
                        SetWordFoldersRequest(serverIds)
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to set folders for ${entry.word}: ${e.message}")
                }
            }
            Resource.Success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting folders: ${e.message}", e)
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }
}
