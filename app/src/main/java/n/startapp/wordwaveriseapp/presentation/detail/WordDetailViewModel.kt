package n.startapp.wordwaveriseapp.presentation.detail

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
import n.startapp.wordwaveriseapp.data.repository.AiRepository
import n.startapp.wordwaveriseapp.data.repository.AuthRepository
import n.startapp.wordwaveriseapp.data.remote.ApiService
import n.startapp.wordwaveriseapp.data.remote.dto.saved.SaveWordRequest
import n.startapp.wordwaveriseapp.util.NetworkError
import n.startapp.wordwaveriseapp.util.Resource
import javax.inject.Inject

@HiltViewModel
class WordDetailViewModel @Inject constructor(
    private val apiService: ApiService,
    private val authRepository: AuthRepository,
    private val aiRepository: AiRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(WordDetailState())
    val state: StateFlow<WordDetailState> = _state.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null

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
