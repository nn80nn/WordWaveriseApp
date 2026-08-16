package com.wordwaverise.wordwaveriseapp.presentation.search

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.wordwaverise.wordwaveriseapp.data.remote.dto.DefinitionDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.PronunciationDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.WordDetailResponse
import com.wordwaverise.wordwaveriseapp.data.remote.dto.WordDto
import com.wordwaverise.wordwaveriseapp.data.repository.SearchRepository
import com.wordwaverise.wordwaveriseapp.util.Resource
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val savedWordsRepository: com.wordwaverise.wordwaveriseapp.data.repository.SavedWordsRepository,
    private val flashcardRepository: com.wordwaverise.wordwaveriseapp.data.repository.FlashcardRepository
) : ViewModel() {

    companion object {
        private const val TAG = "SearchViewModel"
    }

    private val _state = mutableStateOf(SearchState())
    val state: State<SearchState> = _state

    private val _isSaved = mutableStateOf(false)
    val isSaved: State<Boolean> = _isSaved

    private var mediaPlayer: MediaPlayer? = null
    private var suggestJob: Job? = null

    fun onSearchQueryChange(query: String) {
        _state.value = _state.value.copy(
            searchQuery = query,
            error = null,
            suggestions = emptyList()
        )
        suggestJob?.cancel()
        // Autocomplete English single words only. Russian input is answered by the lookup itself
        // now, with explained options, so pre-fetching bare strings while typing is just noise.
        val isEnglishWord = query.length >= 2 &&
            query.none { it in 'Ѐ'..'ӿ' } &&
            !query.trim().contains(' ')
        if (isEnglishWord) {
            suggestJob = viewModelScope.launch {
                delay(300)
                fetchSuggestions(query, prefix = true)
            }
        }
    }

    /**
     * One entry point for every kind of input.
     *
     * The server decides what the query is — a word, a typo, an inflected form, a phrase, a
     * sentence, or Russian — so the client no longer sniffs for Cyrillic or guesses at intent.
     */
    /**
     * @param exact skip the server's resolver entirely — see [searchOriginalQuery].
     */
    fun searchWord(exact: Boolean = false) {
        val query = _state.value.searchQuery.trim()
        if (query.isEmpty()) {
            _state.value = _state.value.copy(error = "Пожалуйста, введите слово для поиска")
            return
        }

        Log.d(TAG, "Looking up: $query")
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                error = null,
                notice = null,
                wordData = null,
                entry = null,
                annotationPending = false,
                annotationDegraded = false,
                hasSearched = false,
                suggestions = emptyList(),
                isRussianSearch = false,
                russianQuery = "",
                ruEnCandidates = emptyList(),
                ruEnNote = null,
                ruEnAmbiguous = false,
                sentenceText = "",
                sentenceTokens = emptyList(),
                selectedTokenIndex = null,
                contextAnalysis = null
            )

            // Collected rather than awaited: a cold word streams an immediate raw response and
            // then the finished article, so each emission replaces the last on screen.
            searchRepository.lookup(query, exact = exact).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val data = result.data ?: return@collect
                        _state.value = _state.value.copy(
                            isLoading = false,
                            hasSearched = true,
                            error = null,
                            notice = data.notice,
                            entry = data.entry,
                            annotationPending = data.annotationStatus == "PENDING",
                            annotationDegraded = data.annotationStatus == "DEGRADED",
                            wordData = data.raw?.toWordDto(),
                            // A sentence has no headword — its words become tappable instead.
                            sentenceText = data.tokenized?.text.orEmpty(),
                            sentenceTokens = data.tokenized?.tokens.orEmpty(),
                            isRussianSearch = data.ruEn != null,
                            russianQuery = if (data.ruEn != null) query else "",
                            ruEnCandidates = data.ruEn?.candidates.orEmpty(),
                            ruEnNote = data.ruEn?.note,
                            ruEnAmbiguous = data.ruEn?.isAmbiguous ?: false
                        )

                        data.entry?.lemma?.takeIf { it.isNotBlank() }?.let { checkIfWordIsSaved(it) }

                        // Nothing to show at all — offer alternatives rather than a bare error.
                        val empty = data.entry == null && data.raw == null &&
                            data.ruEn == null && data.tokenized == null
                        if (empty) {
                            _state.value = _state.value.copy(error = "Слово не найдено")
                            fetchSuggestions(query, prefix = false)
                        }
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "Lookup error: ${result.message}")
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = result.message,
                            hasSearched = true
                        )
                        fetchSuggestions(query, prefix = false)
                    }
                    is Resource.Loading -> _state.value = _state.value.copy(isLoading = true)
                }
            }

            // Stop the spinner if the article never landed; the sources view stays usable.
            if (_state.value.annotationPending) {
                _state.value = _state.value.copy(annotationPending = false)
            }
        }
    }

    /**
     * Asks what one word means in the sentence the user pasted.
     * The index refers to the server's own tokenisation, which shipped with the lookup.
     */
    fun analyzeToken(index: Int) {
        val text = _state.value.sentenceText
        if (text.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(
                selectedTokenIndex = index,
                isAnalyzingContext = true,
                contextAnalysis = null
            )
            when (val result = searchRepository.analyzeInContext(text, index)) {
                is Resource.Success -> _state.value = _state.value.copy(
                    contextAnalysis = result.data, isAnalyzingContext = false
                )
                is Resource.Error -> _state.value = _state.value.copy(
                    isAnalyzingContext = false, error = result.message
                )
                is Resource.Loading -> Unit
            }
        }
    }

    fun dismissContextAnalysis() {
        _state.value = _state.value.copy(selectedTokenIndex = null, contextAnalysis = null)
    }

    fun selectSuggestion(suggestion: String) {
        _state.value = _state.value.copy(
            searchQuery = suggestion,
            suggestions = emptyList(),
            isRussianSearch = false,
            russianQuery = "",
            ruEnCandidates = emptyList()
        )
        searchWord()
    }

    /**
     * Re-runs the search for exactly what was typed, overriding whatever the resolver decided.
     *
     * Repeating the query was not enough: the same input took the same rungs and produced the
     * same answer, so «Искать точно» could never override anything. The override has to reach
     * the server, where the decision is actually made.
     */
    fun searchOriginalQuery(original: String) {
        _state.value = _state.value.copy(searchQuery = original, notice = null)
        searchWord(exact = true)
    }

    private fun fetchSuggestions(query: String, prefix: Boolean = false) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isFetchingSuggestions = true)
            val suggestions = searchRepository.getSuggestions(query, prefix = prefix)
            _state.value = _state.value.copy(
                suggestions = suggestions,
                isFetchingSuggestions = false
            )
        }
    }

    fun clearSearch() {
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

    /**
     * Saves the word, preferring the annotated article.
     *
     * Its Russian is written per sense, so the saved word and the flashcard made from it carry a
     * translation that actually matches the definition sitting next to it.
     */
    fun saveWord() {
        val entry = _state.value.entry
        val wordData = _state.value.wordData
        val word = entry?.lemma?.takeIf { it.isNotBlank() } ?: wordData?.word ?: return

        val firstSense = entry?.posGroups?.firstOrNull()?.senses?.firstOrNull()
        val firstDefinition = wordData?.definitions?.firstOrNull()

        val translation = firstSense?.translationsRu?.firstOrNull() ?: wordData?.translation
        val definition = firstSense?.definitionEn ?: firstDefinition?.definition
        val example = firstSense?.examples?.firstOrNull()?.en ?: firstDefinition?.example
        val partOfSpeech = entry?.posGroups?.firstOrNull()?.pos ?: firstDefinition?.partOfSpeech

        viewModelScope.launch {
            when (savedWordsRepository.saveWord(word, translation, definition)) {
                is Resource.Success -> {
                    _isSaved.value = true
                    if (definition != null) {
                        flashcardRepository.createFlashcard(
                            word = word,
                            definition = definition,
                            example = example,
                            translation = translation,
                            phonetic = entry?.phonetic ?: wordData?.phonetic,
                            partOfSpeech = partOfSpeech
                        )
                    }
                }
                is Resource.Error -> Log.e(TAG, "Failed to save word")
                else -> {}
            }
        }
    }

    fun unsaveWord() {
        val word = _state.value.entry?.lemma?.takeIf { it.isNotBlank() }
            ?: _state.value.wordData?.word ?: return
        viewModelScope.launch {
            when (savedWordsRepository.deleteWord(word)) {
                is Resource.Success -> _isSaved.value = false
                is Resource.Error -> Log.e(TAG, "Failed to remove word")
                else -> {}
            }
        }
    }

    private fun checkIfWordIsSaved(word: String) {
        viewModelScope.launch {
            _isSaved.value = savedWordsRepository.isWordSaved(word)
        }
    }
}

/**
 * Adapts the v2 raw aggregate to the shape the existing sources view renders.
 *
 * The sources tabs were built against the legacy search response; converting here keeps that
 * whole rendering path untouched while the article becomes the primary view.
 */
private fun WordDetailResponse.toWordDto(): WordDto = WordDto(
    word = word,
    phonetic = phonetic,
    audioUrl = audioUrl,
    // Same three fields, two declarations — the DTO split predates this screen.
    pronunciations = pronunciations.map { PronunciationDto(it.region, it.ipa, it.audioMp3Url) },
    translation = translation,
    definitions = definitions.map { def ->
        DefinitionDto(
            partOfSpeech = def.partOfSpeech,
            definition = def.definition,
            example = def.example,
            synonyms = synonyms.take(5),
            antonyms = antonyms.take(5),
            source = def.source
        )
    }
)
