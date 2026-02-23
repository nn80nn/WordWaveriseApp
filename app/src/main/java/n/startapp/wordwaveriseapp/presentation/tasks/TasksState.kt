package n.startapp.wordwaveriseapp.presentation.tasks

import n.startapp.wordwaveriseapp.data.local.entity.FlashcardEntity

data class TasksState(
    val dueCount: Int = 0,
    val totalCount: Int = 0,
    val isSessionActive: Boolean = false,
    val sessionFlashcards: List<FlashcardEntity> = emptyList(),
    val currentCardIndex: Int = 0,
    // AI Exercise mode
    val isExerciseModeActive: Boolean = false,
    val isExerciseLoading: Boolean = false,
    val exerciseSentence: String? = null,
    val exerciseAnswer: String? = null,
    val userAnswer: String = "",
    val exerciseChecked: Boolean = false,
    val exerciseIsCorrect: Boolean = false,
    val exerciseError: String? = null
)
