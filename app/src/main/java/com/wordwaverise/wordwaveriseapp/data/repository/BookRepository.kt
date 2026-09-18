package com.wordwaverise.wordwaveriseapp.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.wordwaverise.wordwaveriseapp.data.local.TokenDataStore
import com.wordwaverise.wordwaveriseapp.data.local.dao.OfflineBlockDao
import com.wordwaverise.wordwaveriseapp.data.local.dao.OfflineBookDao
import com.wordwaverise.wordwaveriseapp.data.local.dao.OfflineHintDao
import com.wordwaverise.wordwaveriseapp.data.local.entity.OfflineBlockEntity
import com.wordwaverise.wordwaveriseapp.data.local.entity.OfflineBookEntity
import com.wordwaverise.wordwaveriseapp.data.local.entity.OfflineHintEntity
import com.wordwaverise.wordwaveriseapp.data.remote.ApiService
import com.wordwaverise.wordwaveriseapp.data.remote.dto.category.CategoryDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.ContextAnalyzeRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.ContextHintDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.ContextTargetDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.BlockDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.BlockPageDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.BookDetailDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.BookmarkDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.SetBookmarkRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.BookDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.BookImportDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.ImportBookTextRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.OfflineHintDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.OfflineStatusDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.ReadingPositionDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.SetPositionRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.RenameBookRequest
import com.wordwaverise.wordwaveriseapp.util.NetworkError
import com.wordwaverise.wordwaveriseapp.util.Resource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Полка и текст книги — и, для Android, офлайн-копия того и другого.
 *
 * Онлайн ничего не кэшируется само по себе: блоки приезжают окнами по мере чтения и живут во
 * ViewModel ровно столько, сколько открыт экран. Офлайн — отдельное, явное действие
 * ([downloadForOffline]): читатель просит скачать конкретную книгу, и только тогда текст и
 * подсказки оседают в Room. [blocks] и [contextHint] сами решают, откуда отвечать — сетью, если
 * она есть, локальной копией, если её нет и книга скачана, — так что вызывающему коду (читалке)
 * не нужно знать, онлайн он сейчас или нет.
 */
@Singleton
class BookRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenDataStore: TokenDataStore,
    private val offlineBookDao: OfflineBookDao,
    private val offlineBlockDao: OfflineBlockDao,
    private val offlineHintDao: OfflineHintDao,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "BookRepository"

        /** Столько же, сколько принимает сервер: отдавать 40 МБ ради 400 незачем. */
        const val MAX_UPLOAD_BYTES = 32 * 1024 * 1024

        /** Пока прогрев идёт, статус спрашивают не чаще — джоб на книгу в тысячи вызовов модели. */
        private const val POLL_INTERVAL_MS = 4000L

        /** Тем же окном, что читалка просит текст: один размер страницы на оба вида скачивания. */
        private const val OFFLINE_PAGE_SIZE = 200
    }

    private val errorJson = Json { ignoreUnknownKeys = true; isLenient = true }

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

    /**
     * Окно блоков — сетью, а при её отсутствии из офлайн-копии, если книга скачана.
     *
     * ⚠️ Как в [com.wordwaverise.wordwaveriseapp.data.repository.SearchRepository.lookup]:
     * локальная копия пробуется на **любой** сбой сети, а не только на `IOException`, — сорвавшийся
     * запрос и разорванное соединение выглядят с этой стороны одинаково, и книга, которую читатель
     * скачал специально на случай отсутствия сети, не должна становиться пустой из-за того, что
     * исключение оказалось не того подкласса.
     */
    suspend fun blocks(id: Int, from: Int, limit: Int = 40): Resource<BlockPageDto> = call { token ->
        try {
            val response = apiService.getBookBlocks(token, id, from, limit)
            val page = response.data
                ?: return@call Resource.Error(response.message ?: "Не удалось загрузить текст")
            Resource.Success(page)
        } catch (e: Exception) {
            val offline = offlineBlocksPage(id, from, limit)
            if (offline != null) {
                Log.d(TAG, "Serving book $id blocks from $from offline")
                Resource.Success(offline)
            } else {
                throw e
            }
        }
    }

    private suspend fun offlineBlocksPage(bookId: Int, from: Int, limit: Int): BlockPageDto? {
        val meta = offlineBookDao.get(bookId) ?: return null
        val rows = offlineBlockDao.page(bookId, from, limit)
        if (rows.isEmpty()) return null
        val blocks = rows.mapNotNull { row ->
            runCatching { errorJson.decodeFromString(BlockDto.serializer(), row.payload) }.getOrNull()
        }
        val last = blocks.lastOrNull()?.ordinal
        return BlockPageDto(
            bookId = bookId,
            from = from,
            blocks = blocks,
            nextOrdinal = if (last != null && last + 1 < meta.blockCount) last + 1 else null
        )
    }

    /**
     * Подсказка на тап — сетью, а при её отсутствии из офлайн-копии по месту в книге.
     *
     * Отдельный от [com.wordwaverise.wordwaveriseapp.data.repository.SearchRepository.contextHint]
     * вызов: тому нечем найти офлайн-копию — у него нет ни книги, ни места в ней, только текст
     * предложения, — а искать в Room приходится по (книга, абзац, предложение, токен).
     */
    suspend fun contextHint(
        bookId: Int,
        blockOrdinal: Int,
        sentenceIndex: Int,
        sentenceText: String,
        tokenIndex: Int,
        tokenEnd: Int? = null
    ): Resource<ContextHintDto> {
        return try {
            val response = apiService.contextHint(ContextAnalyzeRequest(sentenceText, tokenIndex, tokenEnd = tokenEnd))
            if (response.status == "ok" && response.data != null) Resource.Success(response.data)
            else Resource.Error(response.message ?: "Не удалось разобрать слово")
        } catch (e: Exception) {
            // ⚠️ Прогрев ставит подсказку только на цельное слово — диапазон это тап,
            // расширенный вручную уже на экране, и предсказать его заранее нечем.
            val offline = if (tokenEnd == null || tokenEnd == tokenIndex) {
                offlineHintDao.get(bookId, blockOrdinal, sentenceIndex, tokenIndex)
            } else null
            val hint = offline?.let {
                runCatching { errorJson.decodeFromString(ContextHintDto.serializer(), it.payload) }.getOrNull()
            }
            if (hint != null) {
                Log.d(TAG, "Serving hint for book $bookId @$blockOrdinal:$sentenceIndex:$tokenIndex offline")
                Resource.Success(hint)
            } else {
                Resource.Error(NetworkError.getErrorMessage(e))
            }
        }
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
        removeOfflineData(id)
        Resource.Success(Unit)
    }

    /** Папка книги, если уже есть, переименуется вместе с ней — это делает сервер. */
    suspend fun rename(id: Int, title: String): Resource<BookDto> = call { token ->
        val response = apiService.renameBook(token, id, RenameBookRequest(title))
        val book = response.data
            ?: return@call Resource.Error(response.message ?: "Не удалось переименовать книгу")
        Resource.Success(book)
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

    // ── Офлайн ────────────────────────────────────────────────────────────

    /** Прогресс скачивания этой книги, для экрана полки — переживает уход с экрана и обратно. */
    fun offlineBook(bookId: Int): Flow<OfflineBookEntity?> = offlineBookDao.observe(bookId)

    fun offlineBooks(): Flow<List<OfflineBookEntity>> = offlineBookDao.observeAll()

    /**
     * Полное скачивание книги для чтения без сети: текст целиком, потом прогрев подсказок на
     * каждое слово, потом сами подсказки.
     *
     * Текст качается **первым и без зависимости от прогрева** — он не стоит ни одного вызова
     * модели, и даже если прогрев не успеет или упадёт, книга уже открывается офлайн, просто без
     * подсказок на ещё не прогретые слова. Прогрев живёт на сервере и переживает закрытие экрана;
     * если вызывающий код (`viewModelScope`) отменится посреди ожидания, начатый на сервере джоб
     * не пострадает — следующий вызов `start` застанет его тем же самым, идущим.
     */
    suspend fun downloadForOffline(bookId: Int): Resource<Unit> = call { token ->
        val detailResp = apiService.getBook(token, bookId)
        val detail = detailResp.data
            ?: return@call Resource.Error(detailResp.message ?: "Книга не найдена")

        offlineBookDao.put(
            OfflineBookEntity(
                bookId = bookId,
                detailPayload = errorJson.encodeToString(BookDetailDto.serializer(), detail),
                blockCount = detail.book.blockCount,
                status = OfflineBookEntity.STATUS_TEXT,
                totalTokens = 0,
                processedTokens = 0
            )
        )

        var from = 0
        while (true) {
            val page = apiService.getBookBlocks(token, bookId, from, OFFLINE_PAGE_SIZE)
            val data = page.data
            if (data == null) {
                markFailed(bookId)
                return@call Resource.Error(page.message ?: "Не удалось скачать текст книги")
            }
            offlineBlockDao.putAll(
                data.blocks.map {
                    OfflineBlockEntity(bookId, it.ordinal, errorJson.encodeToString(BlockDto.serializer(), it))
                }
            )
            from = data.nextOrdinal ?: break
        }

        val startResp = apiService.startOfflineDownload(token, bookId)
        var status = startResp.data
        if (startResp.status != "ok" || status == null) {
            markFailed(bookId)
            return@call Resource.Error(startResp.message ?: "Не удалось начать скачивание")
        }
        updateProgress(bookId, OfflineBookEntity.STATUS_WARMING, status)

        while (status?.running == true) {
            delay(POLL_INTERVAL_MS)
            val statusResp = apiService.getOfflineStatus(token, bookId)
            status = statusResp.data ?: break
            updateProgress(bookId, OfflineBookEntity.STATUS_WARMING, status)
        }

        updateProgress(bookId, OfflineBookEntity.STATUS_HINTS, status)
        var hintFrom = 0
        while (true) {
            val bundleResp = apiService.getOfflineBundle(token, bookId, hintFrom, OFFLINE_PAGE_SIZE)
            val page = bundleResp.data ?: break
            offlineHintDao.putAll(
                page.hints.map { hint ->
                    OfflineHintEntity(
                        bookId = bookId,
                        blockOrdinal = hint.blockOrdinal,
                        sentenceIndex = hint.sentenceIndex,
                        tokenIndex = hint.tokenIndex,
                        payload = errorJson.encodeToString(ContextHintDto.serializer(), hint.toContextHint())
                    )
                }
            )
            hintFrom = page.nextOrdinal ?: break
        }

        val finished = offlineBookDao.get(bookId) ?: return@call Resource.Error("Скачивание прервано")
        offlineBookDao.put(
            finished.copy(status = OfflineBookEntity.STATUS_READY, updatedAt = System.currentTimeMillis())
        )
        Resource.Success(Unit)
    }

    /** Отменяет офлайн-копию и освобождает место — сама книга на сервере не трогается. */
    suspend fun removeOfflineData(bookId: Int) {
        offlineHintDao.deleteForBook(bookId)
        offlineBlockDao.deleteForBook(bookId)
        offlineBookDao.delete(bookId)
    }

    private suspend fun updateProgress(bookId: Int, status: String, remote: OfflineStatusDto?) {
        val current = offlineBookDao.get(bookId) ?: return
        offlineBookDao.put(
            current.copy(
                status = status,
                totalTokens = remote?.totalTokens ?: current.totalTokens,
                processedTokens = remote?.processedTokens ?: current.processedTokens,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private suspend fun markFailed(bookId: Int) {
        val current = offlineBookDao.get(bookId) ?: return
        offlineBookDao.put(current.copy(status = OfflineBookEntity.STATUS_FAILED, updatedAt = System.currentTimeMillis()))
    }

    private fun OfflineHintDto.toContextHint() = ContextHintDto(
        target = ContextTargetDto(index = tokenIndex),
        lemma = lemma,
        pos = pos,
        translationRu = translationRu,
        senseId = senseId,
        senseMatched = senseMatched,
        senseDefinitionEn = senseDefinitionEn,
        phonetic = phonetic,
        audioUrl = audioUrl,
        translationsRu = translationsRu,
        cefr = cefr,
        register = register,
        countability = countability,
        entryAvailable = entryAvailable
    )

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

    /**
     * Бэкенд отвечает одной строкой всем клиентам сразу, и часть из них — код, а не фраза
     * (`book_limit_reached`, `offline_daily_limit_reached`): переводится здесь, а не там, где
     * упала загрузка, чтобы оба места, где лимит может сработать, показывали одно и то же.
     */
    private fun humanize(message: String?): String? = when (message) {
        "book_limit_reached" ->
            "В библиотеке уже 40 книг — удалите прочитанные, чтобы добавить новую"
        "offline_daily_limit_reached" ->
            "Офлайн-лимит на сегодня исчерпан: 2 книги в день. Остальные — завтра"
        else -> message
    }

    private fun extractBackendMessage(e: HttpException): String? = try {
        val body = e.response()?.errorBody()?.string()
        humanize(body?.let { errorJson.parseToJsonElement(it).jsonObject["message"]?.jsonPrimitive?.content })
    } catch (ex: Exception) {
        null
    }

    private suspend fun <T> call(block: suspend (String) -> Resource<T>): Resource<T> = try {
        val token = bearer()
        if (token == null) Resource.Error("Войдите, чтобы открыть свои книги") else block(token)
    } catch (e: HttpException) {
        Resource.Error(extractBackendMessage(e) ?: NetworkError.getErrorMessage(e))
    } catch (e: Exception) {
        Resource.Error(NetworkError.getErrorMessage(e))
    }
}
