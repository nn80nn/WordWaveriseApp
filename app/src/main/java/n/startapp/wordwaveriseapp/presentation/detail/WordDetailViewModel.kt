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
            _state.update { currentState ->
                currentState.copy(word = word)
            }
            loadWordDetail(word)
            checkIfWordIsSaved(word)
        }
    }

    private fun loadWordDetail(word: String) {
        viewModelScope.launch {
            _state.update { currentState ->
                currentState.copy(isLoading = true, error = null)
            }

            try {
                val response = apiService.searchWord(word)

                // Transform WordDto to WordDetailResponse
                // Note: You'll need to update the API to return WordDetailResponse
                // For now, this is a placeholder transformation
                val wordDto = response.data
                if (wordDto != null) {
                    // TODO: Update API to return the new format
                    // For now, we'll create a mock WordDetailResponse
                    val wordDetail = n.startapp.wordwaveriseapp.data.remote.dto.WordDetailResponse(
                        word = wordDto.word,
                        phonetic = wordDto.phonetic,
                        definitions = wordDto.definitions.map { def ->
                            n.startapp.wordwaveriseapp.data.remote.dto.Definition(
                                partOfSpeech = def.partOfSpeech,
                                definition = def.definition,
                                example = def.example
                            )
                        },
                        synonyms = wordDto.definitions.flatMap { it.synonyms }.distinct(),
                        antonyms = wordDto.definitions.flatMap { it.antonyms }.distinct(),
                        examples = wordDto.definitions.mapNotNull { it.example }
                    )

                    _state.update { currentState ->
                        currentState.copy(
                            wordDetail = wordDetail,
                            isLoading = false
                        )
                    }
                } else {
                    _state.update { currentState ->
                        currentState.copy(
                            error = "Слово не найдено",
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { currentState ->
                    currentState.copy(
                        error = NetworkError.getErrorMessage(e),
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun checkIfWordIsSaved(word: String) {
        viewModelScope.launch {
            try {
                val token = authRepository.token.firstOrNull()
                if (token != null) {
                    val savedWords = apiService.getSavedWords("Bearer $token")
                    val wordsList = savedWords.data?.words ?: emptyList()
                    val isSaved = wordsList.any { savedWord ->
                        savedWord.word.equals(word, ignoreCase = true)
                    }
                    _state.update { currentState ->
                        currentState.copy(isSaved = isSaved)
                    }
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
                    apiService.saveWord(
                        token = "Bearer $token",
                        request = SaveWordRequest(word = word)
                    )
                    _state.update { currentState ->
                        currentState.copy(isSaved = true)
                    }
                }
            } catch (_: Exception) {
                // Handle error
            }
        }
    }

    fun unsaveWord() {
        viewModelScope.launch {
            try {
                val token = authRepository.token.firstOrNull()
                if (token != null) {
                    val word = _state.value.word
                    apiService.deleteSavedWord(
                        token = "Bearer $token",
                        word = word
                    )
                    _state.update { currentState ->
                        currentState.copy(isSaved = false)
                    }
                }
            } catch (_: Exception) {
                // Handle error
            }
        }
    }
}
