package n.startapp.wordwaveriseapp.data.repository

import android.util.Log
import n.startapp.wordwaveriseapp.data.remote.ApiService
import n.startapp.wordwaveriseapp.data.remote.dto.WordDto
import n.startapp.wordwaveriseapp.util.NetworkError
import n.startapp.wordwaveriseapp.util.Resource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepository @Inject constructor(
    private val apiService: ApiService
) {
    companion object {
        private const val TAG = "SearchRepository"
    }

    suspend fun searchWord(word: String): Resource<WordDto> {
        return try {
            if (word.isBlank()) {
                Log.w(TAG, "Search word is blank")
                return Resource.Error("Пожалуйста, введите слово для поиска")
            }

            Log.d(TAG, "Searching for word: $word")
            val response = apiService.searchWord(word.trim().lowercase())

            Log.d(TAG, "Response status: ${response.status}")
            Log.d(TAG, "Response data: ${response.data}")

            if (response.status == "ok" && response.data != null) {
                Log.d(TAG, "Successfully found word: ${response.data.word}")
                Resource.Success(response.data)
            } else {
                Log.w(TAG, "Word not found or status not ok. Status: ${response.status}")
                Resource.Error("Слово не найдено")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching word: ${e.message}", e)
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }
}
