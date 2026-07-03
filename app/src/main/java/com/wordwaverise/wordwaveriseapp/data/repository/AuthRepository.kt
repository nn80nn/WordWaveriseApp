package com.wordwaverise.wordwaveriseapp.data.repository

import android.util.Log
import com.wordwaverise.wordwaveriseapp.data.local.TokenDataStore
import com.wordwaverise.wordwaveriseapp.data.remote.ApiService
import com.wordwaverise.wordwaveriseapp.data.remote.dto.auth.AuthData
import com.wordwaverise.wordwaveriseapp.data.remote.dto.auth.GoogleAuthRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.auth.LoginRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.auth.RegisterData
import com.wordwaverise.wordwaveriseapp.data.remote.dto.auth.RegisterRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.auth.RequestDeletionRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.auth.ResendVerificationRequest
import com.wordwaverise.wordwaveriseapp.data.remote.dto.auth.UserDto
import com.wordwaverise.wordwaveriseapp.data.remote.dto.auth.VerifyEmailRequest
import com.wordwaverise.wordwaveriseapp.util.NetworkError
import com.wordwaverise.wordwaveriseapp.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenDataStore: TokenDataStore
) {
    companion object {
        private const val TAG = "AuthRepository"
        const val EMAIL_NOT_VERIFIED = "EMAIL_NOT_VERIFIED"
    }

    private val errorJson = Json { ignoreUnknownKeys = true; isLenient = true }

    val token: Flow<String?> = tokenDataStore.token
    val userEmail: Flow<String?> = tokenDataStore.userEmail
    val userLogin: Flow<String?> = tokenDataStore.userLogin

    private fun extractBackendMessage(e: HttpException): String? = try {
        val body = e.response()?.errorBody()?.string()
        body?.let { errorJson.parseToJsonElement(it).jsonObject["message"]?.jsonPrimitive?.content }
    } catch (ex: Exception) {
        null
    }

    suspend fun register(email: String, password: String, login: String? = null): Resource<RegisterData> {
        return try {
            Log.d(TAG, "Registering user: $email")
            val response = apiService.register(RegisterRequest(email, password, login))

            if (response.status == "ok" && response.data != null) {
                Log.d(TAG, "Registration accepted, verification required")
                Resource.Success(response.data)
            } else {
                Log.w(TAG, "Registration failed: ${response.message}")
                Resource.Error(response.message ?: "Ошибка регистрации")
            }
        } catch (e: HttpException) {
            Resource.Error(extractBackendMessage(e) ?: NetworkError.getErrorMessage(e))
        } catch (e: Exception) {
            Log.e(TAG, "Registration error: ${e.message}", e)
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }

    suspend fun verifyEmail(email: String, code: String): Resource<AuthData> {
        return try {
            val response = apiService.verifyEmail(VerifyEmailRequest(email, code))
            if (response.status == "ok" && response.data != null) {
                tokenDataStore.saveToken(response.data.token, response.data.user.email, response.data.user.login)
                Resource.Success(response.data)
            } else {
                Resource.Error(response.message ?: "Неверный код")
            }
        } catch (e: HttpException) {
            Resource.Error(extractBackendMessage(e) ?: NetworkError.getErrorMessage(e))
        } catch (e: Exception) {
            Log.e(TAG, "Verify email error: ${e.message}", e)
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }

    suspend fun resendVerification(email: String): Resource<String> {
        return try {
            val response = apiService.resendVerification(ResendVerificationRequest(email))
            if (response.status == "ok") {
                Resource.Success(response.data?.message ?: "Код отправлен повторно")
            } else {
                Resource.Error(response.message ?: "Не удалось отправить код")
            }
        } catch (e: HttpException) {
            Resource.Error(extractBackendMessage(e) ?: NetworkError.getErrorMessage(e))
        } catch (e: Exception) {
            Log.e(TAG, "Resend verification error: ${e.message}", e)
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }

    suspend fun login(email: String, password: String): Resource<AuthData> {
        return try {
            Log.d(TAG, "Logging in user: $email")
            val response = apiService.login(LoginRequest(email, password))

            if (response.status == "ok" && response.data != null) {
                Log.d(TAG, "Login successful")
                tokenDataStore.saveToken(response.data.token, response.data.user.email, response.data.user.login)
                Resource.Success(response.data)
            } else {
                Log.w(TAG, "Login failed: ${response.message}")
                Resource.Error(response.message ?: "Неверный email или пароль")
            }
        } catch (e: HttpException) {
            val backendMessage = extractBackendMessage(e)
            if (backendMessage == "Email not verified") {
                Resource.Error(EMAIL_NOT_VERIFIED)
            } else {
                Resource.Error(backendMessage ?: NetworkError.getErrorMessage(e))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login error: ${e.message}", e)
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }

    suspend fun requestAccountDeletion(password: String): Resource<UserDto> {
        return try {
            val token = tokenDataStore.token.firstOrNull()
                ?: return Resource.Error("Не авторизован")
            val response = apiService.requestAccountDeletion("Bearer $token", RequestDeletionRequest(password))
            if (response.status == "ok" && response.data != null) {
                Resource.Success(response.data.user)
            } else {
                Resource.Error(response.message ?: "Не удалось отправить запрос")
            }
        } catch (e: HttpException) {
            Resource.Error(extractBackendMessage(e) ?: NetworkError.getErrorMessage(e))
        } catch (e: Exception) {
            Log.e(TAG, "Request deletion error: ${e.message}", e)
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }

    suspend fun cancelAccountDeletion(): Resource<UserDto> {
        return try {
            val token = tokenDataStore.token.firstOrNull()
                ?: return Resource.Error("Не авторизован")
            val response = apiService.cancelAccountDeletion("Bearer $token")
            if (response.status == "ok" && response.data != null) {
                Resource.Success(response.data.user)
            } else {
                Resource.Error(response.message ?: "Не удалось отменить удаление")
            }
        } catch (e: HttpException) {
            Resource.Error(extractBackendMessage(e) ?: NetworkError.getErrorMessage(e))
        } catch (e: Exception) {
            Log.e(TAG, "Cancel deletion error: ${e.message}", e)
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }

    suspend fun loginWithGoogle(idToken: String): Resource<AuthData> {
        return try {
            val response = apiService.loginWithGoogle(GoogleAuthRequest(idToken))
            if (response.status == "ok" && response.data != null) {
                tokenDataStore.saveToken(response.data.token, response.data.user.email, response.data.user.login)
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
