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
        // Слово из сохранённых открывается ровно тем, чем его сохранили: резолвер по нему
        // второй раз не ходит, иначе выбранная форма каждый раз уезжала бы на лемму.
        val exact = savedStateHandle.get<Boolean>("exact") ?: false
        if (word != null) {
            _state.update { it.copy(word = word) }
            loadWord(word, exact)
            checkIfWordIsSaved(word)
        }
    }

    /**
     * One request for the whole screen.
     *
     * `/api/v2/words/lookup` returns the annotated article *and* the raw
     * multi-source aggregate that the per-dictionary tabs are built from, so
     * the old `/api/words/details` pair (quick, then full) is redundant — it
     * used to cost two extra round trips to fetch a second copy of the same
     * aggregate through an older endpoint.
     *
     * Collected rather than awaited: a cold word answers with raw data first
     * and the finished article once the server has written it, so each
     * emission replaces the last on screen.
     */
    private fun loadWord(word: String, exact: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            var gotAnything = false

            searchRepository.lookup(word, exact = exact).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val data = result.data ?: return@collect
                        gotAnything = true
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = null,
                                // A later emission may carry only the article, so
                                // the aggregate is kept rather than overwritten
                                // with null.
                                wordDetail = data.raw ?: it.wordDetail,
                                entry = data.entry ?: it.entry,
                                annotationPending = data.annotationStatus == "PENDING",
                                annotationDegraded = data.annotationStatus == "DEGRADED"
                            )
                        }
                    }
                    is Resource.Error -> {
                        // Only an error if nothing has landed yet: the poll for a
                        // finished article must not wipe definitions already shown.
                        if (!gotAnything) {
                            _state.update {
                                it.copy(isLoading = false, error = result.message ?: "Слово не найдено")
                            }
                        }
                    }
                    is Resource.Loading -> _state.update { it.copy(isLoading = true) }
                }
            }

            _state.update { it.copy(isLoading = false, annotationPending = false) }
        }
    }

    private fun checkIfWordIsSaved(word: String) {
        viewModelScope.launch {
            try {
                val token = authRepository.token.firstOrNull()
                if (token != null) {
                    val savedWords = apiService.getSavedWords("Bearer $token")
                    val saved = savedWords.data?.words
                        ?.firstOrNull { it.word.equals(word, ignoreCase = true) }
                    _state.update {
                        it.copy(
                            isSaved = saved != null,
                            pinnedSenseId = saved?.senseId,
                            isSavedLoading = false
                        )
                    }
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

    /**
     * The bookmark on one sense of the article.
     *
     * Tapping the sense already chosen removes the word from saved altogether — "saved, but no
     * sense" is a state with nothing to show. Tapping a different one re-pins it, which the
     * server treats as a change of meaning rather than a second save, so the folder and the
     * card survive.
     */
    fun toggleSense(senseId: String) {
        if (_state.value.pinnedSenseId == senseId) {
            unsaveWord()
            return
        }

        val entry = _state.value.entry ?: return
        val sense = entry.posGroups.flatMap { it.senses }.firstOrNull { it.id == senseId } ?: return

        viewModelScope.launch {
            try {
                val token = authRepository.token.firstOrNull() ?: return@launch
                apiService.saveWord(
                    token = "Bearer $token",
                    request = SaveWordRequest(
                        word = _state.value.word,
                        // Запасной вариант на случай слова, статью которого сервер ещё не
                        // написал: когда корпус значение знает, побеждает его собственный текст.
                        translation = sense.translationsRu.firstOrNull(),
                        definition = sense.definitionEn.takeIf { it.isNotBlank() }
                            ?: sense.definitionRu.takeIf { it.isNotBlank() },
                        senseId = senseId
                    )
                )
                _state.update { it.copy(isSaved = true, pinnedSenseId = senseId) }
            } catch (_: Exception) { }
        }
    }

    fun unsaveWord() {
        viewModelScope.launch {
            try {
                val token = authRepository.token.firstOrNull()
                if (token != null) {
                    apiService.deleteSavedWord(token = "Bearer $token", word = _state.value.word)
                    _state.update { it.copy(isSaved = false, pinnedSenseId = null) }
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
