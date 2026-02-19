package n.startapp.wordwaveriseapp.presentation.search

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import n.startapp.wordwaveriseapp.data.repository.SearchRepository
import n.startapp.wordwaveriseapp.util.Resource
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val savedWordsRepository: n.startapp.wordwaveriseapp.data.repository.SavedWordsRepository,
    private val flashcardRepository: n.startapp.wordwaveriseapp.data.repository.FlashcardRepository
) : ViewModel() {

    companion object {
        private const val TAG = "SearchViewModel"
    }

    private val _state = mutableStateOf(SearchState())
    val state: State<SearchState> = _state

    private val _isSaved = mutableStateOf(false)
    val isSaved: State<Boolean> = _isSaved

    fun onSearchQueryChange(query: String) {
        Log.d(TAG, "Search query changed: $query")
        _state.value = _state.value.copy(
            searchQuery = query,
            error = null
        )
    }

    fun searchWord() {
        val query = _state.value.searchQuery.trim()
        if (query.isEmpty()) {
            Log.w(TAG, "Search attempted with empty query")
            _state.value = _state.value.copy(
                error = "Пожалуйста, введите слово для поиска"
            )
            return
        }

        Log.d(TAG, "Starting search for: $query")
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                error = null,
                wordData = null,
                hasSearched = false
            )
            Log.d(TAG, "State set to loading")

            when (val result = searchRepository.searchWord(query)) {
                is Resource.Success -> {
                    Log.d(TAG, "Search success! Word data: ${result.data}")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        wordData = result.data,
                        error = null,
                        hasSearched = true
                    )
                    Log.d(TAG, "State updated with word data. Current state: ${_state.value}")
                    // Check if word is already saved
                    result.data?.let { wordData ->
                        checkIfWordIsSaved(wordData.word)
                    }
                }
                is Resource.Error -> {
                    Log.e(TAG, "Search error: ${result.message}")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message,
                        wordData = null,
                        hasSearched = true
                    )
                    Log.d(TAG, "State updated with error. Current state: ${_state.value}")
                }
                is Resource.Loading -> {
                    Log.d(TAG, "Resource loading state")
                    _state.value = _state.value.copy(isLoading = true)
                }
            }
        }
    }

    fun clearSearch() {
        Log.d(TAG, "Clearing search")
        _state.value = SearchState()
        _isSaved.value = false
    }

    fun saveWord() {
        val wordData = _state.value.wordData ?: return
        val word = wordData.word
        Log.d(TAG, "Saving word: $word")
        viewModelScope.launch {
            when (savedWordsRepository.saveWord(word)) {
                is Resource.Success -> {
                    Log.d(TAG, "Word saved successfully")
                    _isSaved.value = true

                    // Automatically create flashcard
                    val firstDefinition = wordData.definitions.firstOrNull()
                    if (firstDefinition != null) {
                        Log.d(TAG, "Creating flashcard for word: $word")
                        flashcardRepository.createFlashcard(
                            word = word,
                            definition = firstDefinition.definition,
                            example = firstDefinition.example,
                            translation = wordData.translation,
                            phonetic = wordData.phonetic,
                            partOfSpeech = firstDefinition.partOfSpeech
                        )
                        Log.d(TAG, "Flashcard created for word: $word")
                    }
                }
                is Resource.Error -> {
                    Log.e(TAG, "Failed to save word")
                }
                else -> {}
            }
        }
    }

    fun unsaveWord() {
        val word = _state.value.wordData?.word ?: return
        Log.d(TAG, "Removing word: $word")
        viewModelScope.launch {
            when (savedWordsRepository.deleteWord(word)) {
                is Resource.Success -> {
                    Log.d(TAG, "Word removed successfully")
                    _isSaved.value = false
                }
                is Resource.Error -> {
                    Log.e(TAG, "Failed to remove word")
                }
                else -> {}
            }
        }
    }

    private fun checkIfWordIsSaved(word: String) {
        viewModelScope.launch {
            _isSaved.value = savedWordsRepository.isWordSaved(word)
            Log.d(TAG, "Word $word is saved: ${_isSaved.value}")
        }
    }
}
