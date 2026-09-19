package com.wordwaverise.wordwaveriseapp.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * backend.wordwaverise.com is mid-migration and can go unreachable without notice.
 * wordwaverise.remess.org is a standing alias at the same backend, spared from the
 * migration. On a connection failure or a 502/503/504 from the primary host, this retries
 * the exact same call against the fallback host — once. It only ever touches a request
 * that targets the primary host, so it can't redirect a call pointed anywhere else.
 */
class FailoverInterceptor(
    primaryBaseUrl: String,
    fallbackBaseUrl: String
) : Interceptor {

    private val primaryHost = primaryBaseUrl.toHttpUrlOrNull()?.host
    private val fallbackHost = fallbackBaseUrl.toHttpUrlOrNull()?.host

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (fallbackHost == null || request.url.host != primaryHost) {
            return chain.proceed(request)
        }

        return try {
            val response = chain.proceed(request)
            if (isFailoverStatus(response.code)) {
                response.close()
                chain.proceed(request.withHost(fallbackHost))
            } else {
                response
            }
        } catch (primaryFailure: IOException) {
            try {
                chain.proceed(request.withHost(fallbackHost))
            } catch (fallbackFailure: IOException) {
                throw primaryFailure.also { it.addSuppressed(fallbackFailure) }
            }
        }
    }

    private fun isFailoverStatus(code: Int) = code == 502 || code == 503 || code == 504

    private fun Request.withHost(host: String): Request =
        newBuilder().url(url.newBuilder().host(host).build()).build()
}
