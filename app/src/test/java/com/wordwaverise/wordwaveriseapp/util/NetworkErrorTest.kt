package com.wordwaverise.wordwaveriseapp.util

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * The distinction these tests guard is the one the app got wrong for a whole
 * release: a rejected token is not a missing network, and only the second one
 * may ever be described to the user as being offline.
 */
class NetworkErrorTest {

    private fun http(code: Int) = HttpException(
        Response.error<Any>(code, "".toResponseBody("application/json".toMediaType()))
    )

    @Test
    fun `a name that will not resolve is reported as no connection`() {
        assertEquals("Нет подключения к интернету", NetworkError.getErrorMessage(UnknownHostException()))
    }

    @Test
    fun `a timeout is reported as a timeout`() {
        assertEquals("Превышено время ожидания", NetworkError.getErrorMessage(SocketTimeoutException()))
    }

    @Test
    fun `an expired token is not described as a network problem`() {
        val message = NetworkError.getErrorMessage(http(401))
        assertEquals("Не авторизован", message)
    }

    @Test
    fun `a server failure is not described as a network problem`() {
        assertEquals("Внутренняя ошибка сервера", NetworkError.getErrorMessage(http(500)))
        assertEquals("Сервер недоступен", NetworkError.getErrorMessage(http(502)))
    }
}
