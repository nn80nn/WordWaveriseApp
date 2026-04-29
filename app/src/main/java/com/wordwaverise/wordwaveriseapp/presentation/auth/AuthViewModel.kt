package com.wordwaverise.wordwaveriseapp.presentation.auth

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.wordwaverise.wordwaveriseapp.data.repository.AuthRepository
import com.wordwaverise.wordwaveriseapp.util.Resource
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        private const val TAG = "AuthViewModel"
    }

    private val _state = mutableStateOf(AuthState())
    val state: State<AuthState> = _state

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            combine(authRepository.token, authRepository.userEmail, authRepository.userLogin) { token, email, login ->
                Triple(token, email, login)
            }.collect { (token, email, login) ->
                _state.value = _state.value.copy(
                    isLoggedIn = !token.isNullOrEmpty(),
                    userEmail = email,
                    userLogin = login
                )
            }
        }
    }

    fun onEmailChange(email: String) {
        _state.value = _state.value.copy(email = email, error = null)
    }

    fun onLoginChange(login: String) {
        _state.value = _state.value.copy(login = login, error = null)
    }

    fun onPasswordChange(password: String) {
        _state.value = _state.value.copy(password = password, error = null)
    }

    fun login() {
        val email = _state.value.email.trim()
        val password = _state.value.password
        if (!validateInput(email, password)) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val result = authRepository.login(email, password)) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false, isLoggedIn = true, error = null, email = "", password = ""
                    )
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(isLoading = false, error = result.message)
                }
                is Resource.Loading -> {
                    _state.value = _state.value.copy(isLoading = true)
                }
            }
        }
    }

    fun register() {
        val email = _state.value.email.trim()
        val login = _state.value.login.trim()
        val password = _state.value.password
        if (!validateRegisterInput(email, login, password)) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val result = authRepository.register(email, password, login.ifBlank { null })) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false, isLoggedIn = true, error = null, email = "", login = "", password = ""
                    )
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(isLoading = false, error = result.message)
                }
                is Resource.Loading -> {
                    _state.value = _state.value.copy(isLoading = true)
                }
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val result = authRepository.loginWithGoogle(idToken)) {
                is Resource.Success -> _state.value = _state.value.copy(
                    isLoading = false, isLoggedIn = true, error = null
                )
                is Resource.Error -> _state.value = _state.value.copy(
                    isLoading = false, error = result.message
                )
                else -> {}
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _state.value = AuthState()
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        return when {
            email.isEmpty() -> { _state.value = _state.value.copy(error = "Введите email"); false }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> { _state.value = _state.value.copy(error = "Неверный формат email"); false }
            password.isEmpty() -> { _state.value = _state.value.copy(error = "Введите пароль"); false }
            password.length < 6 -> { _state.value = _state.value.copy(error = "Пароль не менее 6 символов"); false }
            else -> true
        }
    }

    private fun validateRegisterInput(email: String, login: String, password: String): Boolean {
        return when {
            login.isNotBlank() && (login.length < 3 || login.length > 30) -> {
                _state.value = _state.value.copy(error = "Логин: от 3 до 30 символов"); false
            }
            login.isNotBlank() && !Regex("[a-zA-Z0-9_]+").matches(login) -> {
                _state.value = _state.value.copy(error = "Логин: только латиница, цифры и _"); false
            }
            else -> validateInput(email, password)
        }
    }
}
