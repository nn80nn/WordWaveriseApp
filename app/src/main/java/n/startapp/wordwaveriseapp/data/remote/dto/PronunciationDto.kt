package n.startapp.wordwaveriseapp.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PronunciationDto(
    val region: String? = null,       // "uk" | "us" | null
    val ipa: String? = null,
    val audioMp3Url: String? = null
)
