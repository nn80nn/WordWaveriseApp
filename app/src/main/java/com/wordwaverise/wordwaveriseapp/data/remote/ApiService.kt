package com.wordwaverise.wordwaveriseapp.data.remote

import com.wordwaverise.wordwaveriseapp.data.remote.dto.DeleteResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.HealthResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.SuggestApiResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.WordDetailApiResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.WordResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.ContextAnalysisApiResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.ContextAnalyzeRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.ContextHintApiResponse
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
import com.wordwaverise.wordwaveriseapp.data.remote.dto.report.ContentReportRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.report.ContentReportResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.category.SharedFolderPreviewResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.category.CategoryResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.category.CreateCategoryRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.category.RenameCategoryRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.category.SetParentRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.category.SetWordCategoryRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.category.SimpleStringResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.group.AssignmentsResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.group.GroupResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.group.JoinByCodeRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.group.MyGroupsResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.group.ReportAttemptsRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.group.ReportAttemptsResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.saved.SaveWordRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.saved.SaveWordResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.saved.SavedWordsResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.saved.SetWordFoldersRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.BlockPageResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.BookDetailResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.BooksResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.BookImportResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.ImportBookTextRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.ReadingPositionResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.SetPositionRequest
import okhttp3.MultipartBody
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

    /**
     * Быстрая подсказка: лемма, часть речи, перевод в этом предложении.
     *
     * То, что зовёт тап по слову в книге. Полный разбор остаётся отдельным шагом: он пишется
     * секундами, а читающий не должен их ждать, чтобы узнать одно слово.
     */
    @POST("api/v2/context/hint")
    suspend fun contextHint(@Body request: ContextAnalyzeRequest): ContextHintApiResponse

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

    @GET("api/auth/me")
    suspend fun getCurrentUser(@Header("Authorization") token: String): UserWrapperResponse

    @POST("api/auth/request-deletion")
    suspend fun requestAccountDeletion(
        @Header("Authorization") token: String,
        @Body request: RequestDeletionRequest
    ): UserWrapperResponse

    @POST("api/auth/cancel-deletion")
    suspend fun cancelAccountDeletion(@Header("Authorization") token: String): UserWrapperResponse

    /**
     * Жалоба на сгенерированный текст.
     *
     * Google Play требует, чтобы приложение со сгенерированным контентом давало пожаловаться
     * на него изнутри: жалоба, которую можно оставить только на почте поддержки, — это жалоба,
     * которую почти никто не оставит.
     */
    @POST("api/reports")
    suspend fun reportContent(
        @Header("Authorization") token: String,
        @Body request: ContentReportRequest
    ): ContentReportResponse

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

    /** Убирает одно значение, оставляя остальные значения того же слова. */
    @DELETE("api/words/saved/id/{id}")
    suspend fun deleteSavedEntry(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): DeleteResponse

    /** Заменяет весь набор папок одной записи. */
    @PUT("api/words/saved/id/{id}/folders")
    suspend fun setWordFolders(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: SetWordFoldersRequest
    ): SaveWordResponse

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

    // ── Группы ───────────────────────────────────────────────────────────
    // Администрирование целиком в вебе: здесь только то, что нужно ученику — увидеть свои
    // классы, вступить, выйти, посмотреть задания и отчитаться о сессии.

    @GET("api/groups")
    suspend fun getMyGroups(@Header("Authorization") token: String): MyGroupsResponse

    @POST("api/groups/join")
    suspend fun joinGroupByCode(
        @Header("Authorization") token: String,
        @Body request: JoinByCodeRequest
    ): GroupResponse

    @POST("api/g/{token}/join")
    suspend fun joinGroupByInvite(
        @Header("Authorization") token: String,
        @Path("token") inviteToken: String
    ): GroupResponse

    @DELETE("api/groups/{id}/membership")
    suspend fun leaveGroup(
        @Header("Authorization") token: String,
        @Path("id") groupId: Int
    ): SimpleStringResponse

    @GET("api/assignments")
    suspend fun getAssignments(
        @Header("Authorization") token: String,
        @Query("groupId") groupId: Int? = null
    ): AssignmentsResponse

    /** Одна отправка на законченную сессию, а не на ответ. */
    @POST("api/practice/attempts")
    suspend fun reportAttempts(
        @Header("Authorization") token: String,
        @Body request: ReportAttemptsRequest
    ): ReportAttemptsResponse

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

    /** Вложить папку в папку-группу или вынуть обратно. */
    @PUT("api/categories/{id}/parent")
    suspend fun setCategoryParent(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: SetParentRequest
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

    // ── Читалка (все требуют токен) ───────────────────────────────────

    @GET("api/v2/library/books")
    suspend fun getBooks(@Header("Authorization") token: String): BooksResponse

    /**
     * ⚠️ Имя части значения не имеет — сервер берёт первую файловую и останавливается.
     * Заодно это значит, что название рядом с файлом передать нельзя: обычные поля он выбросит.
     */
    @Multipart
    @POST("api/v2/library/books")
    suspend fun uploadBook(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part
    ): BookImportResponse

    /**
     * ⚠️ Название входит в хэш содержимого, поэтому тот же текст под другим названием — это
     * другая книга, а не повторный импорт прежней.
     */
    @POST("api/v2/library/books/text")
    suspend fun importBookText(
        @Header("Authorization") token: String,
        @Body request: ImportBookTextRequest
    ): BookImportResponse

    @GET("api/v2/library/books/{id}")
    suspend fun getBook(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): BookDetailResponse

    /** Окно блоков. Просить дальше по `nextOrdinal`, пока он не придёт пустым. */
    @GET("api/v2/library/books/{id}/blocks")
    suspend fun getBookBlocks(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Query("from") from: Int,
        @Query("limit") limit: Int = 40
    ): BlockPageResponse

    @PUT("api/v2/library/books/{id}/position")
    suspend fun setReadingPosition(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: SetPositionRequest
    ): ReadingPositionResponse

    @DELETE("api/v2/library/books/{id}")
    suspend fun deleteBook(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): DeleteResponse

    /** Папка книги — заводится при первом сохранении из неё. Идемпотентно. */
    @POST("api/v2/library/books/{id}/folder")
    suspend fun ensureBookFolder(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): CategoryResponse

    @GET("api/ai/summary")
    suspend fun getAiSummary(
        @Query("word") word: String
    ): AiTextApiResponse
}
