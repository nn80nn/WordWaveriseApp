package com.wordwaverise.wordwaveriseapp.presentation.saved

import com.wordwaverise.wordwaveriseapp.data.local.entity.CategoryEntity
import com.wordwaverise.wordwaveriseapp.data.local.entity.SavedWordEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class SavedWordsStateTest {

    private val words = listOf(
        SavedWordEntity(word = "apple", categoryId = 1),
        SavedWordEntity(word = "orange", categoryId = 1),
        SavedWordEntity(word = "table", categoryId = null),
        SavedWordEntity(word = "vigilant", categoryId = 2)
    )

    private val state = SavedWordsState(
        words = words,
        categories = listOf(
            CategoryEntity(id = 1, serverId = 3, name = "Fruits"),
            CategoryEntity(id = 2, serverId = 4, name = "Adjectives")
        )
    )

    @Test
    fun `no folder selected shows every word`() {
        assertEquals(4, state.filteredWords.size)
    }

    @Test
    fun `a selected folder shows only its words`() {
        val fruits = state.copy(selectedCategoryId = 1).filteredWords
        assertEquals(listOf("apple", "orange"), fruits.map { it.word })
    }

    @Test
    fun `words with no folder are not swept into a selected one`() {
        val adjectives = state.copy(selectedCategoryId = 2).filteredWords
        assertEquals(listOf("vigilant"), adjectives.map { it.word })
    }
}
