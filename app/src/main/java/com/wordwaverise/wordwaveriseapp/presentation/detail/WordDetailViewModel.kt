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
import kotlinx.coroutines.flow.first
import com.wordwaverise.wordwaveriseapp.data.local.SettingsDataStore
import com.wordwaverise.wordwaveriseapp.data.repository.AiRepository
import com.wordwaverise.wordwaveriseapp.data.repository.CategoryRepository
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
    private val categoryRepository: CategoryRepository,
    private val settingsDataStore: SettingsDataStore,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(WordDetailState())
    val state: StateFlow<WordDetailState> = _state.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null

    init {
        // Папки нужны раньше первого сохранения: диалог, открывшийся пустым и заполнившийся
        // через полсекунды, читается как «папок нет».
        //
        // ⚠️ Только те, о которых знает сервер: этот экран сохраняет напрямую через API, и
        // папка, заведённая без сети, назвать себя серверу пока не может.
        viewModelScope.launch {
            categoryRepository.categories.collect { folders ->
                _state.update { state ->
                    state.copy(ownFolders = folders.filter { !it.readOnly && it.serverId != null })
                }
            }
        }

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
                    // Записей на одно написание может быть несколько — по одной на значение.
                    val saved = savedWords.data?.words
                        ?.filter { it.word.equals(word, ignoreCase = true) }
                        .orEmpty()
                    _state.update {
                        it.copy(
                            savedEntryIds = saved.associateBy({ e -> e.senseId }, { e -> e.id }),
                            pinnedSenseIds = saved.mapNotNull { e -> e.senseId }.toSet()
                        )
                    }
                }
            } catch (_: Exception) {
                // Список сохранённого — не то, ради чего человек открыл слово: статья важнее,
                // и без ответа она всё равно рисуется, просто без отметок на значениях.
            }
        }
    }

    /**
     * The bookmark on one sense of the article.
     *
     * Каждое значение сохраняется само по себе: отметили два определения — в словаре два
     * слова, со своим переводом, своим примером и своим расписанием повторения. Снятие
     * закладки убирает **только** эту запись; другие значения того же слова остаются, они не
     * про то, от чего человек сейчас отказался.
     */
    fun toggleSense(senseId: String) {
        // Снятие закладки ничего не спрашивает: убрать — это уже ответ.
        if (senseId in _state.value.pinnedSenseIds) {
            unsaveSense(senseId)
            return
        }

        val entry = _state.value.entry ?: return
        val sense = entry.posGroups.flatMap { it.senses }.firstOrNull { it.id == senseId } ?: return

        // Сохранение спрашивает, куда. Папки прошлого раза предлагаются отмеченными: слова
        // собирают подряд в один урок, и повторять один и тот же выбор двадцать раз — это не
        // выбор, а работа. ⚠️ Сверяется с текущим списком: папку могли удалить.
        viewModelScope.launch {
            val known = _state.value.ownFolders.map { it.id }.toSet()
            val remembered = settingsDataStore.lastSaveFolders.first().filter { it in known }
            _state.update {
                it.copy(
                    pendingSenseId = senseId,
                    pendingSenseSummary = sense.translationsRu.joinToString(", ")
                        .takeIf { text -> text.isNotBlank() }
                        ?: sense.definitionEn.takeIf { text -> text.isNotBlank() }
                        ?: sense.definitionRu.takeIf { text -> text.isNotBlank() },
                    chosenFolders = remembered
                )
            }
        }
    }

    /** Отметить папку в диалоге сохранения или снять отметку. Пусто — «без папки». */
    fun toggleSaveFolder(id: Long) {
        _state.update {
            it.copy(
                chosenFolders = if (id in it.chosenFolders) it.chosenFolders - id
                                else it.chosenFolders + id
            )
        }
    }

    /** «Без папки» — это выбор, а не его отсутствие: строка снимает все отметки. */
    fun clearSaveFolders() {
        _state.update { it.copy(chosenFolders = emptyList()) }
    }

    fun cancelSaveSense() {
        _state.update { it.copy(pendingSenseId = null, pendingSenseSummary = null) }
    }

    /** Заводит папку прямо из диалога и сразу её отмечает. */
    fun createFolderForSave(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val result = categoryRepository.createCategory(trimmed)
            val created = (result as? Resource.Success)?.data ?: return@launch
            _state.update { it.copy(chosenFolders = it.chosenFolders + created.id) }
        }
    }

    /**
     * Сохраняет значение в выбранные папки.
     *
     * Папки едут вместе с сохранением, а не вторым запросом: слово, которое сначала легло
     * «никуда», а потом переехало, при обрыве между этими двумя шагами остаётся не там, куда
     * его клали, — и человек об этом не узнаёт.
     */
    fun confirmSaveSense() {
        val senseId = _state.value.pendingSenseId ?: return
        val entry = _state.value.entry ?: return
        val sense = entry.posGroups.flatMap { it.senses }.firstOrNull { it.id == senseId } ?: return
        val chosen = _state.value.chosenFolders

        viewModelScope.launch {
            _state.update { it.copy(isSavingSense = true) }
            try {
                val token = authRepository.token.firstOrNull() ?: return@launch
                val serverIds = _state.value.ownFolders
                    .filter { it.id in chosen }
                    .mapNotNull { it.serverId }
                apiService.saveWord(
                    token = "Bearer $token",
                    request = SaveWordRequest(
                        word = _state.value.word,
                        // Запасной вариант на случай слова, статью которого сервер ещё не
                        // написал: когда корпус значение знает, побеждает его собственный текст.
                        translation = sense.translationsRu.firstOrNull(),
                        definition = sense.definitionEn.takeIf { it.isNotBlank() }
                            ?: sense.definitionRu.takeIf { it.isNotBlank() },
                        senseId = senseId,
                        // ⚠️ Пустой список не отправляется: сервер различает «никуда» и
                        // «в эти папки», и `[]` читалось бы как просьба вынуть слово.
                        categoryIds = serverIds.takeIf { it.isNotEmpty() }
                    )
                )
                settingsDataStore.setLastSaveFolders(chosen)
                _state.update {
                    it.copy(
                        pinnedSenseIds = it.pinnedSenseIds + senseId,
                        pendingSenseId = null,
                        pendingSenseSummary = null
                    )
                }
                // Ответ несёт id новой записи — без него снятие закладки не знало бы, какую
                // именно строку убирать, и убрало бы слово целиком.
                checkIfWordIsSaved(_state.value.word)
            } catch (_: Exception) {
            } finally {
                _state.update { it.copy(isSavingSense = false) }
            }
        }
    }

    /** Убирает одно значение. Остальные значения того же слова остаются. */
    private fun unsaveSense(senseId: String) {
        viewModelScope.launch {
            try {
                val token = authRepository.token.firstOrNull() ?: return@launch
                val entryId = _state.value.savedEntryIds[senseId]
                if (entryId != null) {
                    apiService.deleteSavedEntry(token = "Bearer $token", id = entryId)
                } else {
                    // Записи, о которой мы не знаем id, нет и на сервере под этим значением —
                    // остаётся только «слово целиком», что и было единственным вариантом раньше.
                    apiService.deleteSavedWord(token = "Bearer $token", word = _state.value.word)
                }
                _state.update {
                    it.copy(
                        pinnedSenseIds = it.pinnedSenseIds - senseId,
                        savedEntryIds = it.savedEntryIds - senseId
                    )
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
