package n.startapp.wordwaveriseapp.data.remote.dto.saved

import kotlinx.serialization.Serializable

@Serializable
data class SavedWordDto(
    val id: Int,
    val word: String,
    val savedAt: String
)
