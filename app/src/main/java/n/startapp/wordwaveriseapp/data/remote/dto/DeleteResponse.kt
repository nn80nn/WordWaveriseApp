package n.startapp.wordwaveriseapp.data.remote.dto

import kotlinx.serialization.Serializable

/** Generic response for DELETE endpoints where `data` is a plain String message */
@Serializable
data class DeleteResponse(
    val status: String,
    val data: String? = null,
    val message: String? = null
)
