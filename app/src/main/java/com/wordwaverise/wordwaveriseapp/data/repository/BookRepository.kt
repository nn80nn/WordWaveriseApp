package com.wordwaverise.wordwaveriseapp.data.repository

import android.content.Context
import android.net.Uri
import com.wordwaverise.wordwaveriseapp.data.local.TokenDataStore
import com.wordwaverise.wordwaveriseapp.data.remote.ApiService
import com.wordwaverise.wordwaveriseapp.data.remote.dto.category.CategoryDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.BlockPageDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.BookmarkDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.SetBookmarkRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.BookDetailDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.BookDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.BookImportDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.ImportBookTextRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.ReadingPositionDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.SetPositionRequest
import com.wordwaverise.wordwaveriseapp.util.NetworkError
import com.wordwaverise.wordwaveriseapp.util.Resource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Полка и текст книги.
 *
 * Ничего не кэшируется: тап по слову всё равно требует сети (разбор пишет модель), так что
 * офлайновая книга — это книга, в которой не работает главное. Блоки приезжают окнами по мере
 * чтения и живут во ViewModel ровно столько, сколько открыт экран.
 */
@Singleton
class BookRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenDataStore: TokenDataStore,
    @ApplicationContext private val context: Context
) {
    companion object {
        /** Столько же, сколько принимает сервер: отдавать 40 МБ ради 400 незачем. */
        const val MAX_UPLOAD_BYTES = 32 * 1024 * 1024
    }

    private suspend fun bearer(): String? =
        tokenDataStore.token.firstOrNull()?.takeIf { it.isNotEmpty() }?.let { "Bearer $it" }

    suspend fun books(): Resource<List<BookDto>> = call { token ->
        val response = apiService.getBooks(token)
        response.data ?: return@call Resource.Error(response.message ?: "Не удалось загрузить полку")
        Resource.Success(response.data)
    }

    suspend fun book(id: Int): Resource<BookDetailDto> = call { token ->
        val response = apiService.getBook(token, id)
        val detail = response.data
            ?: return@call Resource.Error(response.message ?: "Книга не найдена")
        Resource.Success(detail)
    }

    suspend fun blocks(id: Int, from: Int, limit: Int = 40): Resource<BlockPageDto> = call { token ->
        val response = apiService.getBookBlocks(token, id, from, limit)
        val page = response.data
            ?: return@call Resource.Error(response.message ?: "Не удалось загрузить текст")
        Resource.Success(page)
    }

    suspend fun setPosition(id: Int, ordinal: Int): Resource<ReadingPositionDto> = call { token ->
        val response = apiService.setReadingPosition(token, id, SetPositionRequest(ordinal))
        val position = response.data
            ?: return@call Resource.Error(response.message ?: "Не удалось запомнить место")
        Resource.Success(position)
    }

    suspend fun importText(text: String, title: String?): Resource<BookImportDto> = call { token ->
        val response = apiService.importBookText(
            token,
            ImportBookTextRequest(text = text, title = title?.trim()?.ifBlank { null })
        )
        val result = response.data
            ?: return@call Resource.Error(response.message ?: "Не удалось добавить текст")
        Resource.Success(result)
    }

    /**
     * Загрузка файла по выбранному в системном пикере адресу.
     *
     * ⚠️ Читается целиком в память, и это осознанно: парсеры EPUB и FB2 на сервере всё равно
     * читают архив целиком, а потолок в 32 МБ делает буфер предсказуемым. Файл больше отсекается
     * здесь — сервер ответит на него ровно то же самое, только после мегабайтов трафика.
     */
    suspend fun uploadFile(uri: Uri, fallbackName: String): Resource<BookImportDto> = call { token ->
        val bytes = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } ?: return@call Resource.Error("Не удалось прочитать файл")

        if (bytes.isEmpty()) return@call Resource.Error("Файл пуст")
        if (bytes.size > MAX_UPLOAD_BYTES) return@call Resource.Error("Файл слишком большой")

        val name = displayName(uri) ?: fallbackName
        val part = MultipartBody.Part.createFormData(
            name = "file",
            filename = name,
            body = bytes.toRequestBody("application/octet-stream".toMediaTypeOrNull())
        )
        val response = apiService.uploadBook(token, part)
        val result = response.data
            ?: return@call Resource.Error(response.message ?: "Не удалось разобрать файл")
        Resource.Success(result)
    }

    suspend fun bookmarks(id: Int): Resource<List<BookmarkDto>> = call { token ->
        val response = apiService.getBookmarks(token, id)
        Resource.Success(response.data.orEmpty())
    }

    /** Идемпотентно: отметить одно место дважды — это одна закладка, а не две. */
    suspend fun addBookmark(id: Int, ordinal: Int): Resource<BookmarkDto> = call { token ->
        val response = apiService.addBookmark(token, id, SetBookmarkRequest(ordinal))
        val mark = response.data
            ?: return@call Resource.Error(response.message ?: "Не удалось поставить закладку")
        Resource.Success(mark)
    }

    suspend fun removeBookmark(id: Int, ordinal: Int): Resource<Unit> = call { token ->
        apiService.removeBookmark(token, id, ordinal)
        Resource.Success(Unit)
    }

    suspend fun delete(id: Int): Resource<Unit> = call { token ->
        apiService.deleteBook(token, id)
        Resource.Success(Unit)
    }

    /**
     * Папка книги, заводится при первом сохранении из неё.
     *
     * Идемпотентно на сервере: два быстрых сохранения подряд не откроют две одинаковые папки.
     */
    suspend fun ensureFolder(bookId: Int): Resource<CategoryDto> = call { token ->
        val response = apiService.ensureBookFolder(token, bookId)
        val folder = response.data
            ?: return@call Resource.Error(response.message ?: "Не удалось завести папку книги")
        Resource.Success(folder)
    }

    /**
     * Имя файла из провайдера — только ради подсказки серверу.
     *
     * ⚠️ Формат он определяет **по байтам**: `.txt` с HTML внутри и `.epub`, который на деле
     * зазипованный FB2, встречаются постоянно, поэтому расширению здесь никто не верит.
     */
    private fun displayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }.getOrNull()

    private suspend fun <T> call(block: suspend (String) -> Resource<T>): Resource<T> = try {
        val token = bearer()
        if (token == null) Resource.Error("Войдите, чтобы открыть свои книги") else block(token)
    } catch (e: Exception) {
        Resource.Error(NetworkError.getErrorMessage(e))
    }
}
