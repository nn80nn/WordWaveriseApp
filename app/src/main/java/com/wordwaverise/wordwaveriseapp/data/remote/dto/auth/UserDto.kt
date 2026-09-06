package com.wordwaverise.wordwaveriseapp.data.remote.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: Int,
    val email: String,
    val login: String? = null,
    val createdAt: String,
    val emailVerified: Boolean = true,
    val deletionScheduledFor: String? = null,
    /**
     * Есть ли у аккаунта пароль вообще.
     *
     * Аккаунт, заведённый через Google, живёт без пароля, и диалог удаления обязан спросить
     * другое — свежий вход в Google. По умолчанию true: старый сервер поля не пришлёт, а у
     * подавляющего большинства аккаунтов пароль есть.
     */
    val hasPassword: Boolean = true
)
