package com.wordwaverise.wordwaveriseapp.data.remote.dto.ai

import kotlinx.serialization.Serializable

@Serializable
data class AiWordRequest(val word: String)

@Serializable
data class AiTextData(val result: String = "")

@Serializable
data class AiTextApiResponse(
    val status: String = "",
    val data: AiTextData? = null,
    val message: String? = null
)

@Serializable
data class AiExerciseData(val sentence: String = "", val answer: String = "")

@Serializable
data class AiExerciseApiResponse(
    val status: String = "",
    val data: AiExerciseData? = null,
    val message: String? = null
)
