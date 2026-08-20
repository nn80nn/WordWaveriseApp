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

    /** Папка группы: учить и делать карточки можно, менять — нет. */
    val readOnly: Boolean = false
)
