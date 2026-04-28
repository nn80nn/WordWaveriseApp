package com.wordwaverise.wordwaveriseapp.data.remote.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class GoogleAuthRequest(val idToken: String)
