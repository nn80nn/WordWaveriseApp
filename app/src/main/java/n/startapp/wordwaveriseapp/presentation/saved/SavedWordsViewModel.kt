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
import n.startapp.wordwaveriseapp.data.repository.SavedWordsRepository
import n.startapp.wordwaveriseapp.util.Resource
import javax.inject.Inject

@HiltViewModel
class SavedWordsViewModel @Inject constructor(
    private val savedWordsRepository: SavedWordsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        private const val TAG = "SavedWordsViewModel"
    }

    private val _state = mutableStateOf(SavedWordsState())
    val state: State<SavedWordsState> = _state

    init {
        observeSavedWords()
        observeAuthStatus()
        syncWords()
    }

    private fun observeSavedWords() {
        viewModelScope.launch {
            savedWordsRepository.savedWords.collectLatest { words ->
                Log.d(TAG, "Saved words updated: ${words.size} words")
                _state.value = _state.value.copy(words = words)
            }
        }
    }

    private fun observeAuthStatus() {
        viewModelScope.launch {
            authRepository.token.collectLatest { token ->
                _state.value = _state.value.copy(isLoggedIn = !token.isNullOrEmpty())
                if (!token.isNullOrEmpty()) {
                    syncWords()
                }
            }
        }
    }

    fun deleteWord(word: String) {
        Log.d(TAG, "Deleting word: $word")
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            when (savedWordsRepository.deleteWord(word)) {
                is Resource.Success -> {
                    Log.d(TAG, "Word deleted successfully: $word")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = null
                    )
                }
                is Resource.Error -> {
                    Log.e(TAG, "Failed to delete word: $word")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Не удалось удалить слово"
                    )
                }
                else -> {}
            }
        }
    }

    fun syncWords() {
        Log.d(TAG, "Syncing words")
        viewModelScope.launch {
            val success = savedWordsRepository.syncWords()
            _state.value = _state.value.copy(isOffline = !success)
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
