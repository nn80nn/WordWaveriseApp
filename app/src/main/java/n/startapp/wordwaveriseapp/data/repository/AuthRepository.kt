package n.startapp.wordwaveriseapp.data.repository

import android.util.Log
import n.startapp.wordwaveriseapp.data.local.TokenDataStore
import n.startapp.wordwaveriseapp.data.remote.ApiService
import n.startapp.wordwaveriseapp.data.remote.dto.auth.AuthData
import n.startapp.wordwaveriseapp.data.remote.dto.auth.GoogleAuthRequest
import n.startapp.wordwaveriseapp.data.remote.dto.auth.LoginRequest
import n.startapp.wordwaveriseapp.data.remote.dto.auth.RegisterRequest
import n.startapp.wordwaveriseapp.util.NetworkError
import n.startapp.wordwaveriseapp.util.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenDataStore: TokenDataStore
) {
    companion object {
        private const val TAG = "AuthRepository"
    }

    val token: Flow<String?> = tokenDataStore.token
    val userEmail: Flow<String?> = tokenDataStore.userEmail

    suspend fun register(email: String, password: String): Resource<AuthData> {
        return try {
            Log.d(TAG, "Registering user: $email")
            val response = apiService.register(RegisterRequest(email, password))

            if (response.status == "ok" && response.data != null) {
                Log.d(TAG, "Registration successful")
                tokenDataStore.saveToken(response.data.token, response.data.user.email)
                Resource.Success(response.data)
            } else {
                Log.w(TAG, "Registration failed: ${response.message}")
                Resource.Error(response.message ?: "Ошибка регистрации")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Registration error: ${e.message}", e)
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }

    suspend fun login(email: String, password: String): Resource<AuthData> {
        return try {
            Log.d(TAG, "Logging in user: $email")
            val response = apiService.login(LoginRequest(email, password))

            if (response.status == "ok" && response.data != null) {
                Log.d(TAG, "Login successful")
                tokenDataStore.saveToken(response.data.token, response.data.user.email)
                Resource.Success(response.data)
            } else {
                Log.w(TAG, "Login failed: ${response.message}")
                Resource.Error(response.message ?: "Неверный email или пароль")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login error: ${e.message}", e)
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }

    suspend fun loginWithGoogle(idToken: String): Resource<AuthData> {
        return try {
            val response = apiService.loginWithGoogle(GoogleAuthRequest(idToken))
            if (response.status == "ok" && response.data != null) {
                tokenDataStore.saveToken(response.data.token, response.data.user.email)
                Resource.Success(response.data)
            } else {
                Resource.Error(response.message ?: "Google login failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google login error: ${e.message}", e)
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }

    suspend fun logout() {
        Log.d(TAG, "Logging out user")
        tokenDataStore.clearToken()
    }
}
