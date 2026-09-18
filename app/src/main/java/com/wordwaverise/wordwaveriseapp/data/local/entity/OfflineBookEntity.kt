package com.wordwaverise.wordwaveriseapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One book marked for offline reading — the row that carries its download progress.
 *
 * [detailPayload] is the whole `BookDetailDto`, serialised, the same way [ArticleCacheEntity]
 * stores a finished article: a normalised schema for one row per book would buy nothing, since
 * every read wants the whole thing back.
 *
 * [status] walks TEXT → WARMING → HINTS → READY (or FAILED), and the UI reads it straight off
 * this row rather than from a separate in-memory progress object — the download can outlive the
 * screen that started it, and a row in Room is the one state that survives that.
 */
@Entity(tableName = "offline_books")
data class OfflineBookEntity(
    @PrimaryKey val bookId: Int,
    val detailPayload: String,
    val blockCount: Int,
    val status: String,
    val totalTokens: Int = 0,
    val processedTokens: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_TEXT = "TEXT"
        const val STATUS_WARMING = "WARMING"
        const val STATUS_HINTS = "HINTS"
        const val STATUS_READY = "READY"
        const val STATUS_FAILED = "FAILED"
    }
}
