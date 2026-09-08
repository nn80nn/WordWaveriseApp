package com.wordwaverise.wordwaveriseapp.presentation.reader

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val confirmDelete: BookDto? = null
)

@HiltViewModel
class BooksViewModel @Inject constructor(
    private val repository: BookRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BooksState())
    val state: StateFlow<BooksState> = _state.asStateFlow()

    init {
        refresh()
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

    fun askDelete(book: BookDto?) {
        _state.value = _state.value.copy(confirmDelete = book)
    }

    fun confirmDelete() {
        val book = _state.value.confirmDelete ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(confirmDelete = null)
            when (repository.delete(book.id)) {
                is Resource.Success -> _state.value = _state.value.copy(
                    books = _state.value.books.filterNot { it.id == book.id }
                )
                else -> _state.value = _state.value.copy(error = "Не удалось удалить книгу")
            }
        }
    }
}
