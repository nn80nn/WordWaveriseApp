package com.wordwaverise.wordwaveriseapp.presentation.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordwaverise.wordwaveriseapp.data.local.dao.CategoryDao
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.ContextAnalysisDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.ContextHintDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.BlockDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.BookDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.ChapterDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.SentenceDto
import com.wordwaverise.wordwaveriseapp.data.repository.BookRepository
import com.wordwaverise.wordwaveriseapp.data.repository.CategoryRepository
import com.wordwaverise.wordwaveriseapp.data.repository.SavedWordsRepository
import com.wordwaverise.wordwaveriseapp.data.repository.SearchRepository
import com.wordwaverise.wordwaveriseapp.data.local.entity.CategoryEntity
import com.wordwaverise.wordwaveriseapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Какое слово открыто в листе — адресуется так же, как адресуется текст. */
data class TapTarget(
    val blockOrdinal: Int,
    val sentenceIndex: Int,
    val tokenIndex: Int
)

data class ReaderState(
    val book: BookDto? = null,
    val chapters: List<ChapterDto> = emptyList(),
    val blocks: List<BlockDto> = emptyList(),
    val nextOrdinal: Int? = null,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,

    val target: TapTarget? = null,
    /** Быстрый ответ — то, что видно почти сразу. */
    val hint: ContextHintDto? = null,
    val isHinting: Boolean = false,
    /** Полный разбор — только если его попросили. */
    val analysis: ContextAnalysisDto? = null,
    val isAnalyzing: Boolean = false,

    /** Свои папки — для листа выбора по долгому нажатию. */
    val ownFolders: List<CategoryEntity> = emptyList(),
    val chosenFolders: List<Long> = emptyList(),
    val folderSheetOpen: Boolean = false,
    val isSaving: Boolean = false,
    /** Уже сохранённые пары «слово + значение», чтобы закладка знала своё состояние. */
    val savedKeys: Set<String> = emptySet(),
    val bookFolderName: String? = null,
    val message: String? = null
) {
    val atEnd: Boolean get() = nextOrdinal == null

    fun sentenceOf(target: TapTarget): SentenceDto? =
        blocks.firstOrNull { it.ordinal == target.blockOrdinal }
            ?.sentences?.firstOrNull { it.index == target.sentenceIndex }

    /** Слово, о котором сейчас речь: подсказка отвечает первой, полный разбор её уточняет. */
    val currentLemma: String? get() = hint?.lemma ?: analysis?.lemma
    val currentSenseId: String? get() = hint?.senseId ?: analysis?.senseId
    val currentDefinition: String? get() = hint?.senseDefinitionEn ?: analysis?.senseDefinitionEn
    val currentTranslation: String? get() = hint?.translationRu ?: analysis?.translationRu

    /**
     * Сохранено ли то, что сейчас разобрано.
     *
     * ⚠️ Значение сопоставляется со статьёй не всегда (`senseMatched = false`), и тогда
     * сохранение уходит **без** `senseId`, а сервер подставляет первое значение сам. Сравнение
     * по паре «слово + значение» в этом случае не нашло бы только что сохранённое, и закладка
     * оставалась бы пустой сразу после успеха. Нечего сравнивать — сравниваем по слову.
     */
    val currentSaved: Boolean
        get() {
            val lemma = currentLemma ?: return false
            val senseId = currentSenseId
                ?: return savedKeys.any { it.startsWith("${lemma.trim().lowercase()}#") }
            return key(lemma, senseId) in savedKeys
        }

    companion object {
        fun key(word: String, senseId: String?): String =
            "${word.trim().lowercase()}#${senseId ?: ""}"
    }
}

/**
 * Чтение одной книги.
 *
 * Блоки приезжают окнами и никуда не кэшируются: тап по слову всё равно требует сети, поэтому
 * офлайновая книга — это книга, в которой не работает главное.
 */
@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val books: BookRepository,
    private val search: SearchRepository,
    private val savedWords: SavedWordsRepository,
    private val categories: CategoryRepository,
    private val categoryDao: CategoryDao
) : ViewModel() {

    private val _state = MutableStateFlow(ReaderState())
    val state: StateFlow<ReaderState> = _state.asStateFlow()

    private var bookId: Int = 0
    /** Тап по второму слову, не дождавшись первого, — обычное дело: разбор отменяется, а не гонится. */
    private var analysisJob: Job? = null
    private var positionJob: Job? = null
    private var pendingOrdinal: Int? = null
    private var bookFolderServerId: Int? = null

    fun start(id: Int) {
        if (bookId == id && _state.value.book != null) return
        bookId = id
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val detail = books.book(id)) {
                is Resource.Success -> {
                    val data = detail.data!!
                    _state.value = _state.value.copy(
                        book = data.book,
                        chapters = data.chapters,
                        bookFolderName = data.book.title
                    )
                    // Окно начинается там, где книгу бросили: пролистывать 300 страниц руками
                    // человек не должен, а число сервер помнил всё это время.
                    loadWindow(data.book.position?.ordinal ?: 0, replace = true)
                }
                else -> _state.value = _state.value.copy(
                    isLoading = false,
                    error = detail.message
                )
            }
        }
    }

    init {
        // Папки и сохранённое читаются потоками из Room: закладка обязана знать своё состояние
        // сразу после сохранения, а список папок — сразу после того, как её завели.
        viewModelScope.launch {
            categories.categories.collect { all ->
                _state.value = _state.value.copy(ownFolders = all.filter { !it.readOnly })
            }
        }
        viewModelScope.launch {
            savedWords.savedWords.collect { words ->
                _state.value = _state.value.copy(
                    savedKeys = words.map { ReaderState.key(it.word, it.senseId) }.toSet()
                )
            }
        }
    }

    private suspend fun loadWindow(from: Int, replace: Boolean) {
        when (val page = books.blocks(bookId, from)) {
            is Resource.Success -> {
                val data = page.data!!
                _state.value = _state.value.copy(
                    blocks = if (replace) data.blocks else _state.value.blocks + data.blocks,
                    nextOrdinal = data.nextOrdinal,
                    isLoading = false,
                    isLoadingMore = false
                )
            }
            // Ошибка показывается, только когда показывать больше нечего: сорвавшаяся подгрузка
            // хвоста не должна закрывать собой страницу, которую человек читает.
            else -> _state.value = _state.value.copy(
                isLoading = false,
                isLoadingMore = false,
                error = if (_state.value.blocks.isEmpty()) page.message else null
            )
        }
    }

    fun loadMore() {
        val from = _state.value.nextOrdinal ?: return
        if (_state.value.isLoadingMore || _state.value.isLoading) return
        _state.value = _state.value.copy(isLoadingMore = true)
        viewModelScope.launch { loadWindow(from, replace = false) }
    }

    fun jumpTo(ordinal: Int) {
        closeTap()
        _state.value = _state.value.copy(isLoading = true, blocks = emptyList(), nextOrdinal = null)
        viewModelScope.launch {
            loadWindow(ordinal, replace = true)
            savePosition(ordinal)
        }
    }

    /**
     * Место записывается не чаще раза в пару секунд.
     *
     * ⚠️ Применяется то, что вернул сервер: он зажимает `ordinal` по размеру книги, и клиент с
     * устаревшим счётчиком блоков так исправляется сам, а не спорит.
     */
    fun savePosition(ordinal: Int) {
        pendingOrdinal = ordinal
        if (positionJob?.isActive == true) return
        positionJob = viewModelScope.launch {
            delay(POSITION_DEBOUNCE_MS)
            flushPosition()
        }
    }

    fun commitPosition() {
        positionJob?.cancel()
        viewModelScope.launch { flushPosition() }
    }

    private suspend fun flushPosition() {
        val ordinal = pendingOrdinal ?: return
        pendingOrdinal = null
        val result = books.setPosition(bookId, ordinal)
        if (result is Resource.Success) {
            val book = _state.value.book ?: return
            _state.value = _state.value.copy(book = book.copy(position = result.data))
        }
    }

    // ── Тап по слову ──────────────────────────────────────────────────

    /**
     * Тап по слову: сначала подсказка, и только она.
     *
     * Полный разбор пишется секундами — читающий не должен их ждать, чтобы узнать одно слово.
     * «Почему так» и перевод предложения приезжают по [loadDetails], то есть когда их попросили.
     */
    fun analyze(target: TapTarget) {
        val sentence = _state.value.sentenceOf(target) ?: return
        analysisJob?.cancel()
        _state.value = _state.value.copy(
            target = target,
            hint = null,
            analysis = null,
            isHinting = true,
            isAnalyzing = false
        )
        analysisJob = viewModelScope.launch {
            // ⚠️ Предложение, а не абзац: это ключ серверного кэша разбора, и второй читатель
            // той же строки не платит ничего.
            val result = search.contextHint(sentence.text, target.tokenIndex)
            _state.value = _state.value.copy(
                // Пустой ответ при успехе — это «модель не ответила», а не ошибка.
                hint = (result as? Resource.Success)?.data,
                isHinting = false
            )
        }
    }

    /** Подробности того же слова: почему это значение и как звучит всё предложение. */
    fun loadDetails() {
        val current = _state.value.target ?: return
        if (_state.value.analysis != null || _state.value.isAnalyzing) return
        val sentence = _state.value.sentenceOf(current) ?: return
        _state.value = _state.value.copy(isAnalyzing = true)
        viewModelScope.launch {
            val result = search.analyzeInContext(sentence.text, current.tokenIndex)
            // Пока разбор писался, могли тапнуть другое слово — тогда он уже не про то.
            if (_state.value.target != current) return@launch
            _state.value = _state.value.copy(
                analysis = (result as? Resource.Success)?.data,
                isAnalyzing = false
            )
        }
    }

    fun closeTap() {
        analysisJob?.cancel()
        _state.value = _state.value.copy(
            target = null,
            hint = null,
            analysis = null,
            isHinting = false,
            isAnalyzing = false
        )
    }

    // ── Сохранение ────────────────────────────────────────────────────

    /** Короткое нажатие: молча в папку книги. Чтение не прерывается — в этом вся разница. */
    fun saveQuietly() {
        val lemma = _state.value.currentLemma ?: return
        if (_state.value.isSaving) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            val folderServerId = ensureBookFolder()
            val local = folderServerId?.let { categoryDao.getByServerId(it)?.id }
            val result = savedWords.saveWord(
                word = lemma,
                translation = _state.value.currentTranslation,
                definition = _state.value.currentDefinition,
                senseId = _state.value.currentSenseId,
                categoryLocalIds = listOfNotNull(local),
                categoryServerIds = listOfNotNull(folderServerId)
            )
            finishSave(
                result,
                lemma,
                _state.value.currentSenseId,
                _state.value.bookFolderName?.let { listOf(it) }
            )
        }
    }

    /** Долгое нажатие: тот же лист папок, что в словаре, с уже отмеченной папкой книги. */
    fun openFolderSheet() {
        if (_state.value.currentLemma == null) return
        viewModelScope.launch {
            val folderServerId = ensureBookFolder()
            val local = folderServerId?.let { categoryDao.getByServerId(it)?.id }
            _state.value = _state.value.copy(
                folderSheetOpen = true,
                chosenFolders = listOfNotNull(local)
            )
        }
    }

    fun closeFolderSheet() {
        _state.value = _state.value.copy(folderSheetOpen = false)
    }

    fun toggleFolder(id: Long) {
        val chosen = _state.value.chosenFolders
        _state.value = _state.value.copy(
            chosenFolders = if (id in chosen) chosen - id else chosen + id
        )
    }

    fun clearFolders() {
        _state.value = _state.value.copy(chosenFolders = emptyList())
    }

    fun createFolder(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val result = categories.createCategory(trimmed)
            val created = (result as? Resource.Success)?.data
            if (created != null) {
                // Папку завели ради этого слова — отметить её самим было бы лишним шагом ровно
                // там, где человек уже сказал, чего хочет.
                _state.value = _state.value.copy(
                    chosenFolders = _state.value.chosenFolders + created.id
                )
            }
        }
    }

    fun confirmFolders() {
        val lemma = _state.value.currentLemma ?: return
        val chosen = _state.value.chosenFolders
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            val serverIds = chosen.mapNotNull { local ->
                _state.value.ownFolders.firstOrNull { it.id == local }?.serverId
            }
            val result = savedWords.saveWord(
                word = lemma,
                translation = _state.value.currentTranslation,
                definition = _state.value.currentDefinition,
                senseId = _state.value.currentSenseId,
                categoryLocalIds = chosen,
                categoryServerIds = serverIds
            )
            val names = chosen.mapNotNull { local ->
                _state.value.ownFolders.firstOrNull { it.id == local }?.name
            }
            _state.value = _state.value.copy(folderSheetOpen = false)
            finishSave(result, lemma, _state.value.currentSenseId, names)
        }
    }

    /** Снятие закладки ничего не спрашивает: убрать — это уже ответ. */
    fun unsave() {
        val lemma = _state.value.currentLemma ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            val entry = savedWords.entryFor(lemma, _state.value.currentSenseId)
            val message = if (entry == null) {
                "Это значение не сохранено"
            } else if (savedWords.deleteEntry(entry) is Resource.Success) {
                "Убрано из сохранённых"
            } else {
                "Не удалось убрать слово"
            }
            _state.value = _state.value.copy(isSaving = false, message = message)
        }
    }

    private fun finishSave(
        result: Resource<Boolean>,
        lemma: String,
        senseId: String?,
        folderNames: List<String>?
    ) {
        if (result is Resource.Success) {
            _state.value = _state.value.copy(
                isSaving = false,
                savedKeys = _state.value.savedKeys + ReaderState.key(lemma, senseId),
                message = if (folderNames.isNullOrEmpty()) "Слово сохранено"
                else "Сохранено в «${folderNames.joinToString("», «")}»"
            )
        } else {
            _state.value = _state.value.copy(
                isSaving = false,
                message = result.message ?: "Не удалось сохранить слово"
            )
        }
    }

    fun messageShown() {
        _state.value = _state.value.copy(message = null)
    }

    /**
     * Папка книги, заведённая при первом сохранении.
     *
     * ⚠️ Сервер отдаёт её **серверным** id, а Room ключует папки своим: без синхронизации сразу
     * после создания локальной строки ещё нет, и слово легло бы мимо папки — молча.
     */
    private suspend fun ensureBookFolder(): Int? {
        bookFolderServerId?.let { return it }
        val result = books.ensureFolder(bookId)
        val folder = (result as? Resource.Success)?.data ?: return null
        bookFolderServerId = folder.id
        // Локальной строки для только что заведённой папки ещё нет, а сохранение ждёт локальный
        // id — без этой синхронизации слово легло бы мимо папки, и молча.
        categories.syncCategories()
        _state.value = _state.value.copy(bookFolderName = folder.name)
        return folder.id
    }

    companion object {
        private const val POSITION_DEBOUNCE_MS = 2000L
    }
}
