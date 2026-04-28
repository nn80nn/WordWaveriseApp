package n.startapp.wordwaveriseapp.data.remote

import n.startapp.wordwaveriseapp.data.remote.dto.DeleteResponse
import n.startapp.wordwaveriseapp.data.remote.dto.HealthResponse
import n.startapp.wordwaveriseapp.data.remote.dto.SuggestApiResponse
import n.startapp.wordwaveriseapp.data.remote.dto.WordDetailApiResponse
import n.startapp.wordwaveriseapp.data.remote.dto.WordResponse
import n.startapp.wordwaveriseapp.data.remote.dto.ai.AiExerciseApiResponse
import n.startapp.wordwaveriseapp.data.remote.dto.ai.AiTextApiResponse
import n.startapp.wordwaveriseapp.data.remote.dto.ai.AiWordRequest
import n.startapp.wordwaveriseapp.data.remote.dto.auth.AuthResponse
import n.startapp.wordwaveriseapp.data.remote.dto.auth.GoogleAuthRequest
import n.startapp.wordwaveriseapp.data.remote.dto.auth.LoginRequest
import n.startapp.wordwaveriseapp.data.remote.dto.auth.RegisterRequest
import n.startapp.wordwaveriseapp.data.remote.dto.flashcard.*
import n.startapp.wordwaveriseapp.data.remote.dto.category.CategoriesResponse
import n.startapp.wordwaveriseapp.data.remote.dto.category.CategoryResponse
import n.startapp.wordwaveriseapp.data.remote.dto.category.CreateCategoryRequest
import n.startapp.wordwaveriseapp.data.remote.dto.category.RenameCategoryRequest
import n.startapp.wordwaveriseapp.data.remote.dto.category.SetWordCategoryRequest
import n.startapp.wordwaveriseapp.data.remote.dto.category.SimpleStringResponse
import n.startapp.wordwaveriseapp.data.remote.dto.saved.SaveWordRequest
import n.startapp.wordwaveriseapp.data.remote.dto.saved.SaveWordResponse
import n.startapp.wordwaveriseapp.data.remote.dto.saved.SavedWordsResponse
import retrofit2.http.*

interface ApiService {
    @GET("api/health")
    suspend fun getHealth(): HealthResponse

    @GET("api/words/search")
    suspend fun searchWord(@Query("query") query: String): WordResponse

    @GET("api/words/details")
    suspend fun getWordDetails(
        @Query("query") query: String,
        @Query("quick") quick: Boolean = false
    ): WordDetailApiResponse

    @GET("api/words/suggest")
    suspend fun getSuggestions(
        @Query("query") query: String,
        @Query("prefix") prefix: Boolean = false
    ): SuggestApiResponse

    // Auth endpoints
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/auth/google")
    suspend fun loginWithGoogle(@Body request: GoogleAuthRequest): AuthResponse

    // Saved words endpoints (require auth token)
    @POST("api/words/saved")
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
    ): DeleteResponse

    // Flashcard endpoints (require auth token)
    @GET("api/flashcards")
    suspend fun getFlashcards(@Header("Authorization") token: String): FlashcardsListResponse

    @GET("api/flashcards/due")
    suspend fun getDueFlashcards(@Header("Authorization") token: String): DueFlashcardsResponse

    @GET("api/flashcards/statistics")
    suspend fun getFlashcardStatistics(@Header("Authorization") token: String): FlashcardStatisticsResponse

    @POST("api/flashcards")
    suspend fun createFlashcard(
        @Header("Authorization") token: String,
        @Body request: CreateFlashcardRequest
    ): FlashcardResponse

    @POST("api/flashcards/create")
    suspend fun createFlashcardFromSaved(
        @Header("Authorization") token: String,
        @Body request: CreateFlashcardFromSavedRequest
    ): FlashcardResponse

    @PUT("api/flashcards/{id}")
    suspend fun updateFlashcard(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: UpdateFlashcardRequest
    ): UpdateFlashcardResponse

    @DELETE("api/flashcards/{id}")
    suspend fun deleteFlashcard(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): DeleteResponse

    // Category endpoints (require auth token)
    @GET("api/categories")
    suspend fun getCategories(@Header("Authorization") token: String): CategoriesResponse

    @POST("api/categories")
    suspend fun createCategory(
        @Header("Authorization") token: String,
        @Body request: CreateCategoryRequest
    ): CategoryResponse

    @PUT("api/categories/{id}")
    suspend fun renameCategory(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: RenameCategoryRequest
    ): SimpleStringResponse

    @DELETE("api/categories/{id}")
    suspend fun deleteCategory(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): SimpleStringResponse

    @PUT("api/words/saved/{word}/category")
    suspend fun setWordCategory(
        @Header("Authorization") token: String,
        @Path("word") word: String,
        @Body request: SetWordCategoryRequest
    ): SimpleStringResponse

    // AI endpoints (require auth token)
    @POST("api/ai/explain")
    suspend fun getAiExplanation(
        @Header("Authorization") token: String,
        @Body request: AiWordRequest
    ): AiTextApiResponse

    @POST("api/ai/examples")
    suspend fun getAiExamples(
        @Header("Authorization") token: String,
        @Body request: AiWordRequest
    ): AiTextApiResponse

    @POST("api/ai/exercise")
    suspend fun getAiExercise(
        @Body request: AiWordRequest
    ): AiExerciseApiResponse

    @GET("api/ai/summary")
    suspend fun getAiSummary(
        @Query("word") word: String
    ): AiTextApiResponse
}
