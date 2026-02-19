package n.startapp.wordwaveriseapp.presentation.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import n.startapp.wordwaveriseapp.data.repository.FlashcardRepository
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val flashcardRepository: FlashcardRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TasksState())
    val state: StateFlow<TasksState> = _state.asStateFlow()

    init {
        observeFlashcards()
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
}
