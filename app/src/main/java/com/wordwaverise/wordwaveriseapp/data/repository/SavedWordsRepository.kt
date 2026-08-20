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
     * ⚠️ The local row is *merged*, not replaced. `insertWord` is an upsert on the headword, so
     * building a bare `SavedWordEntity` here threw away the folder and the server id every time
     * the same word was saved again — which is now an ordinary act, since changing your mind
     * about which sense you meant is a second save of the same word.
     */
    suspend fun saveWord(
        word: String,
        translation: String? = null,
        definition: String? = null,
        senseId: String? = null
    ): Resource<Boolean> {
        return try {
            Log.d(TAG, "Saving word: $word (sense=${senseId ?: "—"})")

            val existing = savedWordDao.getSavedWord(word)
            savedWordDao.insertWord(
                existing?.copy(isSynced = false, senseId = senseId ?: existing.senseId)
                    ?: SavedWordEntity(word = word, isSynced = false, senseId = senseId)
            )
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
                        val serverId = response.data.id
                        savedWordDao.updateSyncStatus(word, true, serverId)
                        // Значение приходит обратно от сервера: он мог отказать в привязке,
                        // если статьи ещё нет, и локально это не должно выглядеть иначе.
                        savedWordDao.updateSenseId(word, response.data.senseId ?: senseId)
                        Log.d(TAG, "Word synced successfully: $word (serverId: $serverId)")
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

    suspend fun deleteWord(word: String): Resource<Boolean> {
        return try {
            Log.d(TAG, "Deleting word: $word")

            val token = tokenDataStore.token.firstOrNull()
            if (!token.isNullOrEmpty()) {
                try {
                    Log.d(TAG, "Deleting word from server: $word")
                    apiService.deleteSavedWord("Bearer $token", word)
                    Log.d(TAG, "Word deleted from server: $word")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete word from server: ${e.message}")
                }
            }

            savedWordDao.deleteWordByName(word)
            Log.d(TAG, "Word deleted locally: $word")

            Resource.Success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting word: ${e.message}", e)
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
                // ⚠️ Первичный ключ здесь — само написание слова, а `insertWord` это upsert.
                // Сервер обещает не возвращать один заголовок дважды; если обещание когда-нибудь
                // нарушится, вторая строка не встанет рядом с первой, а затрёт её — вместе с
                // папкой. Дешевле отбросить дубль здесь, чем разбираться потом.
                val serverWords = response.data.words.distinctBy { it.word }

                serverWords.forEach { serverWord ->
                    val existingWord = savedWordDao.getSavedWord(serverWord.word)
                    // Папку слово тоже приносит с сервера — и только так о ней вообще можно
                    // узнать: локальный id ставился раньше лишь при переносе на этом устройстве,
                    // поэтому слово из папки класса оказывалось «без папки» и под её чипом не
                    // показывалось. Папка, до которой синхронизация категорий ещё не дошла,
                    // оставляет null — следующий проход поправит.
                    val localCategoryId = serverWord.categoryId
                        ?.let { categoryDao.getByServerId(it)?.id }
                    if (existingWord == null) {
                        savedWordDao.insertWord(
                            SavedWordEntity(
                                word = serverWord.word,
                                savedAt = System.currentTimeMillis(),
                                serverId = serverWord.id,
                                isSynced = true,
                                senseId = serverWord.senseId,
                                categoryId = localCategoryId,
                                groupServerId = serverWord.groupId,
                                readOnly = serverWord.readOnly
                            )
                        )
                    } else if (
                        existingWord.senseId != serverWord.senseId ||
                        existingWord.groupServerId != serverWord.groupId ||
                        existingWord.readOnly != serverWord.readOnly ||
                        existingWord.categoryId != localCategoryId
                    ) {
                        // Всё это меняется в браузере: значение переставляют, папку выдают
                        // группе и снимают с неё. Сервер здесь источник правды.
                        savedWordDao.updateFromServer(
                            serverWord.word,
                            serverWord.senseId,
                            serverWord.groupId,
                            serverWord.readOnly,
                            localCategoryId
                        )
                    }
                }

                // ⚠️ Синхронизация обязана уметь и вычитать. Раньше она только добавляла,
                // поэтому слово, удалённое в браузере, жило на телефоне вечно и возвращалось
                // в списки — выглядело как «удаление не работает».
                val serverKeys = serverWords.map { it.word }
                if (serverKeys.isEmpty()) savedWordDao.deleteAllSynced()
                else savedWordDao.deleteMissingFromServer(serverKeys)

                // Sync local unsynced words to server
                val unsyncedWords = savedWordDao.getUnsyncedWords()
                unsyncedWords.forEach { localWord ->
                    try {
                        val saveResponse = apiService.saveWord(
                            "Bearer $token",
                            SaveWordRequest(localWord.word, senseId = localWord.senseId)
                        )
                        if (saveResponse.status == "ok" && saveResponse.data != null) {
                            savedWordDao.updateSyncStatus(
                                localWord.word,
                                true,
                                saveResponse.data.id
                            )
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

    suspend fun updateWordCategory(word: String, categoryId: Long?) {
        savedWordDao.updateCategory(word, categoryId)
    }
}
