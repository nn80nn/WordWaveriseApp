package n.startapp.wordwaveriseapp.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class Definition(
    val partOfSpeech: String,
    val definition: String,
    val example: String? = null
)

@Serializable
data class WordDetailResponse(
    val word: String,
    val phonetic: String? = null,
    val definitions: List<Definition> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val examples: List<String> = emptyList()
)
