package com.wordwaverise.wordwaveriseapp.presentation.search

import com.wordwaverise.wordwaveriseapp.data.local.entity.CategoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Крестик очищает поиск, а не человека.
 *
 * Список своих папок приезжает из базы один раз, при создании модели, поэтому потерянный
 * сбросом состояния он не возвращается: диалог сохранения следующего слова предлагал только
 * «Без папки» и «создать новую».
 */
class SearchStateTest {

    private fun folder(id: Long, name: String) =
        CategoryEntity(id = id, serverId = id.toInt(), name = name)

    @Test
    fun `clearing the search keeps the folders`() {
        val state = SearchState(
            searchQuery = "resolve",
            hasSearched = true,
            ownFolders = listOf(folder(1, "Урок 5"), folder(2, "Экзамен"))
        )

        assertEquals(listOf("Урок 5", "Экзамен"), state.cleared().ownFolders.map { it.name })
    }

    @Test
    fun `clearing the search drops everything the search put there`() {
        val cleared = SearchState(
            searchQuery = "resolve",
            hasSearched = true,
            isLoading = true,
            error = "boom",
            pendingSenseId = "v1",
            chosenFolders = listOf(1L),
            ownFolders = listOf(folder(1, "Урок 5"))
        ).cleared()

        assertEquals("", cleared.searchQuery)
        assertTrue(!cleared.hasSearched)
        assertTrue(!cleared.isLoading)
        assertNull(cleared.error)
        assertNull(cleared.pendingSenseId)
        assertTrue(cleared.chosenFolders.isEmpty())
    }
}
