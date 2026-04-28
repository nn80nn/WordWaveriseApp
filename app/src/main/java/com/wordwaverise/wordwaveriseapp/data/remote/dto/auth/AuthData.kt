package com.wordwaverise.wordwaveriseapp.data.remote.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthData(
    val token: String,
    val user: UserDto
)
