package com.wordwaverise.wordwaveriseapp.presentation.reader

import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordwaverise.wordwaveriseapp.data.local.SettingsDataStore
import com.wordwaverise.wordwaveriseapp.data.local.dao.CategoryDao
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.ContextAnalysisDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.ContextHintDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.BlockDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.BookmarkDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.BookDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.ChapterDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.SentenceDto
import com.wordwaverise.wordwaveriseapp.data.repository.BookRepository
import com.wordwaverise.wordwaveriseapp.data.repository.CategoryRepository
import com.wordwaverise.wordwaveriseapp.data.repository.FlashcardRepository
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
import kotlinx.coroutines.flow.first
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
    /** Ordinal самого первого загруженного блока — граница, до которой можно листать назад. */
    val firstOrdinal: Int = 0,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isLoadingBefore: Boolean = false,
    /**
     * Блок, на который надо встать при открытии.
     *
     * Отдельно от позиции, потому что окно начинается **раньше** неё: листать назад человек
     * должен мочь сразу, а не после того, как долистает до начала загруженного куска.
     */
    val openAt: Int? = null,
    /** Листать страницами вместо скролла. Настройка на всё приложение, не на книгу. */
    val paged: Boolean = false,
    /** Точное место внутри абзаца, если его помнит это устройство. */
    val openOffset: Int = 0,
    val settingsOpen: Boolean = false,
    val bookmarks: List<BookmarkDto> = emptyList(),
    val bookmarksOpen: Boolean = false,
    /** Абзац, который сейчас наверху экрана: по нему закладка знает, стоит она или нет. */
    val currentOrdinal: Int = 0,
    /**
     * Абзац, который человек **видит**, — то, что отмечает закладка.
     *
     * ⚠️ Это не то же самое, что [currentOrdinal]. Место хранит абзац, накрывающий верх
     * страницы, чтобы книга открылась ровно там же; но длинный абзац начинается страницей
     * раньше, и закладка на него приводила читателя назад — «ставится на абзац выше».
     * Закладка отмечает первый абзац, который на этой странице **начинается**.
     */
    val visibleOrdinal: Int = 0,
    /**
     * Абзац, к которому только что перешли, — его подсвечивает короткая вспышка.
     *
     * Переход по закладке или оглавлению меняет весь экран разом, и без метки человеку негде
     * узнать, туда ли он попал: страница новая, а какая на ней строка та самая — непонятно.
     */
    val flashOrdinal: Int? = null,
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
    val atStart: Boolean get() = firstOrdinal == 0

    /** Отмечено ли **это** место. Закладка на соседнем абзаце — не эта закладка. */
    val bookmarkedHere: Boolean get() = bookmarks.any { it.ordinal == visibleOrdinal }

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
    private val flashcards: FlashcardRepository,
    private val categoryDao: CategoryDao,
    private val settings: SettingsDataStore
) : ViewModel() {

    private val _state = MutableStateFlow(ReaderState())
    val state: StateFlow<ReaderState> = _state.asStateFlow()

    private var bookId: Int = 0
    /** Тап по второму слову, не дождавшись первого, — обычное дело: разбор отменяется, а не гонится. */
    private var analysisJob: Job? = null
    private var positionJob: Job? = null

    /** Плеер произношения. Живёт не дольше экрана — иначе слово звучит из закрытой книги. */
    private var player: MediaPlayer? = null
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
                    // ⚠️ Окно начинается **раньше** сохранённого места, а не на нём. Иначе
                    // единственное направление, куда можно листать, — вперёд: перечитать
                    // предыдущий абзац, ради чего в книгу и возвращаются, было нечем.
                    val position = data.book.position?.ordinal ?: 0
                    val from = (position - BACKFILL_BLOCKS).coerceAtLeast(0)
                    loadWindow(from, replace = true)
                    loadBookmarks()
                    // ⚠️ Пиксель применяется, только если абзац тот же: читали на другом
                    // устройстве — сервер знает другое место, и локальное смещение не про него.
                    val local = settings.readerOffset(id).first()
                    val offset = local?.takeIf { it.first == position }?.second ?: 0
                    _state.value = _state.value.copy(
                        openAt = position,
                        openOffset = offset,
                        // Место известно с самого открытия: до первой прокрутки читатель уже
                        // стоит здесь, и закладка с переключением режима обязаны это знать.
                        currentOrdinal = position,
                        visibleOrdinal = position
                    )
                }
                else -> _state.value = _state.value.copy(
                    isLoading = false,
                    error = detail.message
                )
            }
        }
    }

    init {
        viewModelScope.launch {
            /**
             * ⚠️ Смена режима и метка места — **одно** обновление состояния.
             *
             * Скролл и страницы читают одну и ту же метку `openAt`, и тот, кто увидит её
             * первым, её же и снимает. Поставленная отдельным шагом, она доставалась
             * уходящему режиму, а приходящий открывался с начала книги — то есть человек
             * терял место ровно тем действием, которое места не касается.
             */
            settings.readerPaged.collect { paged ->
                val current = _state.value
                _state.value = if (current.paged == paged) {
                    current.copy(paged = paged)
                } else {
                    current.copy(paged = paged, openAt = current.currentOrdinal, openOffset = 0)
                }
            }
        }
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
                val blocks = if (replace) data.blocks else _state.value.blocks + data.blocks
                _state.value = _state.value.copy(
                    blocks = blocks,
                    firstOrdinal = blocks.firstOrNull()?.ordinal ?: from,
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

    /**
     * Кусок перед началом загруженного — то, чем листание назад вообще возможно.
     *
     * ⚠️ Дописывается спереди, поэтому список сдвигается: экран обязан подвинуть свою позицию
     * на столько же элементов, иначе человек, долиставший до верха, прыгает вперёд ровно в тот
     * момент, когда шёл назад.
     */
    fun loadBefore() {
        val current = _state.value
        if (current.atStart || current.isLoadingBefore || current.isLoading) return
        val from = (current.firstOrdinal - BACKFILL_BLOCKS).coerceAtLeast(0)
        _state.value = current.copy(isLoadingBefore = true)
        viewModelScope.launch {
            when (val page = books.blocks(bookId, from, limit = current.firstOrdinal - from)) {
                is Resource.Success -> {
                    val earlier = page.data!!.blocks
                    _state.value = _state.value.copy(
                        blocks = earlier + _state.value.blocks,
                        firstOrdinal = earlier.firstOrNull()?.ordinal ?: from,
                        isLoadingBefore = false
                    )
                }
                else -> _state.value = _state.value.copy(isLoadingBefore = false)
            }
        }
    }

    fun loadBookmarks() {
        viewModelScope.launch {
            val result = books.bookmarks(bookId)
            if (result is Resource.Success) {
                _state.value = _state.value.copy(bookmarks = result.data.orEmpty())
            }
        }
    }

    fun showBookmarks(show: Boolean) {
        if (show) loadBookmarks()
        _state.value = _state.value.copy(bookmarksOpen = show)
    }

    /** Одна кнопка на оба действия: место либо отмечено, либо нет, третьего состояния нет. */
    fun toggleBookmark() {
        val ordinal = _state.value.visibleOrdinal
        viewModelScope.launch {
            if (_state.value.bookmarkedHere) {
                books.removeBookmark(bookId, ordinal)
                _state.value = _state.value.copy(
                    bookmarks = _state.value.bookmarks.filterNot { it.ordinal == ordinal },
                    message = "Закладка убрана"
                )
            } else {
                val result = books.addBookmark(bookId, ordinal)
                if (result is Resource.Success) {
                    _state.value = _state.value.copy(
                        bookmarks = listOf(result.data!!) + _state.value.bookmarks,
                        message = "Закладка поставлена"
                    )
                } else {
                    _state.value = _state.value.copy(message = result.message)
                }
            }
        }
    }

    fun setPaged(paged: Boolean) {
        viewModelScope.launch { settings.setReaderPaged(paged) }
    }

    fun showSettings(show: Boolean) {
        _state.value = _state.value.copy(settingsOpen = show)
    }

    fun openHandled() {
        _state.value = _state.value.copy(openAt = null, openOffset = 0)
    }

    /** Вспышку гасит экран, а не таймер модели: считать её надо с момента, когда её видно. */
    fun flashShown() {
        if (_state.value.flashOrdinal != null) {
            _state.value = _state.value.copy(flashOrdinal = null)
        }
    }

    fun jumpTo(ordinal: Int) {
        closeTap()
        _state.value = _state.value.copy(isLoading = true, blocks = emptyList(), nextOrdinal = null)
        viewModelScope.launch {
            loadWindow(ordinal, replace = true)
            savePosition(ordinal)
            _state.value = _state.value.copy(openAt = ordinal, flashOrdinal = ordinal)
        }
    }

    /**
     * Место записывается не чаще раза в пару секунд.
     *
     * ⚠️ Применяется то, что вернул сервер: он зажимает `ordinal` по размеру книги, и клиент с
     * устаревшим счётчиком блоков так исправляется сам, а не спорит.
     */
    /**
     * @param offset точное место внутри абзаца — пиксель для скролла, верх строки для страниц.
     */
    /**
     * Что сейчас на экране целиком: закладке этого достаточно, а месту — нет.
     *
     * Отдельный вызов, а не поле в [savePosition]: место записывается с дебаунсом и уезжает на
     * сервер, а видимый абзац меняется на каждой странице и нужен только флажку в панели.
     */
    /**
     * Прослушать слово.
     *
     * Плеер один на экран и освобождается перед каждым запуском: два нажатия подряд — это
     * замена записи, а не хор. Ошибку не показываем: звук — приятное дополнение к подсказке,
     * и сообщение о нём поверх чтения стоит больше, чем сам звук.
     */
    override fun onCleared() {
        super.onCleared()
        player?.release()
        player = null
    }

    fun playAudio(url: String) {
        viewModelScope.launch {
            runCatching {
                player?.release()
                player = MediaPlayer().apply {
                    setDataSource(url)
                    setOnPreparedListener { it.start() }
                    setOnCompletionListener { it.release(); if (player === it) player = null }
                    prepareAsync()
                }
            }
        }
    }

    fun setVisible(ordinal: Int) {
        if (_state.value.visibleOrdinal != ordinal) {
            _state.value = _state.value.copy(visibleOrdinal = ordinal)
        }
    }

    fun savePosition(ordinal: Int, offset: Int = 0) {
        _state.value = _state.value.copy(currentOrdinal = ordinal)
        viewModelScope.launch { settings.setReaderOffset(bookId, ordinal, offset) }
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
            createCard(lemma)
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
            createCard(lemma)
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

    /**
     * Карточка заводится тем же действием, что и сохранение, — и живёт на подсказке.
     *
     * ⚠️ Статьи у слова из книги может не быть вовсе: она пишется минуты, а тап случился сейчас.
     * Ждать её значит выронить слово из повторений на день, поэтому карточка берёт то, что было
     * на экране: перевод и предложение, в котором слово встретилось. Английское определение
     * допишет обычное обновление из корпуса, когда статья появится, — `customized` не ставится
     * именно поэтому.
     */
    private suspend fun createCard(lemma: String) {
        val sentence = _state.value.target?.let { _state.value.sentenceOf(it) }?.text
        flashcards.createFlashcard(
            word = lemma,
            definition = _state.value.currentDefinition.orEmpty(),
            example = sentence,
            translation = _state.value.currentTranslation,
            partOfSpeech = _state.value.hint?.pos ?: _state.value.analysis?.pos,
            senseId = _state.value.currentSenseId
        )
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

        /**
         * Сколько блоков держать позади текущего места.
         *
         * Пятнадцать — это примерно экран назад: столько перечитывают, вернувшись к книге, и
         * столько же стоит грузить вперёд одним куском, чтобы не дёргать сервер на каждый абзац.
         */
        private const val BACKFILL_BLOCKS = 15
    }
}
