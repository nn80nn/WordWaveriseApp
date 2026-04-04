package n.startapp.wordwaveriseapp.presentation.saved

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import n.startapp.wordwaveriseapp.data.repository.AuthRepository
import n.startapp.wordwaveriseapp.data.repository.CategoryRepository
import n.startapp.wordwaveriseapp.data.repository.SavedWordsRepository
import n.startapp.wordwaveriseapp.util.Resource
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
        syncWords()
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
                    syncWords()
                    viewModelScope.launch { categoryRepository.syncCategories() }
                }
            }
        }
    }

    fun deleteWord(word: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            when (savedWordsRepository.deleteWord(word)) {
                is Resource.Success -> _state.value = _state.value.copy(isLoading = false, error = null)
                is Resource.Error -> _state.value = _state.value.copy(isLoading = false, error = "Не удалось удалить слово")
                else -> {}
            }
        }
    }

    fun syncWords() {
        viewModelScope.launch {
            val success = savedWordsRepository.syncWords()
            _state.value = _state.value.copy(isOffline = !success)
        }
    }

    fun selectCategory(id: Long?) {
        _state.value = _state.value.copy(selectedCategoryId = id)
    }

    fun showCategorySheet() {
        _state.value = _state.value.copy(showCategorySheet = true)
    }

    fun hideCategorySheet() {
        _state.value = _state.value.copy(showCategorySheet = false, wordToMove = null)
    }

    fun setWordToMove(word: String) {
        _state.value = _state.value.copy(wordToMove = word, showCategorySheet = true)
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

    fun deleteCategory(id: Long, serverId: Int?) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(id, serverId)
            if (_state.value.selectedCategoryId == id) {
                _state.value = _state.value.copy(selectedCategoryId = null)
            }
        }
    }

    fun moveWordToCategory(word: String, categoryLocalId: Long?, categoryServerId: Int?) {
        viewModelScope.launch {
            savedWordsRepository.updateWordCategory(word, categoryLocalId)
            categoryRepository.setWordCategory(word, categoryLocalId, categoryServerId)
            _state.value = _state.value.copy(wordToMove = null, showCategorySheet = false)
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
