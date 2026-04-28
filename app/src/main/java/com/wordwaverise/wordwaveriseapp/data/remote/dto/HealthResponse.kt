package com.wordwaverise.wordwaveriseapp.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
    val timestamp: Long? = null
)
