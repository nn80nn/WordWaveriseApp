package n.startapp.wordwaveriseapp.presentation.auth

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import n.startapp.wordwaveriseapp.data.repository.AuthRepository
import n.startapp.wordwaveriseapp.util.Resource
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
            combine(authRepository.token, authRepository.userEmail) { token, email ->
                Pair(token, email)
            }.collect { (token, email) ->
                _state.value = _state.value.copy(
                    isLoggedIn = !token.isNullOrEmpty(),
                    userEmail = email
                )
            }
        }
    }

    fun onEmailChange(email: String) {
        _state.value = _state.value.copy(email = email, error = null)
    }

    fun onPasswordChange(password: String) {
        _state.value = _state.value.copy(password = password, error = null)
    }

    fun login() {
        val email = _state.value.email.trim()
        val password = _state.value.password

        if (!validateInput(email, password)) return

        Log.d(TAG, "Starting login for: $email")
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            when (val result = authRepository.login(email, password)) {
                is Resource.Success -> {
                    Log.d(TAG, "Login successful")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        error = null,
                        email = "",
                        password = ""
                    )
                }
                is Resource.Error -> {
                    Log.e(TAG, "Login failed: ${result.message}")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is Resource.Loading -> {
                    _state.value = _state.value.copy(isLoading = true)
                }
            }
        }
    }

    fun register() {
        val email = _state.value.email.trim()
        val password = _state.value.password

        if (!validateInput(email, password)) return

        Log.d(TAG, "Starting registration for: $email")
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            when (val result = authRepository.register(email, password)) {
                is Resource.Success -> {
                    Log.d(TAG, "Registration successful")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        error = null,
                        email = "",
                        password = ""
                    )
                }
                is Resource.Error -> {
                    Log.e(TAG, "Registration failed: ${result.message}")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message
                    )
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
        Log.d(TAG, "Logging out")
        viewModelScope.launch {
            authRepository.logout()
            _state.value = AuthState()
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        return when {
            email.isEmpty() -> {
                _state.value = _state.value.copy(error = "Введите email")
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                _state.value = _state.value.copy(error = "Неверный формат email")
                false
            }
            password.isEmpty() -> {
                _state.value = _state.value.copy(error = "Введите пароль")
                false
            }
            password.length < 6 -> {
                _state.value = _state.value.copy(error = "Пароль должен быть не менее 6 символов")
                false
            }
            else -> true
        }
    }
}
