package n.startapp.wordwaveriseapp.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DefinitionDto(
    val partOfSpeech: String,
    val definition: String,
    val example: String? = null,
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList()
)
