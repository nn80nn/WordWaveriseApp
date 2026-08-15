package com.wordwaverise.wordwaveriseapp.data.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import com.wordwaverise.wordwaveriseapp.data.local.dao.FlashcardDao
import com.wordwaverise.wordwaveriseapp.data.local.entity.FlashcardEntity
import com.wordwaverise.wordwaveriseapp.data.remote.ApiService
import com.wordwaverise.wordwaveriseapp.data.remote.dto.flashcard.BulkCreateFlashcardsData
import com.wordwaverise.wordwaveriseapp.data.remote.dto.flashcard.BulkCreateFlashcardsRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.flashcard.CreateFlashcardRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.flashcard.FlashcardDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.flashcard.SetFlashcardCategoryRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.flashcard.UpdateFlashcardContentRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.flashcard.UpdateFlashcardRequest
import com.wordwaverise.wordwaveriseapp.util.ExerciseVerdict
import com.wordwaverise.wordwaveriseapp.util.NetworkError
import com.wordwaverise.wordwaveriseapp.util.Resource
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

@Singleton
class FlashcardRepository @Inject constructor(
    private val flashcardDao: FlashcardDao,
    private val apiService: ApiService,
    private val authRepository: AuthRepository
) {
    companion object {
        private const val TAG = "FlashcardRepository"
    }

    val allFlashcards: Flow<List<FlashcardEntity>> = flashcardDao.getAllFlashcards()
    val dueCount: Flow<Int> = flashcardDao.getDueCount()
    val totalCount: Flow<Int> = flashcardDao.getTotalCount()

    fun getFlashcardsForSession(limit: Int = 10): Flow<List<FlashcardEntity>> =
        flashcardDao.getFlashcardsForSession(limit)

    // -- Folders --------------------------------------------------------------
    //
    // `folderId` reads the same way as in the API: null = every folder, -1 = the cards in no
    // folder, anything else = that folder's server id.

    fun getFlashcardsForSession(folderId: Int?, limit: Int = 10): Flow<List<FlashcardEntity>> =
        flashcardDao.getFlashcardsForSessionInFolder(folderId, limit)

    fun dueCountIn(folderId: Int?): Flow<Int> = flashcardDao.getDueCountInFolder(folderId)

    fun totalCountIn(folderId: Int?): Flow<Int> = flashcardDao.getTotalCountInFolder(folderId)

    /**
     * Replaces what a card says, and marks it as hand-edited so the dictionary stops rewriting
     * it. The server is asked first: it owns the `customized` flag, and a local-only edit would
     * be undone by the corpus refresh on the very next study session.
     */
    suspend fun updateContent(
        card: FlashcardEntity,
        word: String,
        translation: String?,
        definition: String,
        example: String?
    ): Resource<Unit> {
        return try {
            val token = authRepository.token.firstOrNull()
            val serverId = card.serverId

            if (serverId != null && !token.isNullOrEmpty()) {
                val response = apiService.updateFlashcardContent(
                    "Bearer $token",
                    serverId,
                    UpdateFlashcardContentRequest(
                        word = word,
                        translation = translation.orEmpty(),
                        definition = definition.takeIf { it.isNotBlank() },
                        example = example?.takeIf { it.isNotBlank() }
                    )
                )
                if (response.status != "ok") {
                    return Resource.Error(response.message ?: "Не удалось сохранить карточку")
                }
            }

            flashcardDao.updateFlashcard(
                card.copy(
                    word = word,
                    translation = translation,
                    definition = definition,
                    example = example,
                    customized = true,
                    updatedAt = System.currentTimeMillis()
                )
            )
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error editing flashcard", e)
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }

    suspend fun setCategory(card: FlashcardEntity, folderId: Int?): Resource<Unit> {
        return try {
            val token = authRepository.token.firstOrNull()
            val serverId = card.serverId
            if (serverId != null && !token.isNullOrEmpty()) {
                apiService.setFlashcardCategory(
                    "Bearer $token", serverId, SetFlashcardCategoryRequest(folderId)
                )
            }
            flashcardDao.updateFlashcard(card.copy(serverCategoryId = folderId))
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to move card to folder: ${e.message}")
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }

    /**
     * Creates a card for every saved word in a folder, in one request.
     *
     * Сервер отдаёт три числа, а не два: слово могло уже иметь карточку вне папок, и такая
     * карточка переезжает в папку, а не создаётся заново.
     */
    suspend fun createFromFolder(folderId: Int?): Resource<BulkCreateFlashcardsData> {
        return try {
            val token = authRepository.token.firstOrNull()
                ?: return Resource.Error("Не авторизован")
            val response = apiService.bulkCreateFlashcards(
                "Bearer $token", BulkCreateFlashcardsRequest(folderId)
            )
            if (response.status == "ok" && response.data != null) {
                syncFromServer()
                Resource.Success(response.data)
            } else {
                Resource.Error(response.message ?: "Не удалось создать карточки")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Bulk create failed", e)
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }

    suspend fun createFlashcard(
        word: String,
        definition: String,
        example: String? = null,
        translation: String? = null,
        phonetic: String? = null,
        partOfSpeech: String? = null
    ): Resource<Long> {
        return try {
            val existing = flashcardDao.getFlashcardByWord(word)
            if (existing != null) {
                Log.d(TAG, "Flashcard already exists for word: $word")
                return Resource.Success(existing.id)
            }

            val flashcard = FlashcardEntity(
                word = word,
                definition = definition,
                example = example,
                translation = translation,
                phonetic = phonetic,
                partOfSpeech = partOfSpeech
            )
            val localId = flashcardDao.insertFlashcard(flashcard)
            Log.d(TAG, "Created local flashcard: $localId")

            syncToServer(flashcard.copy(id = localId))

            Resource.Success(localId)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating flashcard", e)
            Resource.Error("Ошибка создания карточки: ${e.message}")
        }
    }

    private suspend fun syncToServer(flashcard: FlashcardEntity) {
        try {
            val token = authRepository.token.firstOrNull()
            if (token.isNullOrEmpty()) return

            val request = CreateFlashcardRequest(
                word = flashcard.word,
                translation = flashcard.translation,
                definition = flashcard.definition,
                example = flashcard.example
            )

            val response = apiService.createFlashcard("Bearer $token", request)
            if (response.status == "ok" && response.data != null) {
                val serverId = response.data.id
                Log.d(TAG, "Synced flashcard to server: serverId=$serverId")
                val updated = flashcardDao.getFlashcardByWord(flashcard.word)
                if (updated != null && updated.serverId == null) {
                    flashcardDao.updateFlashcard(updated.copy(serverId = serverId))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync to server: ${e.message}")
        }
    }

    suspend fun markAsCorrect(flashcard: FlashcardEntity): Resource<Unit> {
        return try {
            val newLevel = min(flashcard.repetitionLevel + 1, 5)
            val nextReview = calculateNextReview(newLevel)

            val updated = flashcard.copy(
                repetitionLevel = newLevel,
                lastReviewed = System.currentTimeMillis(),
                nextReviewDate = nextReview,
                correctCount = flashcard.correctCount + 1,
                updatedAt = System.currentTimeMillis()
            )

            flashcardDao.updateFlashcard(updated)
            Log.d(TAG, "Marked as correct: ${flashcard.word}, new level: $newLevel")

            syncUpdateToServer(updated, "GOOD")

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking as correct", e)
            Resource.Error("Ошибка обновления карточки")
        }
    }

    suspend fun markAsIncorrect(flashcard: FlashcardEntity): Resource<Unit> {
        return try {
            val newLevel = max(flashcard.repetitionLevel - 1, 0)
            val nextReview = calculateNextReview(newLevel)

            val updated = flashcard.copy(
                repetitionLevel = newLevel,
                lastReviewed = System.currentTimeMillis(),
                nextReviewDate = nextReview,
                incorrectCount = flashcard.incorrectCount + 1,
                updatedAt = System.currentTimeMillis()
            )

            flashcardDao.updateFlashcard(updated)
            Log.d(TAG, "Marked as incorrect: ${flashcard.word}, new level: $newLevel")

            syncUpdateToServer(updated, "AGAIN")

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking as incorrect", e)
            Resource.Error("Ошибка обновления карточки")
        }
    }

    /**
     * A near miss: the word is known, the spelling was not. The card keeps its level rather
     * than advancing, so it comes back soon instead of being treated as learned — or as
     * forgotten, which resetting it would imply.
     */
    suspend fun markAsAlmost(flashcard: FlashcardEntity): Resource<Unit> {
        return try {
            val updated = flashcard.copy(
                lastReviewed = System.currentTimeMillis(),
                nextReviewDate = calculateNextReview(flashcard.repetitionLevel),
                correctCount = flashcard.correctCount + 1,
                updatedAt = System.currentTimeMillis()
            )
            flashcardDao.updateFlashcard(updated)
            syncUpdateToServer(updated, "HARD")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking as almost", e)
            Resource.Error("Ошибка обновления карточки")
        }
    }

    /**
     * Lets an exercise answer move the schedule of the card behind it. Practice that counts
     * towards nothing is practice people stop doing, and a word repeatedly missed in exercises
     * is exactly the word that should come back sooner.
     */
    suspend fun recordPracticeResult(serverCardId: Int, verdict: ExerciseVerdict) {
        val card = flashcardDao.getFlashcardByServerId(serverCardId) ?: return
        when (verdict) {
            ExerciseVerdict.CORRECT -> markAsCorrect(card)
            ExerciseVerdict.ALMOST -> markAsAlmost(card)
            ExerciseVerdict.WRONG -> markAsIncorrect(card)
        }
    }

    private suspend fun syncUpdateToServer(flashcard: FlashcardEntity, difficulty: String) {
        try {
            val token = authRepository.token.firstOrNull()
            if (token.isNullOrEmpty()) return

            val serverId = flashcard.serverId
            if (serverId == null) {
                Log.d(TAG, "No serverId for '${flashcard.word}', skipping server update")
                return
            }

            val response = apiService.updateFlashcard(
                "Bearer $token",
                serverId,
                UpdateFlashcardRequest(difficulty)
            )
            if (response.status == "ok") {
                Log.d(TAG, "Updated flashcard on server: serverId=$serverId, difficulty=$difficulty")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync update to server: ${e.message}")
        }
    }

    private fun calculateNextReview(level: Int): Long {
        val delays = listOf(
            1 * 60 * 1000L,
            10 * 60 * 1000L,
            24 * 60 * 60 * 1000L,
            3 * 24 * 60 * 60 * 1000L,
            7 * 24 * 60 * 60 * 1000L,
            30 * 24 * 60 * 60 * 1000L
        )
        return System.currentTimeMillis() + delays[level]
    }

    suspend fun deleteFlashcard(word: String): Resource<Unit> {
        return try {
            val entity = flashcardDao.getFlashcardByWord(word)
            val token = authRepository.token.firstOrNull()
            if (entity != null && entity.serverId != null && !token.isNullOrEmpty()) {
                try {
                    apiService.deleteFlashcard("Bearer $token", entity.serverId)
                    Log.d(TAG, "Deleted flashcard from server: serverId=${entity.serverId}")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete flashcard from server: ${e.message}")
                }
            }
            flashcardDao.deleteByWord(word)
            Log.d(TAG, "Deleted flashcard locally: $word")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting flashcard", e)
            Resource.Error("Ошибка удаления карточки")
        }
    }

    suspend fun syncFromServer(): Resource<Unit> {
        return try {
            val token = authRepository.token.firstOrNull()
            if (token.isNullOrEmpty()) {
                return Resource.Error("Не авторизован")
            }

            val response = apiService.getFlashcards("Bearer $token")
            if (response.status == "ok" && response.data != null) {
                response.data.forEach { dto ->
                    val existing = flashcardDao.getFlashcardByWord(dto.word)
                    if (existing == null) {
                        flashcardDao.insertFlashcard(dtoToEntity(dto))
                    } else {
                        // The folder and the hand-edited flag are the server's to state, and a
                        // card corrected in the browser should read the same here -- but a card
                        // edited on this device keeps its own wording either way.
                        val adoptWording = !existing.customized && !dto.customized
                        flashcardDao.updateFlashcard(
                            existing.copy(
                                serverId = existing.serverId ?: dto.id,
                                serverCategoryId = dto.categoryId,
                                customized = existing.customized || dto.customized,
                                translation = if (adoptWording) dto.translation else existing.translation,
                                definition = if (adoptWording) dto.definition ?: existing.definition
                                             else existing.definition,
                                example = if (adoptWording) dto.example ?: existing.example
                                          else existing.example
                            )
                        )
                    }
                }
                Log.d(TAG, "Synced ${response.data.size} flashcards from server")
                Resource.Success(Unit)
            } else {
                Resource.Error(response.message ?: "Ошибка синхронизации")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing from server", e)
            Resource.Error("Ошибка синхронизации: ${e.message}")
        }
    }

    private fun dtoToEntity(dto: FlashcardDto): FlashcardEntity {
        val nextReviewTimestamp = runCatching {
            java.time.Instant.parse(dto.nextReview).toEpochMilli()
        }.getOrDefault(System.currentTimeMillis() + dto.daysUntilReview * 24 * 60 * 60 * 1000L)
        return FlashcardEntity(
            serverId = dto.id,
            word = dto.word,
            definition = dto.definition ?: "",
            example = dto.example,
            translation = dto.translation,
            serverCategoryId = dto.categoryId,
            customized = dto.customized,
            nextReviewDate = nextReviewTimestamp
        )
    }
}
