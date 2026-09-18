package com.wordwaverise.wordwaveriseapp.presentation.reader

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordwaverise.wordwaveriseapp.data.local.entity.OfflineBookEntity
import com.wordwaverise.wordwaveriseapp.data.remote.dto.reader.BookDto
import com.wordwaverise.wordwaveriseapp.data.repository.BookRepository
import com.wordwaverise.wordwaveriseapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BooksState(
    val books: List<BookDto> = emptyList(),
    val isLoading: Boolean = true,
    val isImporting: Boolean = false,
    val error: String? = null,
    /** Книга, которую надо открыть: импорт закончился и ждать больше нечего. */
    val openBookId: Int? = null,
    /** «Эта книга уже на полке» — сообщение, а не ошибка. */
    val notice: String? = null,
    val pasteOpen: Boolean = false,
    val pasteText: String = "",
    val pasteTitle: String = "",
    /**
     * Удаление книги спрашивают дважды, и это не перестраховка.
     *
     * Корзина стоит в строке книги, рядом с самой книгой: промах пальцем — и книга вместе с
     * местом, где её бросили, исчезает. Отменить это нечем, а вернуть можно только новой
     * загрузкой файла, которого под рукой может уже не быть.
     */
    val confirmDelete: BookDto? = null,
    val confirmDeleteAgain: BookDto? = null,
    /** Книга, для которой открыт диалог переименования. */
    val renameTarget: BookDto? = null,
    val renameTitle: String = "",
    val isRenaming: Boolean = false,
    /**
     * Офлайн-прогресс по книгам, где он есть — отсутствие ключа значит «не скачивалась».
     *
     * Только Android: у книги нет собственного офлайн-состояния на сервере, оно целиком
     * локальное, поэтому это карта, а не поле [BookDto].
     */
    val offline: Map<Int, OfflineBookEntity> = emptyMap()
)

@HiltViewModel
class BooksViewModel @Inject constructor(
    private val repository: BookRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BooksState())
    val state: StateFlow<BooksState> = _state.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            repository.offlineBooks().collect { rows ->
                _state.value = _state.value.copy(offline = rows.associateBy { it.bookId })
            }
        }
    }

    /**
     * Скачивает книгу целиком для чтения без сети: текст сразу, подсказки — по мере прогрева.
     *
     * Ошибка (в том числе дневной лимит или переполненная полка) идёт как обычное сообщение —
     * возврата к предыдущему состоянию тут нет, `offline`-запись просто не появляется/остаётся
     * `FAILED`, и кнопка снова доступна для повтора.
     */
    fun downloadOffline(bookId: Int) {
        viewModelScope.launch {
            when (val result = repository.downloadForOffline(bookId)) {
                is Resource.Success -> Unit
                else -> _state.value = _state.value.copy(error = result.message ?: "Не удалось скачать книгу")
            }
        }
    }

    /** Освобождает место: текст и подсказки уходят из Room, сама книга остаётся на сервере. */
    fun removeOffline(bookId: Int) {
        viewModelScope.launch { repository.removeOfflineData(bookId) }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val result = repository.books()) {
                is Resource.Success -> _state.value = _state.value.copy(
                    books = result.data.orEmpty(),
                    isLoading = false
                )
                else -> _state.value = _state.value.copy(
                    isLoading = false,
                    error = result.message
                )
            }
        }
    }

    fun importFile(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isImporting = true, error = null)
            when (val result = repository.uploadFile(uri, fallbackName = "book")) {
                is Resource.Success -> finishImport(
                    result.data!!.book,
                    result.data.alreadyExisted
                )
                else -> _state.value = _state.value.copy(isImporting = false, error = result.message)
            }
        }
    }

    fun importPasted() {
        val text = _state.value.pasteText.trim()
        if (text.isEmpty() || _state.value.isImporting) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isImporting = true, error = null)
            when (val result = repository.importText(text, _state.value.pasteTitle)) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        pasteOpen = false,
                        pasteText = "",
                        pasteTitle = ""
                    )
                    finishImport(result.data!!.book, result.data.alreadyExisted)
                }
                else -> _state.value = _state.value.copy(isImporting = false, error = result.message)
            }
        }
    }

    /**
     * ⚠️ `alreadyExisted` — не ошибка. Сервер узнал текст по хэшу и вернул книгу, которая уже
     * лежит, вместе с местом, где её бросили: открываем её там же и говорим об этом словами.
     */
    private fun finishImport(book: BookDto, alreadyExisted: Boolean) {
        _state.value = _state.value.copy(
            isImporting = false,
            openBookId = book.id,
            notice = if (alreadyExisted) "Эта книга уже на полке — открываем, где остановились" else null
        )
        refresh()
    }

    fun openHandled() {
        _state.value = _state.value.copy(openBookId = null)
    }

    fun clearNotice() {
        _state.value = _state.value.copy(notice = null, error = null)
    }

    fun showPaste(show: Boolean) {
        _state.value = _state.value.copy(pasteOpen = show)
    }

    fun setPasteText(text: String) {
        _state.value = _state.value.copy(pasteText = text)
    }

    fun setPasteTitle(title: String) {
        _state.value = _state.value.copy(pasteTitle = title)
    }

    fun startRename(book: BookDto) {
        _state.value = _state.value.copy(renameTarget = book, renameTitle = book.title)
    }

    fun setRenameTitle(title: String) {
        _state.value = _state.value.copy(renameTitle = title)
    }

    fun cancelRename() {
        _state.value = _state.value.copy(renameTarget = null, renameTitle = "")
    }

    fun confirmRename() {
        val book = _state.value.renameTarget ?: return
        val title = _state.value.renameTitle.trim()
        if (title.isEmpty() || _state.value.isRenaming) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isRenaming = true)
            when (val result = repository.rename(book.id, title)) {
                is Resource.Success -> _state.value = _state.value.copy(
                    books = _state.value.books.map { if (it.id == book.id) result.data!! else it },
                    renameTarget = null,
                    renameTitle = "",
                    isRenaming = false,
                    notice = "Книга переименована"
                )
                else -> _state.value = _state.value.copy(
                    isRenaming = false,
                    error = result.message ?: "Не удалось переименовать книгу"
                )
            }
        }
    }

    fun askDelete(book: BookDto?) {
        _state.value = _state.value.copy(confirmDelete = book, confirmDeleteAgain = null)
    }

    /** Первое «удалить» ничего не удаляет — оно только переводит вопрос во второй шаг. */
    fun askDeleteAgain() {
        val book = _state.value.confirmDelete ?: return
        _state.value = _state.value.copy(confirmDelete = null, confirmDeleteAgain = book)
    }

    fun cancelDelete() {
        _state.value = _state.value.copy(confirmDelete = null, confirmDeleteAgain = null)
    }

    fun confirmDelete() {
        val book = _state.value.confirmDeleteAgain ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(confirmDelete = null, confirmDeleteAgain = null)
            when (repository.delete(book.id)) {
                is Resource.Success -> _state.value = _state.value.copy(
                    books = _state.value.books.filterNot { it.id == book.id }
                )
                else -> _state.value = _state.value.copy(error = "Не удалось удалить книгу")
            }
        }
    }
}
