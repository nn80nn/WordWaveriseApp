package n.startapp.wordwaveriseapp.data.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import n.startapp.wordwaveriseapp.data.local.TokenDataStore
import n.startapp.wordwaveriseapp.data.local.dao.CategoryDao
import n.startapp.wordwaveriseapp.data.local.entity.CategoryEntity
import n.startapp.wordwaveriseapp.data.remote.ApiService
import n.startapp.wordwaveriseapp.data.remote.dto.category.CreateCategoryRequest
import n.startapp.wordwaveriseapp.data.remote.dto.category.RenameCategoryRequest
import n.startapp.wordwaveriseapp.data.remote.dto.category.SetWordCategoryRequest
import n.startapp.wordwaveriseapp.util.NetworkError
import n.startapp.wordwaveriseapp.util.Resource
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
                    categoryDao.insert(
                        CategoryEntity(
                            serverId = dto.id,
                            name = dto.name,
                            color = dto.color
                        )
                    )
                }
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
