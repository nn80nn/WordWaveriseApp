package com.wordwaverise.wordwaveriseapp.data.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import com.wordwaverise.wordwaveriseapp.data.local.TokenDataStore
import com.wordwaverise.wordwaveriseapp.data.local.dao.CategoryDao
import com.wordwaverise.wordwaveriseapp.data.local.dao.SavedWordDao
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
    private val savedWordDao: SavedWordDao,
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
                    //
                    // ⚠️ Совпадение по имени ищется только среди своих папок. Папка класса
                    // приходит с сервера и офлайн заведена быть не могла, поэтому одноимённая
                    // локальная строка — это чужая, личная папка человека. Раньше её забирала
                    // себе первая же папка группы с тем же названием («Урок 5» у ученика и
                    // «Урок 5» у учителя): личная папка становилась readOnly, а её слова
                    // уезжали в чужую по remapWordsToOldestDuplicate.
                    val existing = categoryDao.getByServerId(dto.id)
                        ?: if (dto.groupId == null) categoryDao.getUnlinkedByName(dto.name) else null

                    if (existing == null) {
                        categoryDao.insert(
                            CategoryEntity(
                                serverId = dto.id,
                                name = dto.name,
                                color = dto.color,
                                groupServerId = dto.groupId,
                                groupName = dto.groupName,
                                readOnly = dto.readOnly
                            )
                        )
                    } else {
                        // Пометку группы надо переписывать на каждой синхронизации, а не только
                        // при вставке: папка становится «от группы» и перестаёт ею быть уже после
                        // того, как строка на телефоне появилась.
                        categoryDao.linkToServer(
                            existing.id, dto.id, dto.name, dto.color,
                            dto.groupId, dto.groupName, dto.readOnly
                        )
                    }
                }

                // Папка, удалённая на другом устройстве, иначе остаётся здесь навсегда —
                // с чипом, по которому открывается пустой список.
                val serverIds = response.data.map { it.id }
                if (serverIds.isEmpty()) categoryDao.deleteAllSynced()
                else categoryDao.deleteMissingFromServer(serverIds)

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
        readOnlyRefusal(id)?.let { return it }
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
        val borrowed = categoryDao.getByServerId(serverId)?.readOnly == true
        if (borrowed) return Resource.Error("Папкой группы делится преподаватель")
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
        readOnlyRefusal(id)?.let { return it }
        return try {
            // Папка исчезает из списков всех слов, которые в ней лежали. Одной SQL-командой
            // это больше не делается — папки слова хранятся списком в одной колонке, — но
            // строк тут десятки, а не тысячи, и обходить их дешевле, чем заводить таблицу-
            // связку ради единственного места, которое её бы использовало.
            savedWordDao.getAllSavedWords().firstOrNull().orEmpty()
                .filter { id in it.categoryIds }
                .forEach { savedWordDao.updateCategories(it.id, it.categoryIds - id) }
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

    /**
     * Отказ, если папка одолжена группой.
     *
     * ⚠️ Проверять надо **до** правки: и переименование, и удаление меняют локальную строку
     * раньше, чем спрашивают сервер, а сервер такую папку менять откажется. Без этой проверки
     * папка просто исчезала бы с телефона до следующей синхронизации — без слова на экране.
     */
    private suspend fun readOnlyRefusal(id: Long): Resource<Unit>? {
        val borrowed = categoryDao.getAll().firstOrNull()
            ?.firstOrNull { it.id == id }
            ?.readOnly == true
        return if (borrowed) Resource.Error("Папка группы доступна только для чтения") else null
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
