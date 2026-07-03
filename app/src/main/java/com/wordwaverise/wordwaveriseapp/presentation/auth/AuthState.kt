package com.wordwaverise.wordwaveriseapp.presentation.auth

data class AuthState(
    val email: String = "",
    val login: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val userEmail: String? = null,
    val userLogin: String? = null,
    val needsVerification: Boolean = false,
    val pendingEmail: String = "",
    val verificationCode: String = "",
    val resendLoading: Boolean = false,
    val deletionScheduledFor: String? = null,
    val deletionActionLoading: Boolean = false,
    val deletionError: String? = null
)
