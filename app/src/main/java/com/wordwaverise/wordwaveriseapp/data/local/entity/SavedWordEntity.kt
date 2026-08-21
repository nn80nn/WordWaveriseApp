package com.wordwaverise.wordwaveriseapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Одно сохранённое слово — то есть одно **значение** одного слова.
 *
 * ⚠️ Первичным ключом было само написание, и это ставило потолок раньше любого интерфейса:
 * вторая строка для `resolve` не вставала рядом с первой, а затирала её вместе с папкой.
 * Пока ключ был таким, «сохранить два значения как два слова» нельзя было даже показать —
 * не потому, что так решили, а потому, что схема не умела иначе.
 *
 * Ключ теперь — суррогатный `id`, а сервер узнаётся по [serverId]. Само слово только
 * индексируется: одно написание встречается столько раз, сколько значений человек отметил.
 */
@Entity(
    tableName = "saved_words",
    indices = [
        Index(value = ["serverId"], unique = true),
        Index(value = ["word"])
    ]
)
data class SavedWordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val word: String,
    val savedAt: Long = System.currentTimeMillis(),
    val serverId: Int? = null,
    val isSynced: Boolean = false,

    /**
     * Локальные id папок, в которых лежит эта запись. Пусто — «без папки».
     *
     * Локальные, а не серверные: у папки, заведённой офлайн, серверного id ещё нет, и слово,
     * положенное в неё до первой синхронизации, оказалось бы нигде.
     */
    val categoryIds: List<Long> = emptyList(),

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
     * ⚠️ У такого слова не должно быть ни «удалить», ни «переместить в папку»: строка чужая,
     * и переставлять чужой словарь этот экран не вправе.
     */
    val readOnly: Boolean = false
) {
    /** Папка, которую показывает бейдж на карточке слова, когда места ровно на одну. */
    val categoryId: Long? get() = categoryIds.firstOrNull()
}
