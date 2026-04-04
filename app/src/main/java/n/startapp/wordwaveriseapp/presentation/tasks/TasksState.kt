package n.startapp.wordwaveriseapp.presentation.tasks

import n.startapp.wordwaveriseapp.data.local.entity.FlashcardEntity

/** Question for multiple-choice exercise. */
data class MultipleChoiceQuestion(
    val questionText: String,   // what is shown as the question
    val options: List<String>,  // 4 answer options
    val correctIndex: Int,      // index of the correct option in [options]
    val wordFirst: Boolean      // true = "what does X mean?", false = "which word means X?"
)

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
    val exerciseError: String? = null,
    // Multiple Choice mode
    val isMultipleChoiceActive: Boolean = false,
    val multipleChoiceQuestion: MultipleChoiceQuestion? = null,
    val selectedChoiceIndex: Int? = null,   // null = not answered yet
    val choiceAnswered: Boolean = false
)
