package com.wordwaverise.wordwaveriseapp.presentation.saved

import com.wordwaverise.wordwaveriseapp.data.local.entity.CategoryEntity
import com.wordwaverise.wordwaveriseapp.data.local.entity.SavedWordEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class SavedWordsStateTest {

    // Явные метки времени: список сортируется, и «в каком порядке пришло» перестало быть
    // ответом на вопрос «что покажется первым».
    private val words = listOf(
        SavedWordEntity(id = 1, word = "apple", categoryIds = listOf(1), savedAt = 400),
        SavedWordEntity(id = 2, word = "orange", categoryIds = listOf(1), savedAt = 300),
        SavedWordEntity(id = 3, word = "table", savedAt = 200),
        SavedWordEntity(id = 4, word = "vigilant", categoryIds = listOf(2), savedAt = 100)
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
        val shared = SavedWordEntity(id = 5, word = "resolve", categoryIds = listOf(1, 2), savedAt = 50)
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

    // ── Папка-группа ───────────────────────────────────────────────────────────

    /** Модуль (serverId 10) с двумя уроками внутри и одним словом в каждом. */
    private fun withModule(): SavedWordsState {
        val categories = listOf(
            CategoryEntity(id = 10, serverId = 10, name = "Модуль 1"),
            CategoryEntity(id = 11, serverId = 11, name = "Урок 1", parentServerId = 10),
            CategoryEntity(id = 12, serverId = 12, name = "Урок 2", parentServerId = 10)
        )
        return SavedWordsState(
            categories = categories,
            words = listOf(
                SavedWordEntity(id = 1, word = "alpha", categoryIds = listOf(11), savedAt = 300),
                SavedWordEntity(id = 2, word = "beta", categoryIds = listOf(12), savedAt = 200),
                SavedWordEntity(id = 3, word = "gamma", savedAt = 100)
            )
        )
    }

    @Test
    fun `a group folder shows what its lessons hold`() {
        // Иначе счётчик считает вложенные, а список их не ищет: два правдивых числа об одной
        // папке, из которых одно всегда врёт.
        val module = withModule().copy(selectedCategoryId = 10)
        assertEquals(listOf("alpha", "beta"), module.filteredWords.map { it.word })
    }

    @Test
    fun `a group counts its lessons words once`() {
        val state = withModule()
        val both = state.copy(
            words = state.words + SavedWordEntity(
                id = 4, word = "delta", categoryIds = listOf(11, 12), savedAt = 50
            )
        )
        // Слово в двух уроках одного модуля — одно слово модуля, а не два.
        assertEquals(3, both.wordCounts[10])
        assertEquals(2, both.wordCounts[11])
    }

    @Test
    fun `words in no folder have their own filter`() {
        val loose = withModule().copy(selectedCategoryId = SavedWordsState.UNCATEGORIZED)
        assertEquals(listOf("gamma"), loose.filteredWords.map { it.word })
        assertEquals(1, withModule().looseCount)
    }

    @Test
    fun `the open group is the selected folder or its parent`() {
        val state = withModule()
        assertEquals("Модуль 1", state.copy(selectedCategoryId = 10).openGroup?.name)
        // Зайдя в урок, человек не должен терять ни соседние уроки, ни дорогу к модулю.
        assertEquals("Модуль 1", state.copy(selectedCategoryId = 11).openGroup?.name)
        assertEquals(listOf("Урок 1", "Урок 2"), state.copy(selectedCategoryId = 11).openChildren.map { it.name })
    }

    // ── Поиск и порядок ────────────────────────────────────────────────────────

    @Test
    fun `search looks at the word and at its translation`() {
        val withTranslation = state.copy(
            words = words + SavedWordEntity(
                id = 9, word = "resolve", translation = "решать", savedAt = 500
            )
        )
        assertEquals(listOf("resolve"), withTranslation.copy(searchQuery = "реша").filteredWords.map { it.word })
        assertEquals(listOf("apple"), withTranslation.copy(searchQuery = "APP").filteredWords.map { it.word })
    }

    @Test
    fun `sorting is applied to what the filter left`() {
        val fruits = state.copy(selectedCategoryId = 1, sortBy = WordSort.A_Z)
        assertEquals(listOf("apple", "orange"), fruits.filteredWords.map { it.word })
        assertEquals(
            listOf("orange", "apple"),
            fruits.copy(sortBy = WordSort.Z_A).filteredWords.map { it.word }
        )
        assertEquals(
            listOf("vigilant", "table", "orange", "apple"),
            state.copy(sortBy = WordSort.OLDEST).filteredWords.map { it.word }
        )
    }

    @Test
    fun `folder search keeps the group of a lesson it found`() {
        // Найденный «Урок 2» без своего модуля неотличим от «Урока 2» соседнего курса.
        val found = withModule().copy(folderQuery = "урок 2").folderRows
        assertEquals(listOf("Модуль 1", "Урок 2"), found.map { it.folder.name })
    }

    @Test
    fun `folders sort by name and by how much they hold`() {
        val state = withModule()
        assertEquals(
            listOf("Модуль 1", "Урок 1", "Урок 2"),
            state.copy(folderSort = FolderSort.A_Z).folderRows.map { it.folder.name }
        )
        // Модуль держит два слова, уроки по одному — «где у меня всё лежит» отвечает порядком.
        assertEquals(
            "Модуль 1",
            state.copy(folderSort = FolderSort.COUNT).folderRows.first().folder.name
        )
    }
}
