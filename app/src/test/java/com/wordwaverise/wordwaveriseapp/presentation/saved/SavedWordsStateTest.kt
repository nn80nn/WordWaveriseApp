package com.wordwaverise.wordwaveriseapp.presentation.saved

import com.wordwaverise.wordwaveriseapp.data.local.entity.CategoryEntity
import com.wordwaverise.wordwaveriseapp.data.local.entity.SavedWordEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class SavedWordsStateTest {

    private val words = listOf(
        SavedWordEntity(word = "apple", categoryIds = listOf(1)),
        SavedWordEntity(word = "orange", categoryIds = listOf(1)),
        SavedWordEntity(word = "table"),
        SavedWordEntity(word = "vigilant", categoryIds = listOf(2))
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

    @Test
    fun `a word filed in two folders is listed by both of them`() {
        val shared = SavedWordEntity(word = "resolve", categoryIds = listOf(1, 2))
        val withShared = state.copy(words = words + shared)

        assertEquals(
            listOf("apple", "orange", "resolve"),
            withShared.copy(selectedCategoryId = 1).filteredWords.map { it.word }
        )
        assertEquals(
            listOf("vigilant", "resolve"),
            withShared.copy(selectedCategoryId = 2).filteredWords.map { it.word }
        )
        // И ровно один раз в общем списке: лежать в двух папках — не то же самое, что быть
        // двумя словами.
        assertEquals(1, withShared.filteredWords.count { it.word == "resolve" })
    }

    @Test
    fun `a class folder is not offered as somewhere to put a word`() {
        // Сервер такую запись отклоняет, и предлагать её значит обещать то, чего не будет.
        val withClass = state.copy(
            categories = state.categories + CategoryEntity(
                id = 9, serverId = 12, name = "Unit 5", groupServerId = 8, readOnly = true
            )
        )
        assertEquals(listOf("Fruits", "Adjectives"), withClass.ownCategories.map { it.name })
    }
}
