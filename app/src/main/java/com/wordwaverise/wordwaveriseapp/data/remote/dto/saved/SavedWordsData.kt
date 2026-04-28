package com.wordwaverise.wordwaveriseapp.data.remote.dto.saved

import kotlinx.serialization.Serializable

@Serializable
data class SavedWordsData(
    val words: List<SavedWordDto> = emptyList()
)
