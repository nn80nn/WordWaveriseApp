package com.wordwaverise.wordwaveriseapp.data.remote.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class RegisterResponse(
    val status: String,
    val data: RegisterData? = null,
    val message: String? = null
)

@Serializable
data class RegisterData(
    val message: String,
    val email: String,
    val requiresVerification: Boolean = true
)

@Serializable
data class VerifyEmailRequest(
    val email: String,
    val code: String
)

@Serializable
data class ResendVerificationRequest(
    val email: String
)

@Serializable
data class RequestDeletionRequest(
    /** Пароль — для аккаунта, у которого он есть. */
    val password: String? = null,
    /** Свежий id-токен Google — для аккаунта, заведённого через Google: пароля у него нет. */
    val googleIdToken: String? = null
)

/** Generic response for endpoints where `data` is just `{ "message": ... }` */
@Serializable
data class SimpleMessageResponse(
    val status: String,
    val data: SimpleMessageData? = null,
    val message: String? = null
)

@Serializable
data class SimpleMessageData(val message: String)

/** Generic response for endpoints where `data` is `{ "user": ... }` */
@Serializable
data class UserWrapperResponse(
    val status: String,
    val data: UserWrapperData? = null,
    val message: String? = null
)

@Serializable
data class UserWrapperData(val user: UserDto)
