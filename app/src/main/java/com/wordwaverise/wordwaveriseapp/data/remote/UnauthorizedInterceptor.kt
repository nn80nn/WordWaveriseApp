package com.wordwaverise.wordwaveriseapp.data.remote

import android.util.Log
import com.wordwaverise.wordwaveriseapp.data.local.TokenDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Clears the stored session as soon as the server rejects the JWT.
 *
 * The token lives 30 days. Without this the expired token stayed in DataStore forever:
 * the app still considered itself logged in while every authenticated call silently
 * returned 401, so the screens showed stale cache and an "offline" banner.
 *
 * Only requests that actually carried an Authorization header are treated as a dead
 * session — a 401 from /api/auth/login (wrong password) must not wipe anything. The
 * rejected token is compared against the stored one so that a late reply from before a
 * re-login cannot wipe the fresh session.
 */
@Singleton
class UnauthorizedInterceptor @Inject constructor(
    private val tokenDataStore: TokenDataStore
) : Interceptor {

    companion object {
        private const val TAG = "UnauthorizedInterceptor"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        val sentAuth = request.header("Authorization")
        if (response.code == 401 && sentAuth != null) {
            runBlocking {
                val current = tokenDataStore.token.firstOrNull()
                if (!current.isNullOrEmpty() && sentAuth == "Bearer $current") {
                    Log.w(TAG, "Token rejected by ${request.url.encodedPath}, clearing session")
                    tokenDataStore.clearToken()
                }
            }
        }

        return response
    }
}
