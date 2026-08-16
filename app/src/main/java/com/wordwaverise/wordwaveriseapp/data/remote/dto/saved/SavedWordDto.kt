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
    val savedAt: String
)
