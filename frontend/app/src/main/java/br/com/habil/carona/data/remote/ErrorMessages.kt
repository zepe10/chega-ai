package br.com.habil.carona.data.remote

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

fun Throwable.toFriendlyMessage(): String = when (this) {
    is UnknownHostException, is ConnectException ->
        "Não foi possível conectar ao servidor. Verifique sua internet e tente novamente."
    is SocketTimeoutException -> "O servidor demorou demais para responder."
    is HttpException -> "O servidor respondeu com erro (${code()})."
    is IOException -> "Falha de comunicação com o servidor."
    else -> message ?: "Ocorreu um erro inesperado."
}

inline fun <T> runCatchingSuspend(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(e)
}
