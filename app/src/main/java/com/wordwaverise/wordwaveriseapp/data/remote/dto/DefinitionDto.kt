package com.wordwaverise.wordwaveriseapp.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DefinitionDto(
    val partOfSpeech: String,
    val definition: String,
    val example: String? = null,
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val source: String? = null
)
