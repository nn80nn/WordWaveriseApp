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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepository @Inject constructor(
    private val apiService: ApiService
) {
    companion object {
        private const val TAG = "SearchRepository"

        /** A cold article takes one to three minutes to write; give up well after that. */
        private const val ANNOTATION_POLL_TIMEOUT_MS = 4 * 60 * 1000L

        /** Failures the server retries on its own, so they are worth waiting out. */
        private val TRANSIENT_ANNOTATION_FAILURES = setOf("llm_call_failed", "llm_timeout")
        private const val MAX_CONSECUTIVE_POLL_FAILURES = 3
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
     * v2 lookup, emitting each response as it arrives.
     *
     * A cold word answers PENDING with the raw data immediately and finishes the article in the
     * background, so this keeps asking until it lands. It is a Flow rather than a single value
     * because both halves of the answer improve over time: the article appears, and the raw
     * aggregate widens — the first response carries only the fast API sources, and Cambridge and
     * Oxford arrive once the background job's own fetch completes.
     */
    fun lookup(query: String): Flow<Resource<LookupResponseDto>> = flow {
        if (query.isBlank()) {
            emit(Resource.Error("Пожалуйста, введите слово для поиска"))
            return@flow
        }

        val first = try {
            apiService.lookup(query.trim())
        } catch (e: Exception) {
            Log.e(TAG, "Lookup failed for '$query': ${e.message}", e)
            emit(Resource.Error(NetworkError.getErrorMessage(e)))
            return@flow
        }

        if (first.status != "ok" || first.data == null) {
            emit(Resource.Error(first.message ?: "Слово не найдено"))
            return@flow
        }

        var current = first.data!!
        emit(Resource.Success(current))

        val deadline = System.currentTimeMillis() + ANNOTATION_POLL_TIMEOUT_MS
        var consecutiveFailures = 0

        while (System.currentTimeMillis() < deadline && shouldKeepPolling(current)) {
            delay(pollDelay(current))
            try {
                val next = apiService.lookup(query.trim())
                if (next.status == "ok" && next.data != null) {
                    current = next.data!!
                    emit(Resource.Success(current))
                    consecutiveFailures = 0
                    continue
                }
                consecutiveFailures++
            } catch (e: Exception) {
                // A blip while polling must not erase what is already on screen: the first
                // response succeeded, so the user has definitions in front of them.
                Log.d(TAG, "Poll for '$query' failed, retrying: ${e.message}")
                consecutiveFailures++
            }
            if (consecutiveFailures >= MAX_CONSECUTIVE_POLL_FAILURES) break
        }
    }

    /**
     * A degraded article is not necessarily final: the server caches a rate-limited or timed-out
     * annotation only briefly and then retries it. Giving up at the first DEGRADED is what made
     * reopening the screen produce an article that waiting never showed.
     */
    private fun shouldKeepPolling(data: LookupResponseDto): Boolean = when (data.annotationStatus) {
        "PENDING" -> true
        "DEGRADED" -> data.annotationNote in TRANSIENT_ANNOTATION_FAILURES
        else -> false
    }

    private fun pollDelay(data: LookupResponseDto): Long =
        // Nothing will change until the server's own retry window elapses, so ask less often.
        if (data.annotationStatus == "DEGRADED") 15_000L
        else (data.retryAfterMs ?: 5000).toLong().coerceIn(1500L, 10_000L)

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
