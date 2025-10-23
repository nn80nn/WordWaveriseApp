package n.startapp.wordwaveriseapp.data.remote

import n.startapp.wordwaveriseapp.data.remote.dto.HealthResponse
import retrofit2.http.GET

interface ApiService {
    @GET("api/health")
    suspend fun getHealth(): HealthResponse
}
