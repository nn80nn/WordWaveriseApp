package com.wordwaverise.wordwaveriseapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A word folder. [serverId] — not [id] — is the bridge to the server row, and
 * the unique index is what keeps it that way: the sync used to mint a fresh
 * local row for every server category on every run, and code alone can be
 * regressed without anything failing loudly.
 *
 * SQLite allows any number of NULLs in a unique index, so folders created
 * offline (serverId = null) are unaffected.
 */
@Entity(
    tableName = "categories",
    indices = [Index(value = ["serverId"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val serverId: Int? = null,
    val name: String,
    val color: String? = null,
    val createdAt: Long = System.currentTimeMillis(),

    /**
     * Группа, которая одолжила эту папку, или null — если папка своя.
     *
     * Хранится **серверный** id группы: локального у групп нет, они не заводятся офлайн.
     */
    val groupServerId: Int? = null,
    val groupName: String? = null,

    /**
     * Книга, словарём которой служит эта папка, или null у обычной папки.
     *
     * Признак, а не соглашение об имени: значок и фильтр «книги» спрашивают у папки, а
     * названия и книги, и папки человек волен поменять в любой момент. Хранится
     * **серверный** id — книг офлайн не заводят, локального у них нет.
     */
    val bookServerId: Int? = null,

    /**
     * Папка-группа, в которой лежит эта папка. Хранится **серверный** id, как и у групп.
     *
     * Локальный `id` автогенерируемый, API о нём ничего не знает, поэтому связь по нему жила бы
     * только на этом устройстве. Отсюда же следствие: вложить папку, заведённую офлайн, нельзя —
     * у неё ещё нет того, чем на неё ссылаются, и сервер такой запрос всё равно отвергнет.
     */
    val parentServerId: Int? = null,

    /** Папка группы: учить и делать карточки можно, менять — нет. */
    val readOnly: Boolean = false
)
