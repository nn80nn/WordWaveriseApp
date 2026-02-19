package n.startapp.wordwaveriseapp.presentation.tasks

import n.startapp.wordwaveriseapp.data.local.entity.FlashcardEntity

data class TasksState(
    val dueCount: Int = 0,
    val totalCount: Int = 0,
    val isSessionActive: Boolean = false,
    val sessionFlashcards: List<FlashcardEntity> = emptyList(),
    val currentCardIndex: Int = 0
)
