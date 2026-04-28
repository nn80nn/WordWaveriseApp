package com.wordwaverise.wordwaveriseapp.data.remote.dto.saved

import kotlinx.serialization.Serializable

@Serializable
data class SaveWordRequest(
    val word: String,
    val translation: String? = null,
    val definition: String? = null
)
