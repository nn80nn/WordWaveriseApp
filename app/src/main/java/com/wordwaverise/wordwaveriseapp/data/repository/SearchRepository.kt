package com.wordwaverise.wordwaveriseapp.data.repository

import android.util.Log
import com.wordwaverise.wordwaveriseapp.data.remote.ApiService
import com.wordwaverise.wordwaveriseapp.data.remote.dto.WordDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.ContextAnalysisDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.ContextAnalyzeRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.LookupResponseDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.RuEnCandidatesDto
import com.wordwaverise.wordwaveriseapp.util.NetworkError
import com.wordwaverise.wordwaveriseapp.util.Resource
import kotlinx.coroutines.delay
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

    suspend fun getAiSummary(word: String): Resource<String> {
        return try {
            val response = apiService.getAiSummary(word)
            if (response.status == "ok" && response.data != null)
                Resource.Success(response.data.result)
            else Resource.Error(response.message ?: "AI error")
        } catch (e: Exception) {
            Log.d(TAG, "AI summary failed for '$word': ${e.message}")
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }

    /**
     * v2 lookup: resolves the query and returns the annotated article.
     *
     * A cold word answers PENDING with the raw data while annotation finishes in the background,
     * so we re-issue once after the server's own retryAfterMs. The retry lives here rather than
     * in the ViewModel so callers see a single result, and it is capped at one attempt — a second
     * PENDING means the article is slow, not lost, and the raw data is already on screen.
     */
    suspend fun lookup(query: String): Resource<LookupResponseDto> {
        if (query.isBlank()) return Resource.Error("Пожалуйста, введите слово для поиска")

        return try {
            val first = apiService.lookup(query.trim())
            if (first.status != "ok" || first.data == null) {
                return Resource.Error(first.message ?: "Слово не найдено")
            }

            val data = first.data
            if (data.annotationStatus != "PENDING") return Resource.Success(data)

            delay((data.retryAfterMs ?: 2500).toLong().coerceIn(500L, 6000L))
            val second = apiService.lookup(query.trim())
            Resource.Success(second.data ?: data)
        } catch (e: Exception) {
            Log.e(TAG, "Lookup failed for '$query': ${e.message}", e)
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }

    suspend fun analyzeInContext(text: String, tokenIndex: Int): Resource<ContextAnalysisDto> {
        return try {
            val response = apiService.analyzeInContext(ContextAnalyzeRequest(text, tokenIndex))
            if (response.status == "ok" && response.data != null) Resource.Success(response.data)
            else Resource.Error(response.message ?: "Не удалось разобрать слово в контексте")
        } catch (e: Exception) {
            Log.e(TAG, "Context analysis failed: ${e.message}", e)
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }

    suspend fun translateRuEn(query: String): Resource<RuEnCandidatesDto> {
        return try {
            val response = apiService.translateRuEn(query.trim())
            if (response.status == "ok" && response.data != null) Resource.Success(response.data)
            else Resource.Error(response.message ?: "Перевод не найден")
        } catch (e: Exception) {
            Log.e(TAG, "RU→EN translation failed for '$query': ${e.message}", e)
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }

    suspend fun getSuggestions(query: String, prefix: Boolean = false): List<String> {
        return try {
            val response = apiService.getSuggestions(query.trim(), prefix = prefix)
            if (response.status == "ok") response.data?.suggestions.orEmpty()
            else emptyList()
        } catch (e: Exception) {
            Log.d(TAG, "Suggestions failed for '$query': ${e.message}")
            emptyList()
        }
    }
}
