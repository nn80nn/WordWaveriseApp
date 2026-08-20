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
     * **Серверный** id папки. Без него телефон не знал о папке слова вовсе: локальный
     * `categoryId` ставился только когда слово перекладывали руками на этом же устройстве.
     */
    val categoryId: Int? = null,

    /** Группа, из чьей папки пришло слово, или null — если слово своё. */
    val groupId: Int? = null,
    /** У слова из папки группы нет ни удаления, ни переноса — см. `SavedWordEntity`. */
    val readOnly: Boolean = false
)
