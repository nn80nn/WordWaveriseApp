package n.startapp.wordwaveriseapp.data.remote.dto.saved

import kotlinx.serialization.Serializable

@Serializable
data class SaveWordResponse(
    val status: String,
    val data: SaveWordData? = null,
    val message: String? = null
)
