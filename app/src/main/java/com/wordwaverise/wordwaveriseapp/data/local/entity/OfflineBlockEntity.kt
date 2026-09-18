package com.wordwaverise.wordwaveriseapp.data.local.entity

import androidx.room.Entity

/**
 * One block of one offline book — the text itself, sentences and tokens included.
 *
 * [payload] is a serialised `BlockDto`, exactly what `/blocks` already returns: the reader draws
 * from the same DTO either way, so reading it back needs no separate mapping.
 */
@Entity(tableName = "offline_blocks", primaryKeys = ["bookId", "ordinal"])
data class OfflineBlockEntity(
    val bookId: Int,
    val ordinal: Int,
    val payload: String
)
