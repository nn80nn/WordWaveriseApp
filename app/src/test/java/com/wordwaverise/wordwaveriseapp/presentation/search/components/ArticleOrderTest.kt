package com.wordwaverise.wordwaveriseapp.presentation.search.components

import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.LexicalEntryDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.PosGroupDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.lexical.SenseDto
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The rule the saved sense buys the reader: the meaning they chose leads the article.
 *
 * Mirrors `ArticleView.groups` on the web — one folder must not read differently in the browser
 * and on the phone.
 */
class ArticleOrderTest {

    private fun sense(id: String) = SenseDto(id = id, definitionEn = "d-$id")

    /** Noun first in the article, verb second — so a verb pin has to move a whole group. */
    private fun entry() = LexicalEntryDto(
        lemma = "resolve",
        posGroups = listOf(
            PosGroupDto(pos = "noun", posRu = "существительное", senses = listOf(sense("n1"), sense("n2"))),
            PosGroupDto(pos = "verb", posRu = "глагол", senses = listOf(sense("v1"), sense("v2"), sense("v3")))
        )
    )

    @Test
    fun `an unpinned article keeps the dictionary's own order`() {
        assertEquals(listOf("noun", "verb"), orderedGroups(entry(), null).map { it.pos })
        assertEquals(
            listOf(1 to "v1", 2 to "v2", 3 to "v3"),
            orderedSenses(entry().posGroups[1], null).map { it.first to it.second.id }
        )
    }

    @Test
    fun `the pinned sense brings its part of speech to the front`() {
        assertEquals(listOf("verb", "noun"), orderedGroups(entry(), "v3").map { it.pos })
    }

    @Test
    fun `the pinned sense leads its group but keeps its real number`() {
        val ordered = orderedSenses(entry().posGroups[1], "v3").map { it.first to it.second.id }

        // Renumbering to 1 would claim the dictionary lists this sense first.
        assertEquals(listOf(3 to "v3", 1 to "v1", 2 to "v2"), ordered)
    }

    @Test
    fun `a pin the article no longer carries changes nothing`() {
        assertEquals(listOf("noun", "verb"), orderedGroups(entry(), "v9").map { it.pos })
        assertEquals(
            listOf(1 to "v1", 2 to "v2", 3 to "v3"),
            orderedSenses(entry().posGroups[1], "v9").map { it.first to it.second.id }
        )
    }
}
