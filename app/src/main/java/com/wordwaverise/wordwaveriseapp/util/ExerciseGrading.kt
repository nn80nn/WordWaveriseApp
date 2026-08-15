package com.wordwaverise.wordwaveriseapp.util

import com.wordwaverise.wordwaveriseapp.data.remote.dto.exercise.ExerciseDto

/** What the learner's typed answer turned out to be. */
enum class ExerciseVerdict { CORRECT, ALMOST, WRONG }

/**
 * Port of `ExerciseGrading.kt` on the backend, which is the specification and carries the tests.
 *
 * Typed answers are graded on the device rather than on the server so the learner does not wait
 * on a round trip at the one moment they are watching the screen — the answer travels with the
 * exercise. The cost of that is this file: if the backend rules change, this and the web port
 * (`wordwaveriseweb/src/utils/exerciseGrading.ts`) change with them, otherwise the same typed
 * answer starts being right on the phone and wrong in the browser.
 */
object ExerciseGrading {

    private val ARTICLES = setOf("a", "an", "the")

    /**
     * Everything two answers can differ by without differing in meaning: case, spacing,
     * punctuation, a leading article, and the Russian е/ё distinction no keyboard agrees on.
     */
    fun normalize(raw: String): String {
        val cleaned = raw.trim().lowercase().replace('ё', 'е')
            .map { if (it.isLetterOrDigit() || it == '\'' || it == '-') it else ' ' }
            .joinToString("")
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        // "the sea" and "sea" are the same answer; "the" on its own is not an article here.
        val words = if (cleaned.size > 1 && cleaned.first() in ARTICLES) cleaned.drop(1) else cleaned
        return words.joinToString(" ")
    }

    fun accepted(exercise: ExerciseDto): List<String> =
        (listOf(exercise.answer) + exercise.acceptedAnswers)
            .map(::normalize)
            .filter { it.isNotBlank() }
            .distinct()

    fun grade(exercise: ExerciseDto, userAnswer: String): ExerciseVerdict =
        grade(userAnswer, accepted(exercise))

    fun grade(userAnswer: String, acceptedNormalized: List<String>): ExerciseVerdict {
        val given = normalize(userAnswer)
        if (given.isBlank()) return ExerciseVerdict.WRONG
        if (acceptedNormalized.any { it == given }) return ExerciseVerdict.CORRECT

        val almost = acceptedNormalized.any { expected ->
            val budget = typoBudget(expected)
            budget > 0 && editDistance(given, expected, budget) <= budget
        }
        return if (almost) ExerciseVerdict.ALMOST else ExerciseVerdict.WRONG
    }

    /**
     * How many character slips still count as knowing the word. Short words get none: at four
     * letters one edit is often a different word (`hard`/`herd`), and forgiving that would mark
     * a wrong answer right.
     */
    fun typoBudget(expected: String): Int = when {
        expected.length < 5 -> 0
        expected.length <= 8 -> 1
        else -> 2
    }

    /**
     * Edit distance counting a swap of two neighbouring letters as **one** change
     * (Optimal String Alignment), not two.
     *
     * Plain Levenshtein charges 2 for `recieve` → `receive`, the commonest typing mistake there
     * is; with a budget of one it would be graded exactly like an unknown word.
     */
    fun editDistance(a: String, b: String, limit: Int = Int.MAX_VALUE): Int {
        if (a == b) return 0
        if (kotlin.math.abs(a.length - b.length) > limit) return limit + 1
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        var twoAgo = IntArray(b.length + 1)
        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)

        for (i in 1..a.length) {
            current[0] = i
            var rowBest = current[0]
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var value = minOf(
                    current[j - 1] + 1,
                    previous[j] + 1,
                    previous[j - 1] + cost
                )
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    value = minOf(value, twoAgo[j - 2] + 1)
                }
                current[j] = value
                if (value < rowBest) rowBest = value
            }
            if (rowBest > limit) return limit + 1
            val spare = twoAgo; twoAgo = previous; previous = current; current = spare
        }
        return previous[b.length]
    }
}
