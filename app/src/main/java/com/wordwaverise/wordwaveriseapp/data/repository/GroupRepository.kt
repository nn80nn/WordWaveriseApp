package com.wordwaverise.wordwaveriseapp.data.repository

import com.wordwaverise.wordwaveriseapp.data.local.TokenDataStore
import com.wordwaverise.wordwaveriseapp.data.remote.ApiService
import com.wordwaverise.wordwaveriseapp.data.remote.dto.group.AttemptDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.group.GroupDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.group.JoinByCodeRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.group.MyGroupsDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.group.ReportAttemptsRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.group.StudentAssignmentDto
import com.wordwaverise.wordwaveriseapp.util.NetworkError
import com.wordwaverise.wordwaveriseapp.util.Resource
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Классы ученика.
 *
 * Тонкий, как `ExerciseRepository`: тянет и разворачивает конверт, и больше ничего. Своей
 * таблицы у групп нет — офлайн они и не нужны, потому что всё, ради чего они существуют на
 * телефоне, это папки и карточки, а те уже лежат в Room и работают без сети сами.
 */
@Singleton
class GroupRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenDataStore: TokenDataStore
) {

    private suspend fun bearer(): String? =
        tokenDataStore.token.firstOrNull()?.takeIf { it.isNotEmpty() }?.let { "Bearer $it" }

    suspend fun myGroups(): Resource<MyGroupsDto> {
        val auth = bearer() ?: return Resource.Success(MyGroupsDto())
        return try {
            val response = apiService.getMyGroups(auth)
            if (response.status == "ok" && response.data != null) Resource.Success(response.data)
            else Resource.Error(response.message ?: "Не удалось загрузить группы")
        } catch (e: Exception) {
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }

    suspend fun assignments(groupId: Int? = null): Resource<List<StudentAssignmentDto>> {
        val auth = bearer() ?: return Resource.Success(emptyList())
        return try {
            val response = apiService.getAssignments(auth, groupId)
            if (response.status == "ok" && response.data != null) Resource.Success(response.data)
            else Resource.Error(response.message ?: "Не удалось загрузить задания")
        } catch (e: Exception) {
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }

    suspend fun joinByCode(code: String): Resource<GroupDto> {
        val auth = bearer() ?: return Resource.Error("Нужен вход в аккаунт")
        return try {
            val response = apiService.joinGroupByCode(auth, JoinByCodeRequest(code.trim().lowercase()))
            if (response.status == "ok" && response.data != null) Resource.Success(response.data)
            else Resource.Error(response.message ?: "Код не подошёл")
        } catch (e: Exception) {
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }

    suspend fun leave(groupId: Int): Resource<String> {
        val auth = bearer() ?: return Resource.Error("Нужен вход в аккаунт")
        return try {
            val response = apiService.leaveGroup(auth, groupId)
            if (response.status == "ok") Resource.Success(response.data ?: "Вы вышли из группы")
            else Resource.Error(response.message ?: "Не удалось выйти")
        } catch (e: Exception) {
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }

    /**
     * Отчёт о законченной сессии.
     *
     * Отправляется одним запросом в конце: результаты и так в памяти, а запрос на каждый вопрос
     * удвоил бы трафик практики ради данных, которые никто не читает в реальном времени.
     *
     * Очереди на Room здесь пока нет — отправка идёт только онлайн. Повторить её можно в любой
     * момент без последствий: ключ каждого ответа создан при ответе, и сервер отвечает
     * «дубликат», а не «плюс один».
     */
    suspend fun reportAttempts(
        groupId: Int,
        assignmentId: Int?,
        categoryId: Int?,
        attempts: List<AttemptDto>
    ): Resource<Unit> {
        if (attempts.isEmpty()) return Resource.Success(Unit)
        val auth = bearer() ?: return Resource.Error("Нужен вход в аккаунт")
        return try {
            val response = apiService.reportAttempts(
                auth,
                ReportAttemptsRequest(
                    groupId = groupId,
                    assignmentId = assignmentId,
                    categoryId = categoryId,
                    attempts = attempts
                )
            )
            if (response.status == "ok") Resource.Success(Unit)
            else Resource.Error(response.message ?: "Не удалось отправить результаты")
        } catch (e: Exception) {
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }
}
