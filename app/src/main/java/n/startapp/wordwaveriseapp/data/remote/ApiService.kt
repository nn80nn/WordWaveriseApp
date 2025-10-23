package n.startapp.wordwaveriseapp.data.remote

import n.startapp.wordwaveriseapp.data.remote.dto.HealthResponse
import n.startapp.wordwaveriseapp.data.remote.dto.WordResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("api/health")
    suspend fun getHealth(): HealthResponse

    @GET("api/words/search")
    suspend fun searchWord(@Query("query") query: String): WordResponse
}
