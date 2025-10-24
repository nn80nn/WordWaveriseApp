package n.startapp.wordwaveriseapp.data.remote.dto.saved

import kotlinx.serialization.Serializable

@Serializable
data class SaveWordData(
    val success: Boolean,
    val word: SavedWordDto? = null
)
