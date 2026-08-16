package com.wordwaverise.wordwaveriseapp.data.remote

import com.wordwaverise.wordwaveriseapp.data.remote.dto.DeleteResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.HealthResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.SuggestApiResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.WordDetailApiResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.WordResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.ContextAnalysisApiResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.ContextAnalyzeRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.LookupApiResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.RuEnApiResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.TokenizeRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.TokenizedApiResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.ai.AiExerciseApiResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.ai.AiTextApiResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.ai.AiWordRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.auth.AuthResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.auth.GoogleAuthRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.auth.LoginRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.auth.RegisterRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.auth.RegisterResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.auth.RequestDeletionRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.auth.ResendVerificationRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.auth.SimpleMessageResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.auth.UserWrapperResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.auth.VerifyEmailRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.exercise.ExerciseBatchResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.exercise.ExerciseKindsResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.exercise.ExerciseRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.flashcard.*
import com.wordwaverise.wordwaveriseapp.data.remote.dto.category.CategoriesResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.category.ImportResultResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.category.ShareLinkResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.category.SharedFolderPreviewResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.category.CategoryResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.category.CreateCategoryRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.category.RenameCategoryRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.category.SetWordCategoryRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.category.SimpleStringResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.saved.SaveWordRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.saved.SaveWordResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.saved.SavedWordsResponse
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

    // ── v2: LLM-annotated article ───────────────────────────────────────────

    /**
     * Resolves the query (typos, inflections, phrases, Russian, sentences) and returns the
     * annotated article. May answer PENDING with the raw data while annotation finishes.
     */
    @GET("api/v2/words/lookup")
    suspend fun lookup(
        @Query("query") query: String,
        /**
         * Look the query up as typed — no correction, no lemmatisation, no fallback.
         * Null rather than false so the parameter is simply absent on a normal search.
         */
        @Query("exact") exact: Boolean? = null
    ): LookupApiResponse

    /** Explains one word as used in one sentence. */
    @POST("api/v2/context/analyze")
    suspend fun analyzeInContext(@Body request: ContextAnalyzeRequest): ContextAnalysisApiResponse

    @POST("api/v2/context/tokenize")
    suspend fun tokenize(@Body request: TokenizeRequest): TokenizedApiResponse

    /** Russian → English options, each with the context needed to choose it. */
    @GET("api/v2/translate/ru-en")
    suspend fun translateRuEn(@Query("query") query: String): RuEnApiResponse

    // Auth endpoints
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    @POST("api/auth/verify-email")
    suspend fun verifyEmail(@Body request: VerifyEmailRequest): AuthResponse

    @POST("api/auth/resend-verification")
    suspend fun resendVerification(@Body request: ResendVerificationRequest): SimpleMessageResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/auth/google")
    suspend fun loginWithGoogle(@Body request: GoogleAuthRequest): AuthResponse

    @POST("api/auth/request-deletion")
    suspend fun requestAccountDeletion(
        @Header("Authorization") token: String,
        @Body request: RequestDeletionRequest
    ): UserWrapperResponse

    @POST("api/auth/cancel-deletion")
    suspend fun cancelAccountDeletion(@Header("Authorization") token: String): UserWrapperResponse

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
    //
    // `categoryId` reads the same way everywhere: omitted = every folder, -1 = the cards in
    // no folder, anything else = that folder's server id.
    @GET("api/flashcards")
    suspend fun getFlashcards(
        @Header("Authorization") token: String,
        @Query("categoryId") categoryId: Int? = null
    ): FlashcardsListResponse

    @GET("api/flashcards/due")
    suspend fun getDueFlashcards(
        @Header("Authorization") token: String,
        @Query("categoryId") categoryId: Int? = null
    ): DueFlashcardsResponse

    @GET("api/flashcards/statistics")
    suspend fun getFlashcardStatistics(
        @Header("Authorization") token: String,
        @Query("categoryId") categoryId: Int? = null
    ): FlashcardStatisticsResponse

    /** One card for every saved word in a folder, in a single request. */
    @POST("api/flashcards/bulk")
    suspend fun bulkCreateFlashcards(
        @Header("Authorization") token: String,
        @Body request: BulkCreateFlashcardsRequest
    ): BulkCreateFlashcardsResponse

    /** Replaces what the card says. Marks it hand-edited, so the corpus stops rewriting it. */
    @PUT("api/flashcards/{id}/content")
    suspend fun updateFlashcardContent(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: UpdateFlashcardContentRequest
    ): FlashcardResponse

    // ── Общие папки ──────────────────────────────────────────────────────
    @POST("api/categories/{id}/share")
    suspend fun shareCategory(
        @Header("Authorization") token: String,
        @Path("id") categoryId: Int
    ): ShareLinkResponse

    @GET("api/categories/{id}/share")
    suspend fun categoryShareLink(
        @Header("Authorization") token: String,
        @Path("id") categoryId: Int
    ): ShareLinkResponse

    @DELETE("api/categories/{id}/share")
    suspend fun revokeCategoryShare(
        @Header("Authorization") token: String,
        @Path("id") categoryId: Int
    ): SimpleStringResponse

    /** Предпросмотр открыт без токена: ссылка и есть разрешение. */
    @GET("api/share/{token}")
    suspend fun sharedFolderPreview(@Path("token") shareToken: String): SharedFolderPreviewResponse

    @POST("api/share/{token}/import")
    suspend fun importSharedFolder(
        @Header("Authorization") token: String,
        @Path("token") shareToken: String
    ): ImportResultResponse

    @PUT("api/flashcards/{id}/category")
    suspend fun setFlashcardCategory(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: SetFlashcardCategoryRequest
    ): FlashcardResponse

    // Exercises — the server owns the questions, so both clients show the same practice.
    @GET("api/exercises/kinds")
    suspend fun getExerciseKinds(
        @Header("Authorization") token: String,
        @Query("categoryId") categoryId: Int? = null,
        @Query("scope") scope: String = "SAVED"
    ): ExerciseKindsResponse

    @POST("api/exercises/generate")
    suspend fun generateExercises(
        @Header("Authorization") token: String,
        @Body request: ExerciseRequest
    ): ExerciseBatchResponse

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
