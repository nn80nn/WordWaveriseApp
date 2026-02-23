package n.startapp.wordwaveriseapp.presentation.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import n.startapp.wordwaveriseapp.data.repository.AiRepository
import n.startapp.wordwaveriseapp.data.repository.FlashcardRepository
import n.startapp.wordwaveriseapp.util.Resource
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val flashcardRepository: FlashcardRepository,
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TasksState())
    val state: StateFlow<TasksState> = _state.asStateFlow()

    init {
        syncFromServer()
        observeFlashcards()
    }

    private fun syncFromServer() {
        viewModelScope.launch {
            flashcardRepository.syncFromServer()
        }
    }

    private fun observeFlashcards() {
        viewModelScope.launch {
            combine(
                flashcardRepository.dueCount,
                flashcardRepository.totalCount
            ) { due, total ->
                Pair(due, total)
            }.collect { (due, total) ->
                _state.update { currentState ->
                    currentState.copy(
                        dueCount = due,
                        totalCount = total
                    )
                }
            }
        }
    }

    fun startSession() {
        viewModelScope.launch {
            flashcardRepository.getFlashcardsForSession(10)
                .firstOrNull()
                ?.let { cards ->
                    if (cards.isNotEmpty()) {
                        _state.update { currentState ->
                            currentState.copy(
                                isSessionActive = true,
                                sessionFlashcards = cards,
                                currentCardIndex = 0
                            )
                        }
                    }
                }
        }
    }

    fun markCorrect() {
        viewModelScope.launch {
            val currentState = _state.value
            val currentCard = currentState.sessionFlashcards.getOrNull(currentState.currentCardIndex)

            if (currentCard != null) {
                flashcardRepository.markAsCorrect(currentCard)
                moveToNextCard()
            }
        }
    }

    fun markIncorrect() {
        viewModelScope.launch {
            val currentState = _state.value
            val currentCard = currentState.sessionFlashcards.getOrNull(currentState.currentCardIndex)

            if (currentCard != null) {
                flashcardRepository.markAsIncorrect(currentCard)
                moveToNextCard()
            }
        }
    }

    private fun moveToNextCard() {
        _state.update { currentState ->
            currentState.copy(
                currentCardIndex = currentState.currentCardIndex + 1
            )
        }
    }

    fun exitSession() {
        _state.update { currentState ->
            currentState.copy(
                isSessionActive = false,
                sessionFlashcards = emptyList(),
                currentCardIndex = 0
            )
        }
    }

    // ── AI Exercise mode ──────────────────────────────────────────────────────

    fun startExerciseMode() {
        _state.update { it.copy(isExerciseModeActive = true) }
        loadNextExercise()
    }

    fun exitExerciseMode() {
        _state.update {
            it.copy(
                isExerciseModeActive = false,
                exerciseSentence = null,
                exerciseAnswer = null,
                userAnswer = "",
                exerciseChecked = false,
                exerciseIsCorrect = false,
                exerciseError = null
            )
        }
    }

    fun onUserAnswerChange(answer: String) {
        _state.update { it.copy(userAnswer = answer) }
    }

    fun checkAnswer() {
        val state = _state.value
        val correct = state.userAnswer.trim().equals(state.exerciseAnswer?.trim(), ignoreCase = true)
        _state.update { it.copy(exerciseChecked = true, exerciseIsCorrect = correct) }
    }

    fun loadNextExercise() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isExerciseLoading = true,
                    exerciseSentence = null,
                    exerciseAnswer = null,
                    userAnswer = "",
                    exerciseChecked = false,
                    exerciseIsCorrect = false,
                    exerciseError = null
                )
            }
            val allCards = flashcardRepository.allFlashcards.firstOrNull() ?: emptyList()
            if (allCards.isEmpty()) {
                _state.update { it.copy(isExerciseLoading = false, exerciseError = "Нет сохранённых слов") }
                return@launch
            }
            val word = allCards.random().word
            when (val result = aiRepository.getExercise(word)) {
                is Resource.Success -> _state.update {
                    it.copy(
                        isExerciseLoading = false,
                        exerciseSentence = result.data?.sentence,
                        exerciseAnswer = result.data?.answer
                    )
                }
                is Resource.Error -> _state.update {
                    it.copy(isExerciseLoading = false, exerciseError = result.message)
                }
                else -> {}
            }
        }
    }
}
