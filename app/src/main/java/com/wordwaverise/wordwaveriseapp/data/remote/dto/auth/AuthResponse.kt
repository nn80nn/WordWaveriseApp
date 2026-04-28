package com.wordwaverise.wordwaveriseapp.data.remote.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    val status: String,
    val data: AuthData? = null,
    val message: String? = null
)
