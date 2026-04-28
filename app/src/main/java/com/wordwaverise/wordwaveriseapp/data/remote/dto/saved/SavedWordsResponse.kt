package com.wordwaverise.wordwaveriseapp.data.remote.dto.saved

import kotlinx.serialization.Serializable

@Serializable
data class SavedWordsResponse(
    val status: String,
    val data: SavedWordsData? = null,
    val message: String? = null
)
