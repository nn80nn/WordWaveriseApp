package com.wordwaverise.wordwaveriseapp.presentation.saved

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.wordwaverise.wordwaveriseapp.data.local.entity.SavedWordEntity
import com.wordwaverise.wordwaveriseapp.data.repository.AuthRepository
import com.wordwaverise.wordwaveriseapp.data.repository.CategoryRepository
import com.wordwaverise.wordwaveriseapp.data.repository.SavedWordsRepository
import com.wordwaverise.wordwaveriseapp.util.Resource
import com.wordwaverise.wordwaveriseapp.util.SyncResult
import javax.inject.Inject

@HiltViewModel
class SavedWordsViewModel @Inject constructor(
    private val savedWordsRepository: SavedWordsRepository,
    private val categoryRepository: CategoryRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        private const val TAG = "SavedWordsViewModel"
    }

    private val _state = mutableStateOf(SavedWordsState())
    val state: State<SavedWordsState> = _state

    init {
        observeSavedWords()
        observeCategories()
        observeAuthStatus()
    }

    private fun observeSavedWords() {
        viewModelScope.launch {
            savedWordsRepository.savedWords.collectLatest { words ->
                _state.value = _state.value.copy(words = words)
            }
        }
    }

    private fun observeCategories() {
        viewModelScope.launch {
            categoryRepository.categories.collectLatest { cats ->
                _state.value = _state.value.copy(categories = cats)
            }
        }
    }

    private fun observeAuthStatus() {
        viewModelScope.launch {
            authRepository.token.collectLatest { token ->
                _state.value = _state.value.copy(isLoggedIn = !token.isNullOrEmpty())
                if (!token.isNullOrEmpty()) {
                    // ⚠️ Папки первыми и до конца: слово приносит с сервера **серверный** id
                    // своей папки, и превратить его в локальный можно только когда строка папки
                    // уже есть. Иначе слово из папки класса ляжет без папки и под её чипом не
                    // покажется — до следующей синхронизации.
                    viewModelScope.launch {
                        categoryRepository.syncCategories()
                        syncWords()
                    }
                }
            }
        }
    }

    /**
     * Убирает одну запись, а не всё написание.
     *
     * Человек мог сохранить два значения `resolve` как два слова; забрать вместе с одним и
     * второе значило бы уничтожить работу, о которой он не просил.
     */
    fun deleteEntry(entry: SavedWordEntity) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            when (savedWordsRepository.deleteEntry(entry)) {
                is Resource.Success -> _state.value = _state.value.copy(isLoading = false, error = null)
                is Resource.Error -> _state.value = _state.value.copy(isLoading = false, error = "Не удалось удалить слово")
                else -> {}
            }
        }
    }

    /**
     * Pull to refresh. A failed sync used to be a dead end — the only way to
     * retry was to leave the tab and come back, because nothing on the screen
     * asked the server again.
     */
    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRefreshing = true)
            categoryRepository.syncCategories()
            val result = savedWordsRepository.syncWords()
            _state.value = _state.value.copy(
                isRefreshing = false,
                isOffline = result == SyncResult.OFFLINE
            )
        }
    }

    fun syncWords() {
        viewModelScope.launch {
            val result = savedWordsRepository.syncWords()
            _state.value = _state.value.copy(isOffline = result == SyncResult.OFFLINE)
        }
    }

    fun selectCategory(id: Long?) {
        _state.value = _state.value.copy(selectedCategoryId = id)
    }

    fun showCategorySheet() {
        _state.value = _state.value.copy(showCategorySheet = true)
    }

    fun hideCategorySheet() {
        _state.value = _state.value.copy(
            showCategorySheet = false,
            entryToFile = null,
            chosenFolders = emptySet()
        )
    }

    fun setWordToFile(entry: SavedWordEntity) {
        _state.value = _state.value.copy(
            entryToFile = entry,
            chosenFolders = entry.categoryIds.toSet(),
            showCategorySheet = true
        )
    }

    fun toggleFolder(id: Long) {
        val chosen = _state.value.chosenFolders
        _state.value = _state.value.copy(
            chosenFolders = if (id in chosen) chosen - id else chosen + id
        )
    }

    fun setNewCategoryName(name: String) {
        _state.value = _state.value.copy(newCategoryName = name)
    }

    fun createCategory() {
        val name = _state.value.newCategoryName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            categoryRepository.createCategory(name)
            _state.value = _state.value.copy(newCategoryName = "")
        }
    }

    /**
     * Переименование правит только имя: слова, их привязка к папке и серверный id остаются.
     * Пустое имя и имя без изменений не отправляются — это не переименование, а способ
     * оставить папку без подписи.
     */
    fun renameCategory(id: Long, serverId: Int?, name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            categoryRepository.renameCategory(id, serverId, trimmed)
        }
    }

    /**
     * Готовит ссылку и отдаёт её экрану — дальше системный лист «Поделиться».
     *
     * Именно лист, а не буфер обмена: на телефоне папку отправляют в конкретный чат, и
     * «скопировано» оставляет человека доделывать это руками.
     */
    fun shareCategory(serverId: Int?) {
        viewModelScope.launch {
            when (val result = categoryRepository.shareCategory(serverId)) {
                is Resource.Success -> _state.value =
                    _state.value.copy(pendingShareUrl = result.data)
                is Resource.Error -> _state.value =
                    _state.value.copy(importMessage = result.message)
                else -> {}
            }
        }
    }

    /** Ссылку отдали системе — второй раз лист открываться не должен. */
    fun shareHandled() {
        _state.value = _state.value.copy(pendingShareUrl = null)
    }

    fun setImportLink(value: String) {
        _state.value = _state.value.copy(importLink = value)
    }

    fun importSharedFolder() {
        val link = _state.value.importLink.trim()
        if (link.isBlank() || _state.value.importing) return
        viewModelScope.launch {
            _state.value = _state.value.copy(importing = true, importMessage = null)
            when (val result = categoryRepository.importSharedFolder(link)) {
                is Resource.Success -> {
                    val data = result.data
                    val alreadyHad = data?.alreadyHad ?: 0
                    // Про слова, которые остались на своих местах, надо сказать прямо: иначе
                    // «добавил 3 из 10» выглядит как потеря, а не как уважение к своим папкам.
                    val tail = if (alreadyHad > 0) ", $alreadyHad уже были у вас" else ""
                    _state.value = _state.value.copy(
                        importing = false,
                        importLink = "",
                        importMessage = "Папка «${data?.name}»: ${data?.added} новых слов$tail"
                    )
                    // Слова приехали на сервер — местная копия догоняет их сама.
                    syncWords()
                }
                is Resource.Error -> _state.value = _state.value.copy(
                    importing = false,
                    importMessage = result.message
                )
                else -> {}
            }
        }
    }

    fun dismissImportMessage() {
        _state.value = _state.value.copy(importMessage = null)
    }

    fun deleteCategory(id: Long, serverId: Int?) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(id, serverId)
            if (_state.value.selectedCategoryId == id) {
                _state.value = _state.value.copy(selectedCategoryId = null)
            }
        }
    }

    /**
     * Раскладывает запись по отмеченным папкам — по всем сразу.
     *
     * Слово честно принадлежит и уроку, и теме к экзамену, и заставлять человека выбирать,
     * какая из двух папок окажется неправильной, больше незачем.
     */
    fun saveFolders() {
        val entry = _state.value.entryToFile ?: return
        val chosen = _state.value.chosenFolders
        val categories = _state.value.categories
        viewModelScope.launch {
            savedWordsRepository.setFolders(
                entry = entry,
                localIds = chosen.toList(),
                // Папка, заведённая офлайн, серверного id ещё не имеет: сервер о ней узнает
                // на следующей синхронизации, а до тех пор раскладка живёт на телефоне.
                serverIds = categories.filter { it.id in chosen }.mapNotNull { it.serverId }
            )
            _state.value = _state.value.copy(
                entryToFile = null,
                chosenFolders = emptySet(),
                showCategorySheet = false
            )
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
