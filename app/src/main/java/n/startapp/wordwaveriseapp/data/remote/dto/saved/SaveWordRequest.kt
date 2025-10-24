package n.startapp.wordwaveriseapp.data.remote.dto.saved

import kotlinx.serialization.Serializable

@Serializable
data class SaveWordRequest(
    val word: String
)
