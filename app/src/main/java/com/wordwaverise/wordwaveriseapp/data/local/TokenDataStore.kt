package com.wordwaverise.wordwaveriseapp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

class TokenDataStore(private val context: Context) {

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("jwt_token")
        private val EMAIL_KEY = stringPreferencesKey("user_email")
        private val LOGIN_KEY = stringPreferencesKey("user_login")

        /**
         * Есть ли у аккаунта пароль.
         *
         * Хранится, а не спрашивается каждый раз: экран Профиля открывается и без сети, а
         * диалог удаления обязан сразу знать, что спрашивать — пароль или вход в Google.
         */
        private val HAS_PASSWORD_KEY = booleanPreferencesKey("has_password")
    }

    val token: Flow<String?> = context.dataStore.data.map { it[TOKEN_KEY] }
    val userEmail: Flow<String?> = context.dataStore.data.map { it[EMAIL_KEY] }
    val userLogin: Flow<String?> = context.dataStore.data.map { it[LOGIN_KEY] }
    val hasPassword: Flow<Boolean> = context.dataStore.data.map { it[HAS_PASSWORD_KEY] ?: true }

    suspend fun saveToken(
        token: String,
        email: String,
        login: String? = null,
        hasPassword: Boolean = true
    ) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
            prefs[EMAIL_KEY] = email
            prefs[HAS_PASSWORD_KEY] = hasPassword
            if (login != null) prefs[LOGIN_KEY] = login
            else prefs.remove(LOGIN_KEY)
        }
    }

    /** Обновляет то, что могло измениться без нового входа (пароль у Google-аккаунта завели). */
    suspend fun setHasPassword(hasPassword: Boolean) {
        context.dataStore.edit { prefs -> prefs[HAS_PASSWORD_KEY] = hasPassword }
    }

    suspend fun clearToken() {
        context.dataStore.edit { prefs ->
            prefs.remove(TOKEN_KEY)
            prefs.remove(EMAIL_KEY)
            prefs.remove(LOGIN_KEY)
            prefs.remove(HAS_PASSWORD_KEY)
        }
    }
}
