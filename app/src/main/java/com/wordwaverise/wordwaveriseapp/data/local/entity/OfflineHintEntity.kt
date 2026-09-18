package com.wordwaverise.wordwaveriseapp.data.local.entity

import androidx.room.Entity

/**
 * One word's tap answer, ready without a network call — addressed by where it sits in the book,
 * not by the sentence text: the reader already knows the position when it taps, and looking the
 * row up by four integers is exactly what the online endpoint does by (text, token index) too.
 *
 * [payload] is a serialised `ContextHintDto`, the same shape a live tap gets back — the tap
 * handler does not need to know whether an answer came from the network or from this table.
 */
@Entity(
    tableName = "offline_hints",
    primaryKeys = ["bookId", "blockOrdinal", "sentenceIndex", "tokenIndex"]
)
data class OfflineHintEntity(
    val bookId: Int,
    val blockOrdinal: Int,
    val sentenceIndex: Int,
    val tokenIndex: Int,
    val payload: String
)
