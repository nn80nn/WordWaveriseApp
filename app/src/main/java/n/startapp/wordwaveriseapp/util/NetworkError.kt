package n.startapp.wordwaveriseapp.util

import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object NetworkError {
    fun getErrorMessage(throwable: Throwable): String {
        return when (throwable) {
            is UnknownHostException -> "Нет подключения к интернету"
            is SocketTimeoutException -> "Превышено время ожидания"
            is IOException -> "Ошибка сети: ${throwable.localizedMessage}"
            is HttpException -> {
                when (throwable.code()) {
                    400 -> "Неверный запрос"
                    401 -> "Не авторизован"
                    403 -> "Доступ запрещен"
                    404 -> "Ресурс не найден"
                    500 -> "Внутренняя ошибка сервера"
                    502 -> "Сервер недоступен"
                    503 -> "Сервис временно недоступен"
                    else -> "HTTP ошибка: ${throwable.code()}"
                }
            }
            else -> "Неизвестная ошибка: ${throwable.localizedMessage}"
        }
    }
}
