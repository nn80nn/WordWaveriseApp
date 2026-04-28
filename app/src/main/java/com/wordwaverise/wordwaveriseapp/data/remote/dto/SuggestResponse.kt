package com.wordwaverise.wordwaveriseapp.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SuggestDto(
    val query: String = "",
    val lang: String = "",
    val suggestions: List<String> = emptyList()
)

@Serializable
data class SuggestApiResponse(
    val status: String = "",
    val data: SuggestDto? = null
)
