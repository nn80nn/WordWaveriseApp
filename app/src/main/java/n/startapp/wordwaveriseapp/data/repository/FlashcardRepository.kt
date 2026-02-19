package n.startapp.wordwaveriseapp.data.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import n.startapp.wordwaveriseapp.data.local.dao.FlashcardDao
import n.startapp.wordwaveriseapp.data.local.entity.FlashcardEntity
import n.startapp.wordwaveriseapp.data.remote.ApiService
import n.startapp.wordwaveriseapp.data.remote.dto.flashcard.CreateFlashcardRequest
import n.startapp.wordwaveriseapp.data.remote.dto.flashcard.FlashcardDto
import n.startapp.wordwaveriseapp.data.remote.dto.flashcard.UpdateFlashcardRequest
import n.startapp.wordwaveriseapp.util.Resource
import java.text.SimpleDateFormat
import java.util.*
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

    suspend fun createFlashcard(
        word: String,
        definition: String,
        example: String? = null,
        translation: String? = null,
        phonetic: String? = null,
        partOfSpeech: String? = null
    ): Resource<Long> {
        return try {
            // Проверяем, нет ли уже карточки для этого слова
            val existing = flashcardDao.getFlashcardByWord(word)
            if (existing != null) {
                Log.d(TAG, "Flashcard already exists for word: $word")
                return Resource.Success(existing.id)
            }

            // Создаём локально
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

            // Синхронизируем с сервером
            syncToServer(flashcard)

            Resource.Success(localId)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating flashcard", e)
            Resource.Error("Ошибка создания карточки: ${e.message}")
        }
    }

    private suspend fun syncToServer(flashcard: FlashcardEntity) {
        try {
            val token = authRepository.token.firstOrNull()
            if (token.isNullOrEmpty()) {
                Log.d(TAG, "No token, skipping server sync")
                return
            }

            val request = CreateFlashcardRequest(
                word = flashcard.word,
                definition = flashcard.definition,
                example = flashcard.example,
                translation = flashcard.translation,
                phonetic = flashcard.phonetic,
                partOfSpeech = flashcard.partOfSpeech
            )

            val response = apiService.createFlashcard("Bearer $token", request)
            if (response.status == "ok" && response.data != null) {
                Log.d(TAG, "Synced flashcard to server: ${response.data.id}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync to server: ${e.message}")
            // Не критичная ошибка, карточка сохранена локально
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

            // Синхронизация с сервером
            syncUpdateToServer(updated)

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

            // Синхронизация с сервером
            syncUpdateToServer(updated)

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking as incorrect", e)
            Resource.Error("Ошибка обновления карточки")
        }
    }

    private suspend fun syncUpdateToServer(flashcard: FlashcardEntity) {
        try {
            val token = authRepository.token.firstOrNull()
            if (token.isNullOrEmpty()) return

            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            val request = UpdateFlashcardRequest(
                repetitionLevel = flashcard.repetitionLevel,
                lastReviewed = dateFormat.format(Date(flashcard.lastReviewed ?: 0)),
                nextReviewDate = dateFormat.format(Date(flashcard.nextReviewDate)),
                correctCount = flashcard.correctCount,
                incorrectCount = flashcard.incorrectCount
            )

            // Note: нужен serverId для обновления на сервере
            // Для полной реализации нужно добавить serverId в FlashcardEntity
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync update to server: ${e.message}")
        }
    }

    private fun calculateNextReview(level: Int): Long {
        val delays = listOf(
            1 * 60 * 1000L,           // Level 0: 1 минута
            10 * 60 * 1000L,          // Level 1: 10 минут
            24 * 60 * 60 * 1000L,     // Level 2: 1 день
            3 * 24 * 60 * 60 * 1000L, // Level 3: 3 дня
            7 * 24 * 60 * 60 * 1000L, // Level 4: 7 дней
            30 * 24 * 60 * 60 * 1000L // Level 5: 30 дней
        )
        return System.currentTimeMillis() + delays[level]
    }

    suspend fun deleteFlashcard(word: String): Resource<Unit> {
        return try {
            flashcardDao.deleteByWord(word)
            Log.d(TAG, "Deleted flashcard: $word")
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
                // Сохраняем карточки с сервера локально
                response.data.flashcards.forEach { dto ->
                    val entity = dtoToEntity(dto)
                    flashcardDao.insertFlashcard(entity)
                }
                Log.d(TAG, "Synced ${response.data.flashcards.size} flashcards from server")
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
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        return FlashcardEntity(
            id = dto.id.toLong(),
            word = dto.word,
            definition = dto.definition,
            example = dto.example,
            translation = dto.translation,
            phonetic = dto.phonetic,
            partOfSpeech = dto.partOfSpeech,
            repetitionLevel = dto.repetitionLevel,
            lastReviewed = dto.lastReviewed?.let { dateFormat.parse(it)?.time },
            nextReviewDate = dateFormat.parse(dto.nextReviewDate)?.time ?: System.currentTimeMillis(),
            correctCount = dto.correctCount,
            incorrectCount = dto.incorrectCount,
            createdAt = dateFormat.parse(dto.createdAt)?.time ?: System.currentTimeMillis(),
            updatedAt = dateFormat.parse(dto.updatedAt)?.time ?: System.currentTimeMillis()
        )
    }
}
