package tv.caffeine.app.api.model

import com.google.gson.Gson
import kotlinx.coroutines.Deferred
import retrofit2.Response
import tv.caffeine.app.api.ApiErrorResult

sealed class CaffeineResult<T> {
    class Success<T>(val value: T) : CaffeineResult<T>()
    class Error<T>(val error: ApiErrorResult) : CaffeineResult<T>()
    class Failure<T>(val exception: Throwable) : CaffeineResult<T>()
}

sealed class CaffeineEmptyResult {
    object Success : CaffeineEmptyResult()
    class Error(val error: ApiErrorResult) : CaffeineEmptyResult()
    class Failure(val exception: Throwable) : CaffeineEmptyResult()
}

suspend fun <T> Deferred<Response<T>>.awaitAndParseErrors(gson: Gson): CaffeineResult<T> {
    val response = await()
    val body = response.body()
    val errorBody = response.errorBody()
    return when {
        response.isSuccessful && body != null -> CaffeineResult.Success(body)
        errorBody != null -> CaffeineResult.Error(gson.fromJson(errorBody.string(), ApiErrorResult::class.java))
        else -> CaffeineResult.Failure(Exception("awaitAndParseErrors"))
    }
}

suspend fun <T> Deferred<Response<T>>.awaitEmptyAndParseErrors(gson: Gson): CaffeineEmptyResult {
    val response = await()
    val errorBody = response.errorBody()
    return when {
        response.isSuccessful -> CaffeineEmptyResult.Success
        errorBody != null -> CaffeineEmptyResult.Error(gson.fromJson(errorBody.string(), ApiErrorResult::class.java))
        else -> CaffeineEmptyResult.Failure(Exception("awaitAndParseErrors"))
    }
}
