package com.wordwaverise.wordwaveriseapp.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The same cases as `ExerciseGradingTest.kt` on the backend, which owns the specification.
 *
 * Typed answers are graded on the device so the learner does not wait on a round trip, which
 * means the rules exist in three places — Kotlin on the server, TypeScript in the browser, and
 * here. These tests are what stop the same typed answer from being right on the phone and wrong
 * in the browser; if a case changes on the backend, it changes here too.
 */
class ExerciseGradingTest {

    private fun grade(user: String, expected: String) =
        ExerciseGrading.grade(user, listOf(ExerciseGrading.normalize(expected)))

    @Test
    fun `normalisation ignores everything that is not the answer`() {
        assertEquals("resolve", ExerciseGrading.normalize("  Resolve. "))
        assertEquals("resolve", ExerciseGrading.normalize("\"resolve\""))
        assertEquals("make a decision", ExerciseGrading.normalize("make  a   decision"))
        assertEquals("sea", ExerciseGrading.normalize("the sea"))
        assertEquals("the", ExerciseGrading.normalize("The"))
        assertEquals("еж", ExerciseGrading.normalize("Ёж"))
    }

    @Test
    fun `case and punctuation do not change the verdict`() {
        assertEquals(ExerciseVerdict.CORRECT, grade("Perseverance!", "perseverance"))
        assertEquals(ExerciseVerdict.CORRECT, grade("the sea", "sea"))
    }

    @Test
    fun `a single slip in a long word is a near miss, not a failure`() {
        assertEquals(ExerciseVerdict.ALMOST, grade("recieve", "receive"))
        assertEquals(ExerciseVerdict.ALMOST, grade("perseverence", "perseverance"))
    }

    @Test
    fun `short words get no forgiveness, because one letter is another word`() {
        assertEquals(ExerciseVerdict.WRONG, grade("herd", "hard"))
        assertEquals(ExerciseVerdict.WRONG, grade("see", "sea"))
        assertEquals(0, ExerciseGrading.typoBudget("sea"))
    }

    @Test
    fun `a different word entirely is wrong`() {
        assertEquals(ExerciseVerdict.WRONG, grade("abandon", "perseverance"))
        assertEquals(ExerciseVerdict.WRONG, grade("", "anything"))
    }

    @Test
    fun `an ambiguous near miss is graded as neither right nor plainly wrong`() {
        assertEquals(ExerciseVerdict.ALMOST, grade("massive", "missive"))
    }

    @Test
    fun `any accepted spelling counts`() {
        val accepted = listOf("resolved", "resolve")
        assertEquals(ExerciseVerdict.CORRECT, ExerciseGrading.grade("Resolve", accepted))
        assertEquals(ExerciseVerdict.CORRECT, ExerciseGrading.grade("resolved", accepted))
    }

    @Test
    fun `swapping two neighbouring letters costs one edit, not two`() {
        assertEquals(1, ExerciseGrading.editDistance("receive", "recieve"))
        assertEquals(0, ExerciseGrading.editDistance("same", "same"))
        assertEquals(2, ExerciseGrading.editDistance("abcdefgh", "zzzzzzzz", limit = 1))
    }
}
