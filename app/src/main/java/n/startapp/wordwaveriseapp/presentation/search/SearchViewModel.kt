package n.startapp.wordwaveriseapp.presentation.search

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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

    private var mediaPlayer: MediaPlayer? = null

    fun onSearchQueryChange(query: String) {
        Log.d(TAG, "Search query changed: $query")
        _state.value = _state.value.copy(
            searchQuery = query,
            error = null,
            suggestions = emptyList()
        )
        // Live suggestions for Russian (Cyrillic) input — translate to English candidates
        if (query.length >= 2 && query.any { it in '\u0400'..'\u04FF' }) {
            fetchSuggestions(query)
        }
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
                hasSearched = false,
                suggestions = emptyList()
            )

            when (val result = searchRepository.searchWord(query)) {
                is Resource.Success -> {
                    Log.d(TAG, "Search success! Word data: ${result.data}")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        wordData = result.data,
                        error = null,
                        hasSearched = true
                    )
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
                    // Fetch spelling/translation suggestions on failure
                    fetchSuggestions(query)
                }
                is Resource.Loading -> {
                    _state.value = _state.value.copy(isLoading = true)
                }
            }
        }
    }

    fun selectSuggestion(suggestion: String) {
        _state.value = _state.value.copy(
            searchQuery = suggestion,
            suggestions = emptyList()
        )
        searchWord()
    }

    private fun fetchSuggestions(query: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isFetchingSuggestions = true)
            val suggestions = searchRepository.getSuggestions(query)
            Log.d(TAG, "Suggestions for '$query': $suggestions")
            _state.value = _state.value.copy(
                suggestions = suggestions,
                isFetchingSuggestions = false
            )
        }
    }

    fun clearSearch() {
        Log.d(TAG, "Clearing search")
        stopAudio()
        _state.value = SearchState()
        _isSaved.value = false
    }

    fun playAudio(url: String) {
        if (_state.value.playingAudioUrl == url && _state.value.isPlayingAudio) {
            stopAudio()
            return
        }
        viewModelScope.launch(Dispatchers.Main) {
            try {
                _state.value = _state.value.copy(isPlayingAudio = true, playingAudioUrl = url)
                mediaPlayer?.release()
                mediaPlayer = null
                val mp = MediaPlayer()
                mediaPlayer = mp
                mp.setDataSource(url)
                mp.setOnPreparedListener { it.start() }
                mp.setOnCompletionListener {
                    _state.value = _state.value.copy(isPlayingAudio = false, playingAudioUrl = null)
                }
                mp.setOnErrorListener { _, _, _ ->
                    _state.value = _state.value.copy(isPlayingAudio = false, playingAudioUrl = null)
                    true
                }
                mp.prepareAsync()
            } catch (e: Exception) {
                Log.e(TAG, "Audio playback error: ${e.message}")
                _state.value = _state.value.copy(isPlayingAudio = false, playingAudioUrl = null)
            }
        }
    }

    fun stopAudio() {
        try {
            mediaPlayer?.let { if (it.isPlaying) it.stop() }
        } catch (_: Exception) { }
        mediaPlayer?.release()
        mediaPlayer = null
        _state.value = _state.value.copy(isPlayingAudio = false, playingAudioUrl = null)
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun saveWord() {
        val wordData = _state.value.wordData ?: return
        val word = wordData.word
        val firstDefinition = wordData.definitions.firstOrNull()
        Log.d(TAG, "Saving word: $word")
        viewModelScope.launch {
            when (savedWordsRepository.saveWord(
                word = word,
                translation = wordData.translation,
                definition = firstDefinition?.definition
            )) {
                is Resource.Success -> {
                    Log.d(TAG, "Word saved successfully")
                    _isSaved.value = true

                    // Automatically create flashcard
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
