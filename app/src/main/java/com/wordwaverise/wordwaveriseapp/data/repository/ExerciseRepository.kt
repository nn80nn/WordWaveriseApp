package com.wordwaverise.wordwaveriseapp.data.repository

import android.util.Log
import kotlinx.coroutines.flow.firstOrNull
import com.wordwaverise.wordwaveriseapp.data.remote.ApiService
import com.wordwaverise.wordwaveriseapp.data.remote.dto.exercise.ExerciseBatchDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.exercise.ExerciseKindsDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.exercise.ExerciseRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.exercise.ExerciseScope
import com.wordwaverise.wordwaveriseapp.util.NetworkError
import com.wordwaverise.wordwaveriseapp.util.Resource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Practice sessions, assembled by the server.
 *
 * Deliberately thin: it fetches and unwraps, and nothing more. Every judgement about what a
 * question is — which words, which kinds, what counts as an answer — belongs to the backend,
 * because that is the only way the phone and the browser can show the same practice for the
 * same folder. Logic added here is logic the web does not have.
 */
@Singleton
class ExerciseRepository @Inject constructor(
    private val apiService: ApiService,
    private val authRepository: AuthRepository
) {
    companion object {
        private const val TAG = "ExerciseRepository"
    }

    /** What the chosen folder can currently produce, and how many of each. */
    suspend fun kinds(folderId: Int?, scope: ExerciseScope): Resource<ExerciseKindsDto> {
        return try {
            val token = authRepository.token.firstOrNull()
                ?: return Resource.Error("Не авторизован")
            val response = apiService.getExerciseKinds("Bearer $token", folderId, scope.name)
            if (response.status == "ok" && response.data != null) {
                Resource.Success(response.data)
            } else {
                Resource.Error(response.message ?: "Не удалось загрузить типы заданий")
            }
        } catch (e: Exception) {
            Log.w(TAG, "kinds failed: ${e.message}")
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }

    /** A whole session in one round trip — questions, answers and explanations. */
    suspend fun generate(request: ExerciseRequest): Resource<ExerciseBatchDto> {
        return try {
            val token = authRepository.token.firstOrNull()
                ?: return Resource.Error("Не авторизован")
            val response = apiService.generateExercises("Bearer $token", request)
            if (response.status == "ok" && response.data != null) {
                Resource.Success(response.data)
            } else {
                Resource.Error(response.message ?: "Не удалось собрать упражнения")
            }
        } catch (e: Exception) {
            Log.e(TAG, "generate failed: ${e.message}")
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }
}
