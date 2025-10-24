package n.startapp.wordwaveriseapp.data.remote

import n.startapp.wordwaveriseapp.data.remote.dto.HealthResponse
import n.startapp.wordwaveriseapp.data.remote.dto.WordResponse
import n.startapp.wordwaveriseapp.data.remote.dto.auth.AuthResponse
import n.startapp.wordwaveriseapp.data.remote.dto.auth.LoginRequest
import n.startapp.wordwaveriseapp.data.remote.dto.auth.RegisterRequest
import n.startapp.wordwaveriseapp.data.remote.dto.saved.SaveWordRequest
import n.startapp.wordwaveriseapp.data.remote.dto.saved.SaveWordResponse
import n.startapp.wordwaveriseapp.data.remote.dto.saved.SavedWordsResponse
import retrofit2.http.*

interface ApiService {
    @GET("api/health")
    suspend fun getHealth(): HealthResponse

    @GET("api/words/search")
    suspend fun searchWord(@Query("query") query: String): WordResponse

    // Auth endpoints
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    // Saved words endpoints (require auth token)
    @POST("api/words/save")
    suspend fun saveWord(
        @Header("Authorization") token: String,
        @Body request: SaveWordRequest
    ): SaveWordResponse

    @GET("api/words/saved")
    suspend fun getSavedWords(@Header("Authorization") token: String): SavedWordsResponse

    @DELETE("api/words/saved/{word}")
    suspend fun deleteSavedWord(
        @Header("Authorization") token: String,
        @Path("word") word: String
    ): SaveWordResponse
}
