package com.wordwaverise.wordwaveriseapp.presentation.profile

data class ProfileState(
    val savedWordsCount: Int = 0,
    val totalFlashcards: Int = 0,
    val dueFlashcards: Int = 0,

    /** Классы, в которых человек состоит, и сколько заданий ещё не закрыто. */
    val groupCount: Int = 0,
    val openAssignments: Int = 0
)
