package n.startapp.wordwaveriseapp.data.repository

import android.util.Log
import kotlinx.coroutines.flow.firstOrNull
import n.startapp.wordwaveriseapp.data.remote.ApiService
import n.startapp.wordwaveriseapp.data.remote.dto.ai.AiExerciseData
import n.startapp.wordwaveriseapp.data.remote.dto.ai.AiWordRequest
import n.startapp.wordwaveriseapp.util.NetworkError
import n.startapp.wordwaveriseapp.util.Resource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepository @Inject constructor(
    private val apiService: ApiService,
    private val authRepository: AuthRepository
) {
    companion object {
        private const val TAG = "AiRepository"
    }

    suspend fun explainWord(word: String): Resource<String> {
        return try {
            val token = authRepository.token.firstOrNull()
                ?: return Resource.Error("Не авторизован")
            val response = apiService.getAiExplanation("Bearer $token", AiWordRequest(word))
            if (response.status == "ok" && response.data != null) {
                Resource.Success(response.data.result)
            } else {
                Resource.Error(response.message ?: "Ошибка ИИ")
            }
        } catch (e: Exception) {
            Log.e(TAG, "explainWord failed: ${e.message}")
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }

    suspend fun getExamples(word: String): Resource<String> {
        return try {
            val token = authRepository.token.firstOrNull()
                ?: return Resource.Error("Не авторизован")
            val response = apiService.getAiExamples("Bearer $token", AiWordRequest(word))
            if (response.status == "ok" && response.data != null) {
                Resource.Success(response.data.result)
            } else {
                Resource.Error(response.message ?: "Ошибка ИИ")
            }
        } catch (e: Exception) {
            Log.e(TAG, "getExamples failed: ${e.message}")
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }

    suspend fun getExercise(word: String): Resource<AiExerciseData> {
        return try {
            val response = apiService.getAiExercise(AiWordRequest(word))
            if (response.status == "ok" && response.data != null) {
                Resource.Success(response.data)
            } else {
                Resource.Error(response.message ?: "Ошибка ИИ")
            }
        } catch (e: Exception) {
            Log.e(TAG, "getExercise failed: ${e.message}")
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }
}
