package com.wordwaverise.wordwaveriseapp.data.remote.dto.saved

import kotlinx.serialization.Serializable

@Serializable
data class SavedWordDto(
    val id: Int,
    val word: String,
    val translation: String? = null,
    val definition: String? = null,
    val example: String? = null,
    /** Which sense of the article the user pinned, or null when they saved the whole word. */
    val senseId: String? = null,
    val savedAt: String,

    /**
     * **Серверный** id первой папки. Сервер продолжает его слать ради сборок, вышедших до
     * того, как папок стало несколько; новый код читает [categoryIds].
     */
    val categoryId: Int? = null,

    /** Все папки этой записи, серверными id. Пусто — «без папки». */
    val categoryIds: List<Int> = emptyList(),

    /** Группа, из чьей папки пришло слово, или null — если слово своё. */
    val groupId: Int? = null,
    /** У слова из папки группы нет ни удаления, ни переноса — см. `SavedWordEntity`. */
    val readOnly: Boolean = false
)
