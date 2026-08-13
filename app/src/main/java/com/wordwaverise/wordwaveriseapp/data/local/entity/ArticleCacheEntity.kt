package com.wordwaverise.wordwaveriseapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A finished article, kept for offline reading.
 *
 * Safe to store indefinitely: on the server a `lexical_entries` row has no TTL,
 * so the article for a lemma is written once and never changes. Only READY
 * articles are cached — a PENDING or DEGRADED response is a moment in time, not
 * an answer.
 *
 * Keyed by the string that was looked up rather than by the lemma alone, because
 * the resolver maps typos and inflected forms onto a lemma: caching under both
 * lets "teh" find its cached article without a round trip.
 */
@Entity(tableName = "article_cache")
data class ArticleCacheEntity(
    @PrimaryKey val key: String,
    /** The whole `LookupResponseDto`, serialised — article and raw sources. */
    val payload: String,
    val updatedAt: Long = System.currentTimeMillis()
)
