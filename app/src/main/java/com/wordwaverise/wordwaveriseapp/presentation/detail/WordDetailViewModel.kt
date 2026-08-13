package com.wordwaverise.wordwaveriseapp.presentation.detail

import android.media.MediaPlayer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.wordwaverise.wordwaveriseapp.data.repository.AiRepository
import com.wordwaverise.wordwaveriseapp.data.repository.AuthRepository
import com.wordwaverise.wordwaveriseapp.data.repository.SearchRepository
import com.wordwaverise.wordwaveriseapp.data.remote.ApiService
import com.wordwaverise.wordwaveriseapp.data.remote.dto.saved.SaveWordRequest
import com.wordwaverise.wordwaveriseapp.util.NetworkError
import com.wordwaverise.wordwaveriseapp.util.Resource
import javax.inject.Inject

@HiltViewModel
class WordDetailViewModel @Inject constructor(
    private val apiService: ApiService,
    private val authRepository: AuthRepository,
    private val aiRepository: AiRepository,
    private val searchRepository: SearchRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(WordDetailState())
    val state: StateFlow<WordDetailState> = _state.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null

    init {
        val word = savedStateHandle.get<String>("word")
        if (word != null) {
            _state.update { it.copy(word = word) }
            loadArticle(word)
            loadWordDetail(word)
            checkIfWordIsSaved(word)
        }
    }

    /**
     * The annotated article, on the same v2 endpoint the search screen uses.
     *
     * Opening a saved word used to land on the raw source aggregate while the
     * identical word opened from search showed the article — two front doors
     * onto the same entry. This is the same door.
     *
     * Collected rather than awaited: a cold word answers with raw data first and
     * the finished article a moment later, so each emission replaces the last.
     */
    private fun loadArticle(word: String) {
        viewModelScope.launch {
            searchRepository.lookup(word).collect { result ->
                if (result is Resource.Success) {
                    val data = result.data ?: return@collect
                    _state.update {
                        it.copy(
                            entry = data.entry ?: it.entry,
                            annotationPending = data.annotationStatus == "PENDING",
                            annotationDegraded = data.annotationStatus == "DEGRADED"
                        )
                    }
                }
                // A failed lookup is not an error for this screen: the source
                // tabs are loaded separately and stay usable on their own.
            }
            _state.update { it.copy(annotationPending = false) }
        }
    }

    private fun loadWordDetail(word: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Phase 1: quick load (API only, ~1-2s) — show data immediately
            val quickLoaded = try {
                val response = apiService.getWordDetails(word, quick = true)
                if (response.status == "ok" && response.data != null) {
                    _state.update { it.copy(wordDetail = response.data, isLoading = false, isLoadingFull = true) }
                    true
                } else {
                    false
                }
            } catch (_: Exception) { false }

            // Phase 2: full load (with scrapers, ~5-10s) — update data in background
            try {
                val response = apiService.getWordDetails(word, quick = false)
                if (response.status == "ok" && response.data != null) {
                    _state.update { it.copy(wordDetail = response.data, isLoading = false, isLoadingFull = false) }
                } else if (!quickLoaded) {
                    _state.update { it.copy(error = response.message ?: "Слово не найдено", isLoading = false, isLoadingFull = false) }
                } else {
                    _state.update { it.copy(isLoadingFull = false) }
                }
            } catch (e: Exception) {
                if (!quickLoaded) {
                    _state.update { it.copy(error = NetworkError.getErrorMessage(e), isLoading = false, isLoadingFull = false) }
                } else {
                    _state.update { it.copy(isLoadingFull = false) }
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
                    val isSaved = savedWords.data?.words?.any { it.word.equals(word, ignoreCase = true) } == true
                    _state.update { it.copy(isSaved = isSaved, isSavedLoading = false) }
                } else {
                    _state.update { it.copy(isSavedLoading = false) }
                }
            } catch (_: Exception) {
                _state.update { it.copy(isSavedLoading = false) }
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

    // ── Audio playback ────────────────────────────────────────────────────────

    /** Toggle play/stop for the given URL. */
    fun playAudio(url: String) {
        if (_state.value.playingAudioUrl == url && _state.value.isPlayingAudio) {
            stopAudio()
            return
        }
        viewModelScope.launch(Dispatchers.Main) {
            try {
                _state.update { it.copy(isPlayingAudio = true, playingAudioUrl = url, audioError = null) }
                mediaPlayer?.release()
                mediaPlayer = null
                val mp = MediaPlayer()
                mediaPlayer = mp
                mp.setDataSource(url)
                mp.setOnPreparedListener { it.start() }
                mp.setOnCompletionListener {
                    _state.update { it.copy(isPlayingAudio = false, playingAudioUrl = null) }
                }
                mp.setOnErrorListener { _, _, _ ->
                    _state.update {
                        it.copy(isPlayingAudio = false, playingAudioUrl = null,
                            audioError = "Ошибка воспроизведения")
                    }
                    true
                }
                mp.prepareAsync()
            } catch (e: Exception) {
                _state.update {
                    it.copy(isPlayingAudio = false, playingAudioUrl = null,
                        audioError = "Не удалось загрузить аудио")
                }
            }
        }
    }

    fun stopAudio() {
        try {
            mediaPlayer?.let { if (it.isPlaying) it.stop() }
        } catch (_: Exception) { }
        mediaPlayer?.release()
        mediaPlayer = null
        _state.update { it.copy(isPlayingAudio = false, playingAudioUrl = null) }
    }

    // ── AI features ───────────────────────────────────────────────────────────

    fun loadAiExplanation() {
        val word = _state.value.word.ifBlank { return }
        if (_state.value.isAiExplanationLoading || _state.value.aiExplanation != null) return
        viewModelScope.launch {
            _state.update { it.copy(isAiExplanationLoading = true, aiError = null) }
            when (val result = aiRepository.explainWord(word)) {
                is Resource.Success -> _state.update {
                    it.copy(aiExplanation = result.data, isAiExplanationLoading = false)
                }
                is Resource.Error -> _state.update {
                    it.copy(aiError = result.message, isAiExplanationLoading = false)
                }
                else -> {}
            }
        }
    }

    fun loadAiExamples() {
        val word = _state.value.word.ifBlank { return }
        if (_state.value.isAiExamplesLoading || _state.value.aiExamples != null) return
        viewModelScope.launch {
            _state.update { it.copy(isAiExamplesLoading = true, aiError = null) }
            when (val result = aiRepository.getExamples(word)) {
                is Resource.Success -> _state.update {
                    it.copy(aiExamples = result.data, isAiExamplesLoading = false)
                }
                is Resource.Error -> _state.update {
                    it.copy(aiError = result.message, isAiExamplesLoading = false)
                }
                else -> {}
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
