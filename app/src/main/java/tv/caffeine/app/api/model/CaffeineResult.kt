package tv.caffeine.app.api.model

import com.google.gson.Gson
import kotlinx.coroutines.Deferred
import retrofit2.Response
import tv.caffeine.app.api.ApiErrorResult

sealed class CaffeineResult<T> {
    class Success<T>(val value: T) : CaffeineResult<T>()
    class Error<T>(val error: ApiErrorResult) : CaffeineResult<T>()
    class Failure<T>(val throwable: Throwable) : CaffeineResult<T>()
}

sealed class CaffeineEmptyResult {
    object Success : CaffeineEmptyResult()
    class Error(val error: ApiErrorResult) : CaffeineEmptyResult()
    class Failure(val throwable: Throwable) : CaffeineEmptyResult()
}

suspend fun <T> Deferred<Response<T>>.awaitAndParseErrors(gson: Gson): CaffeineResult<T> {
    val response = runCatching { await() }.getOrElse { return CaffeineResult.Failure(it) }
    val body = response.body()
    val errorBody = response.errorBody()
    return when {
        response.isSuccessful && body != null -> CaffeineResult.Success(body)
        errorBody != null -> parseApiError(gson, errorBody.string())?.let { CaffeineResult.Error<T>(it) } ?: CaffeineResult.Failure(Exception("Couldn't parse error"))
        else -> CaffeineResult.Failure(Exception("awaitAndParseErrors"))
    }
}

suspend fun <T> Deferred<Response<T>>.awaitEmptyAndParseErrors(gson: Gson): CaffeineEmptyResult {
    val response = runCatching { await() }.getOrElse { return CaffeineEmptyResult.Failure(it) }
    val errorBody = response.errorBody()
    return when {
        response.isSuccessful -> CaffeineEmptyResult.Success
        errorBody != null -> parseApiError(gson, errorBody.string())?.let { CaffeineEmptyResult.Error(it) } ?: CaffeineEmptyResult.Failure(Exception("Couldn't parse error"))
        else -> CaffeineEmptyResult.Failure(Exception("awaitAndParseErrors"))
    }
}

private fun parseApiError(gson: Gson, error: String) = try {
    gson.fromJson(error, ApiErrorResult::class.java)
} catch (e: Exception) {
    null
}
