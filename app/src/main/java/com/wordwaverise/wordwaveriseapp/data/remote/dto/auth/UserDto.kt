package com.wordwaverise.wordwaveriseapp.data.remote.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: Int,
    val email: String,
    val login: String? = null,
    val createdAt: String,
    val emailVerified: Boolean = true,
    val deletionScheduledFor: String? = null
)
