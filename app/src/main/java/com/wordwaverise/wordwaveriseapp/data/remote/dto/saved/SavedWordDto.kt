package com.wordwaverise.wordwaveriseapp.data.remote.dto.saved

import kotlinx.serialization.Serializable

@Serializable
data class SavedWordDto(
    val id: Int,
    val word: String,
    val translation: String? = null,
    val definition: String? = null,
    val savedAt: String
)
