package com.wordwaverise.wordwaveriseapp.presentation.tasks

import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.wordwaverise.wordwaveriseapp.data.local.entity.FlashcardEntity
import com.wordwaverise.wordwaveriseapp.data.remote.dto.exercise.ExerciseKind
import com.wordwaverise.wordwaveriseapp.data.remote.dto.exercise.ExerciseRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.exercise.ExerciseScope
import com.wordwaverise.wordwaveriseapp.data.repository.CategoryRepository
import com.wordwaverise.wordwaveriseapp.data.repository.ExerciseRepository
import com.wordwaverise.wordwaveriseapp.data.repository.FlashcardRepository
import com.wordwaverise.wordwaveriseapp.util.ExerciseGrading
import com.wordwaverise.wordwaveriseapp.util.ExerciseVerdict
import com.wordwaverise.wordwaveriseapp.util.Resource
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val flashcardRepository: FlashcardRepository,
    private val exerciseRepository: ExerciseRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TasksState())
    val state: StateFlow<TasksState> = _state.asStateFlow()

    /** The counters watch one folder at a time; switching folders replaces the subscription. */
    private var countsJob: Job? = null

    /** One clip at a time, and never one that outlives the screen. */
    private var mediaPlayer: MediaPlayer? = null

    init {
        viewModelScope.launch {
            flashcardRepository.syncFromServer()
            categoryRepository.syncCategories()
        }
        observeFolders()
        observeCounts(null)
    }

    // ── Folders ──────────────────────────────────────────────────────────────

    private fun observeFolders() {
        viewModelScope.launch {
            categoryRepository.categories.collect { categories ->
                // Only folders the server knows can filter a server-built session; a folder
                // created offline has no id the API would recognise yet.
                val synced = categories.mapNotNull { category ->
                    category.serverId?.let { FolderOption(it, category.name) }
                }
                _state.update { current ->
                    val options = buildList {
                        add(FolderOption(null, "Все"))
                        addAll(synced)
                        add(FolderOption(FolderOption.UNCATEGORIZED, "Без папки"))
                    }
                    // A folder that disappeared must not leave the screen filtering by nothing.
                    val stillThere = options.any { it.id == current.selectedFolder }
                    current.copy(
                        folders = options,
                        selectedFolder = if (stillThere) current.selectedFolder else null
                    )
                }
            }
        }
    }

    fun selectFolder(folderId: Int?) {
        if (_state.value.selectedFolder == folderId) return
        _state.update { it.copy(selectedFolder = folderId, bulkMessage = null) }
        observeCounts(folderId)
        loadKinds()
    }

    private fun observeCounts(folderId: Int?) {
        countsJob?.cancel()
        countsJob = viewModelScope.launch {
            combine(
                flashcardRepository.dueCountIn(folderId),
                flashcardRepository.totalCountIn(folderId)
            ) { due, total -> due to total }
                .collect { (due, total) ->
                    _state.update { it.copy(dueCount = due, totalCount = total) }
                }
        }
    }

    // ── Flashcard session ────────────────────────────────────────────────────

    fun startSession() {
        viewModelScope.launch {
            val folder = _state.value.selectedFolder
            val cards = flashcardRepository.getFlashcardsForSession(folder, 10).firstOrNull()
            if (cards.isNullOrEmpty()) return@launch
            _state.update {
                it.copy(
                    mode = TasksMode.CARD_SESSION,
                    sessionFlashcards = cards,
                    currentCardIndex = 0
                )
            }
        }
    }

    /** Те же три оценки, что и в браузере: иначе одна колода расходится между устройствами. */
    fun markCorrect() = reviewCurrentCard(CardVerdict.EASY)

    fun markHard() = reviewCurrentCard(CardVerdict.HARD)

    fun markIncorrect() = reviewCurrentCard(CardVerdict.AGAIN)

    private enum class CardVerdict { AGAIN, HARD, EASY }

    private fun reviewCurrentCard(verdict: CardVerdict) {
        viewModelScope.launch {
            val current = _state.value
            val card = current.sessionFlashcards.getOrNull(current.currentCardIndex) ?: return@launch
            when (verdict) {
                CardVerdict.AGAIN -> flashcardRepository.markAsIncorrect(card)
                CardVerdict.HARD -> flashcardRepository.markAsAlmost(card)
                CardVerdict.EASY -> flashcardRepository.markAsEasy(card)
            }
            _state.update { it.copy(currentCardIndex = it.currentCardIndex + 1) }
        }
    }

    fun exitSession() {
        _state.update {
            it.copy(
                mode = TasksMode.OVERVIEW,
                sessionFlashcards = emptyList(),
                currentCardIndex = 0
            )
        }
    }

    // ── Editing a card ───────────────────────────────────────────────────────

    fun editCard(card: FlashcardEntity) {
        _state.update { it.copy(editingCard = card) }
    }

    /** Edits the card the session is currently showing, which is where a mistake is noticed. */
    fun editCurrentCard() {
        val current = _state.value
        current.sessionFlashcards.getOrNull(current.currentCardIndex)?.let { editCard(it) }
    }

    fun dismissEditor() {
        _state.update { it.copy(editingCard = null) }
    }

    fun saveCard(word: String, translation: String, definition: String, example: String) {
        val card = _state.value.editingCard ?: return
        viewModelScope.launch {
            val result = flashcardRepository.updateContent(
                card = card,
                word = word.trim().ifBlank { card.word },
                translation = translation.trim().ifBlank { null },
                definition = definition.trim(),
                example = example.trim().ifBlank { null }
            )
            if (result is Resource.Error) {
                _state.update { it.copy(error = result.message, editingCard = null) }
                return@launch
            }
            // The open session holds its own copy of the card, so it has to be told.
            _state.update { current ->
                current.copy(
                    editingCard = null,
                    sessionFlashcards = current.sessionFlashcards.map { existing ->
                        if (existing.id != card.id) existing
                        else existing.copy(
                            word = word.trim().ifBlank { existing.word },
                            translation = translation.trim().ifBlank { null },
                            definition = definition.trim(),
                            example = example.trim().ifBlank { null },
                            customized = true
                        )
                    }
                )
            }
        }
    }

    // ── Filling a folder with cards ──────────────────────────────────────────

    fun createCardsFromFolder() {
        viewModelScope.launch {
            _state.update { it.copy(isBulkCreating = true, bulkMessage = null) }
            when (val result = flashcardRepository.createFromFolder(_state.value.selectedFolder)) {
                is Resource.Success -> {
                    val created = result.data?.created ?: 0
                    val moved = result.data?.moved ?: 0
                    val skipped = result.data?.skipped ?: 0
                    // Перенос называется переносом: карточка не появилась заново, её история
                    // повторений цела, и «создано» читалось бы как потеря прогресса.
                    val done = buildList {
                        if (created > 0) add("создано $created")
                        if (moved > 0) add("перенесено в папку $moved")
                    }
                    _state.update {
                        it.copy(
                            isBulkCreating = false,
                            bulkMessage = when {
                                done.isNotEmpty() -> "Готово: ${done.joinToString(", ")}"
                                skipped > 0 -> "Карточки для этих слов уже есть"
                                else -> "В этой папке нет сохранённых слов"
                            }
                        )
                    }
                }
                else -> _state.update {
                    it.copy(isBulkCreating = false, bulkMessage = result.message ?: "Не удалось создать карточки")
                }
            }
            loadKinds()
        }
    }

    // ── Exercise setup ───────────────────────────────────────────────────────

    fun openExerciseSetup() {
        _state.update { it.copy(mode = TasksMode.EXERCISE_SETUP, error = null) }
        loadKinds()
    }

    fun setScope(scope: ExerciseScope) {
        if (_state.value.scope == scope) return
        _state.update { it.copy(scope = scope) }
        loadKinds()
    }

    fun setCount(count: Int) {
        _state.update { it.copy(count = count) }
    }

    fun toggleKind(kind: ExerciseKind) {
        _state.update { current ->
            val next = current.selectedKinds.toMutableSet()
            if (!next.remove(kind)) next.add(kind)
            current.copy(selectedKinds = next)
        }
    }

    fun toggleAllKinds() {
        _state.update { current ->
            val all = current.availableKinds.map { it.kind }.toSet()
            current.copy(selectedKinds = if (current.selectedKinds == all) emptySet() else all)
        }
    }

    private fun loadKinds() {
        viewModelScope.launch {
            _state.update { it.copy(isKindsLoading = true) }
            val current = _state.value
            when (val result = exerciseRepository.kinds(current.selectedFolder, current.scope)) {
                is Resource.Success -> {
                    val kinds = result.data?.kinds.orEmpty()
                    _state.update {
                        it.copy(
                            isKindsLoading = false,
                            kinds = kinds,
                            wordsAvailable = result.data?.wordsAvailable ?: 0,
                            // Everything the selection supports is on by default: a mixed
                            // session is the point, and a picker that starts empty makes the
                            // learner configure before they can practise.
                            selectedKinds = kinds.filter { info -> info.available > 0 }
                                .map { info -> info.kind }.toSet(),
                            error = null
                        )
                    }
                }
                else -> _state.update {
                    it.copy(isKindsLoading = false, kinds = emptyList(), error = result.message)
                }
            }
        }
    }

    // ── Exercise session ─────────────────────────────────────────────────────

    fun startExercises() {
        viewModelScope.launch {
            val current = _state.value
            _state.update { it.copy(isExerciseLoading = true, error = null) }

            val result = exerciseRepository.generate(
                ExerciseRequest(
                    categoryId = current.selectedFolder,
                    scope = current.scope,
                    kinds = current.selectedKinds.toList(),
                    count = current.count
                )
            )

            when (result) {
                is Resource.Success -> {
                    val batch = result.data
                    val exercises = batch?.exercises.orEmpty()
                    _state.update {
                        it.copy(
                            isExerciseLoading = false,
                            exercises = exercises,
                            exerciseIndex = 0,
                            results = emptyList(),
                            verdict = null,
                            given = "",
                            typed = "",
                            notice = batch?.noticeRu,
                            wordsAvailable = batch?.wordsAvailable ?: it.wordsAvailable,
                            mode = if (exercises.isEmpty()) TasksMode.EXERCISE_SETUP
                                   else TasksMode.EXERCISE_SESSION
                        )
                    }
                    playIfListening()
                }
                else -> _state.update {
                    it.copy(isExerciseLoading = false, error = result.message)
                }
            }
        }
    }

    // ── Listening ────────────────────────────────────────────────────────────

    /**
     * Plays the recording the exercise carries.
     *
     * Called on its own when a listening question arrives: the question *is* the sound, and
     * making the learner press a button before they can hear it adds a step to every single
     * question of that kind. The control stays on screen for replays.
     */
    fun playAudio() {
        playUrl(_state.value.currentExercise?.audioUrl ?: return)
    }

    /** The recording on the front of a flashcard, played on request rather than on arrival. */
    fun playCardAudio() {
        val card = _state.value.sessionFlashcards.getOrNull(_state.value.currentCardIndex) ?: return
        playUrl(card.audioUrl ?: return)
    }

    private fun playUrl(url: String) {
        stopAudio()
        try {
            val player = MediaPlayer()
            mediaPlayer = player
            player.setDataSource(url)
            player.setOnPreparedListener { it.start() }
            player.setOnCompletionListener { _state.update { s -> s.copy(isAudioPlaying = false) } }
            player.setOnErrorListener { _, _, _ ->
                // A recording that will not load must not look like one that is still playing.
                _state.update { s -> s.copy(isAudioPlaying = false) }
                true
            }
            _state.update { it.copy(isAudioPlaying = true) }
            player.prepareAsync()
        } catch (e: Exception) {
            Log.w("TasksViewModel", "Audio playback failed: ${e.message}")
            _state.update { it.copy(isAudioPlaying = false) }
        }
    }

    private fun stopAudio() {
        runCatching { mediaPlayer?.let { if (it.isPlaying) it.stop() } }
        mediaPlayer?.release()
        mediaPlayer = null
        _state.update { it.copy(isAudioPlaying = false) }
    }

    /** Every route into a new question passes through here, so nothing is left silent. */
    private fun playIfListening() {
        if (_state.value.currentExercise?.audioUrl != null) playAudio()
    }

    override fun onCleared() {
        super.onCleared()
        stopAudio()
    }

    fun onTypedChange(value: String) {
        _state.update { it.copy(typed = value) }
    }

    fun submitTyped() {
        val current = _state.value
        if (current.typed.isBlank()) return
        answer(current.typed)
    }

    fun chooseOption(index: Int) {
        val exercise = _state.value.currentExercise ?: return
        answer(exercise.options.getOrNull(index).orEmpty())
    }

    private fun answer(userAnswer: String) {
        val current = _state.value
        val exercise = current.currentExercise ?: return
        if (current.answered) return

        val verdict = ExerciseGrading.grade(exercise, userAnswer)
        _state.update {
            it.copy(
                verdict = verdict,
                given = userAnswer,
                results = it.results + ExerciseResult(exercise, verdict, userAnswer)
            )
        }

        exercise.cardId?.let { cardId ->
            viewModelScope.launch { flashcardRepository.recordPracticeResult(cardId, verdict) }
        }
    }

    fun nextExercise() {
        stopAudio()
        _state.update { current ->
            val nextIndex = current.exerciseIndex + 1
            current.copy(
                exerciseIndex = nextIndex,
                verdict = null,
                given = "",
                typed = "",
                mode = if (nextIndex >= current.exercises.size) TasksMode.EXERCISE_RESULT
                       else current.mode
            )
        }
        playIfListening()
    }

    /** Skipping is not failing, but it is not knowing either — the question comes back later. */
    fun skipExercise() {
        stopAudio()
        _state.update { current ->
            val exercise = current.currentExercise ?: return@update current
            val rest = current.exercises.toMutableList()
            rest.removeAt(current.exerciseIndex)
            rest.add(exercise)
            current.copy(exercises = rest, verdict = null, given = "", typed = "")
        }
        playIfListening()
    }

    fun exitExercises() {
        stopAudio()
        _state.update {
            it.copy(
                mode = TasksMode.OVERVIEW,
                exercises = emptyList(),
                exerciseIndex = 0,
                results = emptyList(),
                verdict = null,
                given = "",
                typed = ""
            )
        }
    }

    fun backToSetup() {
        _state.update { it.copy(mode = TasksMode.EXERCISE_SETUP) }
        loadKinds()
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    /** Used by the result screen to show only what is worth looking at again. */
    fun mistakes(): List<ExerciseResult> =
        _state.value.results.filter { it.verdict != ExerciseVerdict.CORRECT }
}
