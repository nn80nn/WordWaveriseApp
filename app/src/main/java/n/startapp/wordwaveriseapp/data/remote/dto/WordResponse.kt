package n.startapp.wordwaveriseapp.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class WordResponse(
    val status: String = "",
    val data: WordDto? = null
)
