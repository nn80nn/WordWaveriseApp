package n.startapp.wordwaveriseapp.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import n.startapp.wordwaveriseapp.data.repository.AuthRepository
import n.startapp.wordwaveriseapp.data.remote.ApiService
import n.startapp.wordwaveriseapp.data.remote.dto.saved.SaveWordRequest
import n.startapp.wordwaveriseapp.util.NetworkError
import javax.inject.Inject

@HiltViewModel
class WordDetailViewModel @Inject constructor(
    private val apiService: ApiService,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(WordDetailState())
    val state: StateFlow<WordDetailState> = _state.asStateFlow()

    init {
        val word = savedStateHandle.get<String>("word")
        if (word != null) {
            _state.update { it.copy(word = word) }
            loadWordDetail(word)
            checkIfWordIsSaved(word)
        }
    }

    private fun loadWordDetail(word: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val response = apiService.getWordDetails(word)
                if (response.status == "ok" && response.data != null) {
                    _state.update { it.copy(wordDetail = response.data, isLoading = false) }
                } else {
                    _state.update { it.copy(error = response.message ?: "Слово не найдено", isLoading = false) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = NetworkError.getErrorMessage(e), isLoading = false) }
            }
        }
    }

    private fun checkIfWordIsSaved(word: String) {
        viewModelScope.launch {
            try {
                val token = authRepository.token.firstOrNull()
                if (token != null) {
                    val savedWords = apiService.getSavedWords("Bearer $token")
                    val isSaved = savedWords.data?.words?.any { it.word.equals(word, ignoreCase = true) } == true
                    _state.update { it.copy(isSaved = isSaved) }
                }
            } catch (_: Exception) {
                // Silently fail - user might not be logged in
            }
        }
    }

    fun saveWord() {
        viewModelScope.launch {
            try {
                val token = authRepository.token.firstOrNull()
                if (token != null) {
                    val word = _state.value.word
                    val detail = _state.value.wordDetail
                    apiService.saveWord(
                        token = "Bearer $token",
                        request = SaveWordRequest(
                            word = word,
                            translation = detail?.translation,
                            definition = detail?.definitions?.firstOrNull()?.definition
                        )
                    )
                    _state.update { it.copy(isSaved = true) }
                }
            } catch (_: Exception) { }
        }
    }

    fun unsaveWord() {
        viewModelScope.launch {
            try {
                val token = authRepository.token.firstOrNull()
                if (token != null) {
                    apiService.deleteSavedWord(token = "Bearer $token", word = _state.value.word)
                    _state.update { it.copy(isSaved = false) }
                }
            } catch (_: Exception) { }
        }
    }
}
