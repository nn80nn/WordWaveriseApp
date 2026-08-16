package com.wordwaverise.wordwaveriseapp.data.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import com.wordwaverise.wordwaveriseapp.data.local.TokenDataStore
import com.wordwaverise.wordwaveriseapp.data.local.dao.CategoryDao
import com.wordwaverise.wordwaveriseapp.data.local.entity.CategoryEntity
import com.wordwaverise.wordwaveriseapp.data.remote.ApiService
import com.wordwaverise.wordwaveriseapp.data.remote.dto.category.CreateCategoryRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.category.ImportResultDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.category.RenameCategoryRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.category.SetWordCategoryRequest
import com.wordwaverise.wordwaveriseapp.util.NetworkError
import com.wordwaverise.wordwaveriseapp.util.Resource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val apiService: ApiService,
    private val categoryDao: CategoryDao,
    private val tokenDataStore: TokenDataStore
) {
    companion object {
        private const val TAG = "CategoryRepository"
    }

    val categories: Flow<List<CategoryEntity>> = categoryDao.getAll()

    suspend fun syncCategories(): Resource<List<CategoryEntity>> {
        return try {
            val token = tokenDataStore.token.firstOrNull() ?: return Resource.Success(emptyList())
            val response = apiService.getCategories("Bearer $token")
            if (response.status == "ok" && response.data != null) {
                response.data.forEach { dto ->
                    // Match an existing row first: a blind insert leaves id = 0, so Room
                    // autogenerates a new primary key and every sync duplicated the chip.
                    val existing = categoryDao.getByServerId(dto.id)
                        ?: categoryDao.getUnlinkedByName(dto.name)

                    if (existing == null) {
                        categoryDao.insert(
                            CategoryEntity(
                                serverId = dto.id,
                                name = dto.name,
                                color = dto.color
                            )
                        )
                    } else {
                        categoryDao.linkToServer(existing.id, dto.id, dto.name, dto.color)
                    }
                }

                // Clean up copies produced by earlier versions of this sync.
                categoryDao.remapWordsToOldestDuplicate()
                categoryDao.deleteDuplicateServerCategories()
            }
            Resource.Success(categoryDao.getAll().firstOrNull() ?: emptyList())
        } catch (e: Exception) {
            Log.w(TAG, "syncCategories failed: ${e.message}")
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }

    suspend fun createCategory(name: String, color: String? = null): Resource<CategoryEntity> {
        return try {
            val token = tokenDataStore.token.firstOrNull()
            var serverId: Int? = null

            if (!token.isNullOrEmpty()) {
                try {
                    val response = apiService.createCategory(
                        "Bearer $token",
                        CreateCategoryRequest(name, color)
                    )
                    if (response.status == "ok") serverId = response.data?.id
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to create category on server: ${e.message}")
                }
            }

            val entity = CategoryEntity(serverId = serverId, name = name, color = color)
            val id = categoryDao.insert(entity)
            Resource.Success(entity.copy(id = id))
        } catch (e: Exception) {
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }

    suspend fun renameCategory(id: Long, serverId: Int?, name: String): Resource<Unit> {
        return try {
            categoryDao.rename(id, name)
            val token = tokenDataStore.token.firstOrNull()
            if (!token.isNullOrEmpty() && serverId != null) {
                try {
                    apiService.renameCategory("Bearer $token", serverId, RenameCategoryRequest(name))
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to rename category on server: ${e.message}")
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }

    // ── Общие папки ──────────────────────────────────────────────────────

    /**
     * Ссылка на папку. Папка, которой ещё не делились, ссылки не имеет — сервер её выдаёт
     * по первому запросу и потом возвращает ту же самую.
     */
    suspend fun shareCategory(serverId: Int?): Resource<String> {
        if (serverId == null) return Resource.Error("Папка ещё не синхронизирована")
        return try {
            val token = tokenDataStore.token.firstOrNull()
                ?: return Resource.Error("Не авторизован")
            val response = apiService.shareCategory("Bearer $token", serverId)
            val url = response.data?.url
            if (response.status == "ok" && !url.isNullOrBlank()) Resource.Success(url)
            else Resource.Error(response.message ?: "Не удалось создать ссылку")
        } catch (e: Exception) {
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }

    suspend fun revokeShare(serverId: Int?): Resource<Unit> {
        if (serverId == null) return Resource.Error("Папка ещё не синхронизирована")
        return try {
            val token = tokenDataStore.token.firstOrNull()
                ?: return Resource.Error("Не авторизован")
            apiService.revokeCategoryShare("Bearer $token", serverId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }

    /**
     * Забирает себе папку по ссылке.
     *
     * Принимается и полная ссылка, и один токен: человек копирует то, что ему прислали, и
     * заставлять его вырезать хвост вручную — верный способ получить «не работает».
     */
    suspend fun importSharedFolder(linkOrToken: String): Resource<ImportResultDto> {
        val shareToken = linkOrToken.trim().trimEnd('/').substringAfterLast('/')
        if (shareToken.isBlank()) return Resource.Error("Пустая ссылка")
        return try {
            val token = tokenDataStore.token.firstOrNull()
                ?: return Resource.Error("Не авторизован")
            val response = apiService.importSharedFolder("Bearer $token", shareToken)
            val result = response.data
            if (response.status == "ok" && result != null) {
                // Папка появилась на сервере — локальный список должен узнать о ней сразу,
                // иначе слова приедут в папку, которой на этом экране ещё нет.
                syncCategories()
                Resource.Success(result)
            } else {
                Resource.Error(response.message ?: "Не удалось добавить папку")
            }
        } catch (e: Exception) {
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }

    suspend fun deleteCategory(id: Long, serverId: Int?): Resource<Unit> {
        return try {
            categoryDao.unassignWords(id)
            categoryDao.deleteById(id)
            val token = tokenDataStore.token.firstOrNull()
            if (!token.isNullOrEmpty() && serverId != null) {
                try {
                    apiService.deleteCategory("Bearer $token", serverId)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete category on server: ${e.message}")
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }

    suspend fun setWordCategory(word: String, categoryLocalId: Long?, categoryServerId: Int?): Resource<Unit> {
        return try {
            val token = tokenDataStore.token.firstOrNull()
            if (!token.isNullOrEmpty()) {
                try {
                    apiService.setWordCategory(
                        "Bearer $token",
                        word,
                        SetWordCategoryRequest(categoryServerId)
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to set word category on server: ${e.message}")
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }
}
