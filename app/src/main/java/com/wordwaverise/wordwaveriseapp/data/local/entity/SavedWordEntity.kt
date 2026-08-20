package com.wordwaverise.wordwaveriseapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_words")
data class SavedWordEntity(
    @PrimaryKey
    val word: String,
    val savedAt: Long = System.currentTimeMillis(),
    val serverId: Int? = null,
    val isSynced: Boolean = false,
    val categoryId: Long? = null,

    /**
     * The sense of the article this word was saved under, as the *server* names senses
     * ("n1", "v2"). Kept locally so the article can open on the chosen meaning without waiting
     * for the list to come back from the network.
     */
    val senseId: String? = null,

    /** Группа, из чьей папки пришло слово, или null — если слово своё. */
    val groupServerId: Int? = null,

    /**
     * Слово из папки группы.
     *
     * ⚠️ У такого слова не должно быть ни «удалить», ни «переместить в папку»: обе операции
     * сервер ключует по написанию, и они попали бы в собственную строку ученика с тем же словом.
     */
    val readOnly: Boolean = false
)
