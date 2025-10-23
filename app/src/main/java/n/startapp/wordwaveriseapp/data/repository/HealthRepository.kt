package n.startapp.wordwaveriseapp.data.repository

import n.startapp.wordwaveriseapp.data.remote.ApiService
import n.startapp.wordwaveriseapp.data.remote.dto.HealthResponse
import n.startapp.wordwaveriseapp.util.NetworkError
import n.startapp.wordwaveriseapp.util.Resource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun checkHealth(): Resource<HealthResponse> {
        return try {
            val response = apiService.getHealth()
            Resource.Success(response)
        } catch (e: Exception) {
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }
}
