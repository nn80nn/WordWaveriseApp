package com.wordwaverise.wordwaveriseapp.data.repository

import android.util.Log
import com.wordwaverise.wordwaveriseapp.data.local.TokenDataStore
import com.wordwaverise.wordwaveriseapp.data.local.dao.CategoryDao
import com.wordwaverise.wordwaveriseapp.data.local.dao.FlashcardDao
import com.wordwaverise.wordwaveriseapp.data.local.dao.SavedWordDao
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
    private val tokenDataStore: TokenDataStore,
    private val savedWordDao: SavedWordDao,
    private val flashcardDao: FlashcardDao,
    private val categoryDao: CategoryDao
) {
    companion object {
        private const val TAG = "AuthRepository"
        const val EMAIL_NOT_VERIFIED = "EMAIL_NOT_VERIFIED"
    }

    private val errorJson = Json { ignoreUnknownKeys = true; isLenient = true }

    val token: Flow<String?> = tokenDataStore.token
    val userEmail: Flow<String?> = tokenDataStore.userEmail
    val userLogin: Flow<String?> = tokenDataStore.userLogin
    val hasPassword: Flow<Boolean> = tokenDataStore.hasPassword

    // Перевод стоит здесь, а не у каждого вызова: сообщение про аккаунт Google одинаково
    // прилетает и логину, и регистрации, и разъехались бы они молча.
    private fun extractBackendMessage(e: HttpException): String? = try {
        val body = e.response()?.errorBody()?.string()
        humanize(body?.let { errorJson.parseToJsonElement(it).jsonObject["message"]?.jsonPrimitive?.content })
    } catch (ex: Exception) {
        null
    }

    /**
     * Бэкенд отвечает одной английской строкой всем клиентам сразу.
     *
     * Про аккаунт из Google это важно перевести, а не показать как есть: у такого аккаунта
     * пароля не существует вовсе, и английское сообщение отправило бы человека восстанавливать
     * то, чего нет.
     */
    private fun humanize(message: String?): String? = when (message) {
        "This account uses Google Sign-In" ->
            "Этот аккаунт заведён через Google — войдите кнопкой «Войти через Google»"
        "This email is already signed up with Google" ->
            "На этот адрес уже есть аккаунт Google — войдите кнопкой «Войти через Google»"
        "Google account email is not verified" -> "Google не подтвердил адрес этого аккаунта"
        "Invalid email or password" -> "Неверный email или пароль"
        "User with this email already exists" -> "Аккаунт с таким email уже существует"
        else -> message
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
                tokenDataStore.saveToken(
                    response.data.token,
                    response.data.user.email,
                    response.data.user.login,
                    response.data.user.hasPassword
                )
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
                tokenDataStore.saveToken(
                    response.data.token,
                    response.data.user.email,
                    response.data.user.login,
                    response.data.user.hasPassword
                )
                Resource.Success(response.data)
            } else {
                Log.w(TAG, "Login failed: ${response.message}")
                Resource.Error(humanize(response.message) ?: "Неверный email или пароль")
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

    /**
     * Просит удалить аккаунт, подтверждая это тем, что у аккаунта вообще есть.
     *
     * ⚠️ Аккаунт, заведённый через Google, пароля не имеет вовсе, и раньше сервер отвечал ему
     * «неверный пароль» на единственно возможный — пустой. Удалить такой аккаунт было нельзя,
     * а Google Play требует, чтобы было можно, поэтому вторым ключом идёт свежий id-токен.
     */
    suspend fun requestAccountDeletion(
        password: String? = null,
        googleIdToken: String? = null
    ): Resource<UserDto> {
        return try {
            val token = tokenDataStore.token.firstOrNull()
                ?: return Resource.Error("Не авторизован")
            val response = apiService.requestAccountDeletion(
                "Bearer $token",
                RequestDeletionRequest(password = password, googleIdToken = googleIdToken)
            )
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

    /**
     * Перечитывает аккаунт с сервера.
     *
     * Нужно ровно за двумя полями, и оба нельзя вывести локально: есть ли у аккаунта пароль
     * (иначе диалог удаления спросит не то) и не назначено ли уже удаление — раньше баннер
     * «аккаунт будет удалён» жил только до перезапуска приложения, то есть отменить удаление
     * со второго запуска было нечем.
     *
     * ⚠️ Сбой запроса ничего не меняет и никого не разлогинивает: 401 разбирает
     * `UnauthorizedInterceptor`, а запуск без сети не повод выкидывать человека из сессии.
     */
    suspend fun refreshUser(): Resource<UserDto> {
        return try {
            val token = tokenDataStore.token.firstOrNull()
                ?: return Resource.Error("Не авторизован")
            val response = apiService.getCurrentUser("Bearer $token")
            val user = response.data?.user
            if (response.status == "ok" && user != null) {
                tokenDataStore.setHasPassword(user.hasPassword)
                Resource.Success(user)
            } else {
                Resource.Error(response.message ?: "Не удалось прочитать профиль")
            }
        } catch (e: Exception) {
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
                tokenDataStore.saveToken(
                    response.data.token,
                    response.data.user.email,
                    response.data.user.login,
                    response.data.user.hasPassword
                )
                Resource.Success(response.data)
            } else {
                Resource.Error(response.message ?: "Google login failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google login error: ${e.message}", e)
            Resource.Error(NetworkError.getErrorMessage(e))
        }
    }

    /**
     * Logging out has to take the local copy with it.
     *
     * Room outlived the session: clearing only the token left the previous
     * account's saved words, folders and flashcards in the database, and the
     * next person to sign in on the device saw them merged into their own list.
     * Everything dropped here exists on the server and syncs back on login.
     *
     * The article cache is deliberately kept — it holds dictionary entries, not
     * anybody's data, and re-downloading them would be pure waste.
     */
    suspend fun logout() {
        Log.d(TAG, "Logging out user")
        tokenDataStore.clearToken()
        try {
            savedWordDao.deleteAll()
            flashcardDao.deleteAll()
            categoryDao.deleteAll()
        } catch (e: Exception) {
            Log.w(TAG, "Could not clear local data on logout: ${e.message}")
        }
    }
}
