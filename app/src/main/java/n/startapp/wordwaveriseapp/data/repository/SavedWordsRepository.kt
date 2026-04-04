package n.startapp.wordwaveriseapp.data.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import n.startapp.wordwaveriseapp.data.local.TokenDataStore
import n.startapp.wordwaveriseapp.data.local.dao.SavedWordDao
import n.startapp.wordwaveriseapp.data.local.entity.SavedWordEntity
import n.startapp.wordwaveriseapp.data.remote.ApiService
import n.startapp.wordwaveriseapp.data.remote.dto.saved.SaveWordRequest
import n.startapp.wordwaveriseapp.util.NetworkError
import n.startapp.wordwaveriseapp.util.Resource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavedWordsRepository @Inject constructor(
    private val apiService: ApiService,
    private val savedWordDao: SavedWordDao,
    private val tokenDataStore: TokenDataStore
) {
    companion object {
        private const val TAG = "SavedWordsRepository"
    }

    val savedWords: Flow<List<SavedWordEntity>> = savedWordDao.getAllSavedWords()

    suspend fun saveWord(
        word: String,
        translation: String? = null,
        definition: String? = null
    ): Resource<Boolean> {
        return try {
            Log.d(TAG, "Saving word: $word")

            // Save locally first
            val entity = SavedWordEntity(word = word, isSynced = false)
            savedWordDao.insertWord(entity)
            Log.d(TAG, "Word saved locally: $word")

            // Try to sync with server if user is logged in
            val token = tokenDataStore.token.firstOrNull()
            if (!token.isNullOrEmpty()) {
                try {
                    Log.d(TAG, "Syncing word to server: $word")
                    val response = apiService.saveWord(
                        "Bearer $token",
                        SaveWordRequest(word, translation, definition)
                    )

                    if (response.status == "ok" && response.data != null) {
                        val serverId = response.data.id
                        savedWordDao.updateSyncStatus(word, true, serverId)
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

    suspend fun syncWords(): Boolean {
        try {
            val token = tokenDataStore.token.firstOrNull()
            if (token.isNullOrEmpty()) {
                Log.d(TAG, "No token, skipping sync")
                return true
            }

            Log.d(TAG, "Starting words synchronization")

            val response = apiService.getSavedWords("Bearer $token")
            if (response.status == "ok" && response.data != null) {
                val serverWords = response.data.words

                serverWords.forEach { serverWord ->
                    val existingWord = savedWordDao.getSavedWord(serverWord.word)
                    if (existingWord == null) {
                        savedWordDao.insertWord(
                            SavedWordEntity(
                                word = serverWord.word,
                                savedAt = System.currentTimeMillis(),
                                serverId = serverWord.id,
                                isSynced = true
                            )
                        )
                    }
                }

                // Sync local unsynced words to server
                val unsyncedWords = savedWordDao.getUnsyncedWords()
                unsyncedWords.forEach { localWord ->
                    try {
                        val saveResponse = apiService.saveWord(
                            "Bearer $token",
                            SaveWordRequest(localWord.word)
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
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Sync error: ${e.message}", e)
            return false
        }
    }

    suspend fun isWordSaved(word: String): Boolean {
        return savedWordDao.getSavedWord(word) != null
    }

    suspend fun updateWordCategory(word: String, categoryId: Long?) {
        savedWordDao.updateCategory(word, categoryId)
    }
}
