package n.startapp.wordwaveriseapp.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class WordDto(
    val word: String = "",
    val phonetic: String? = null,
    val audioUrl: String? = null,
    val translation: String? = null,
    val definitions: List<DefinitionDto> = emptyList()
)
