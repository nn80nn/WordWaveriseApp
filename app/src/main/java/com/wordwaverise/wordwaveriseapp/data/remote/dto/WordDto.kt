package com.wordwaverise.wordwaveriseapp.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class WordDto(
    val word: String = "",
    val phonetic: String? = null,
    val audioUrl: String? = null,
    val pronunciations: List<PronunciationDto> = emptyList(),
    val translation: String? = null,
    val definitions: List<DefinitionDto> = emptyList()
)
