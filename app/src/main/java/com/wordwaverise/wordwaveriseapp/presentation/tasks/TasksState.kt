package com.wordwaverise.wordwaveriseapp.presentation.tasks

import com.wordwaverise.wordwaveriseapp.data.local.entity.FlashcardEntity
import com.wordwaverise.wordwaveriseapp.data.remote.dto.exercise.ExerciseDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.exercise.ExerciseKind
import com.wordwaverise.wordwaveriseapp.data.remote.dto.exercise.ExerciseKindInfoDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.exercise.ExerciseScope
import com.wordwaverise.wordwaveriseapp.util.ExerciseVerdict

/** Which part of the tab is on screen. One value, so two of them can never both be true. */
enum class TasksMode { OVERVIEW, CARD_SESSION, EXERCISE_SETUP, EXERCISE_SESSION, EXERCISE_RESULT }

/**
 * A folder as the picker offers it. [id] is the **server** id — the same number the API takes,
 * and the same one `CategoryEntity.serverId` carries — with `null` meaning every folder and
 * [UNCATEGORIZED] meaning the words filed in none.
 */
data class FolderOption(val id: Int?, val name: String) {
    companion object {
        const val UNCATEGORIZED = -1
    }
}

data class ExerciseResult(
    val exercise: ExerciseDto,
    val verdict: ExerciseVerdict,
    /** What the learner picked or typed, for the review at the end. */
    val given: String
)

data class TasksState(
    val mode: TasksMode = TasksMode.OVERVIEW,

    // ── Folder ────────────────────────────────────────────────────────────
    val folders: List<FolderOption> = emptyList(),
    val selectedFolder: Int? = null,

    // ── Flashcards ────────────────────────────────────────────────────────
    val dueCount: Int = 0,
    val totalCount: Int = 0,
    val sessionFlashcards: List<FlashcardEntity> = emptyList(),
    val currentCardIndex: Int = 0,
    /** The card being edited, if the editor is open. */
    val editingCard: FlashcardEntity? = null,
    val isBulkCreating: Boolean = false,
    val bulkMessage: String? = null,

    // ── Exercise setup ────────────────────────────────────────────────────
    val kinds: List<ExerciseKindInfoDto> = emptyList(),
    val selectedKinds: Set<ExerciseKind> = emptySet(),
    val scope: ExerciseScope = ExerciseScope.SAVED,
    val count: Int = 10,
    val wordsAvailable: Int = 0,
    val isKindsLoading: Boolean = false,

    // ── Exercise session ──────────────────────────────────────────────────
    val isExerciseLoading: Boolean = false,
    val exercises: List<ExerciseDto> = emptyList(),
    val exerciseIndex: Int = 0,
    /** Set once the question is answered; cleared on the way to the next one. */
    val verdict: ExerciseVerdict? = null,
    val given: String = "",
    val typed: String = "",
    val isAudioPlaying: Boolean = false,
    val results: List<ExerciseResult> = emptyList(),
    val notice: String? = null,
    val error: String? = null
) {
    val currentExercise: ExerciseDto? get() = exercises.getOrNull(exerciseIndex)
    val answered: Boolean get() = verdict != null

    val correctCount: Int get() = results.count { it.verdict == ExerciseVerdict.CORRECT }
    val almostCount: Int get() = results.count { it.verdict == ExerciseVerdict.ALMOST }
    val wrongCount: Int get() = results.count { it.verdict == ExerciseVerdict.WRONG }

    /** A near miss is knowledge with a slip in it, so it counts — at half weight. */
    val accuracy: Int
        get() = if (results.isEmpty()) 0
        else (((correctCount + almostCount * 0.5) / results.size) * 100).toInt()

    val availableKinds: List<ExerciseKindInfoDto> get() = kinds.filter { it.available > 0 }
}
