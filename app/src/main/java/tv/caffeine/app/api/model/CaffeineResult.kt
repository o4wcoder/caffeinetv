package tv.caffeine.app.api.model

import tv.caffeine.app.api.ApiErrorResult

sealed class CaffeineResult<T> {
    class Success<T>(val value: T) : CaffeineResult<T>()
    class Error<T>(val error: ApiErrorResult) : CaffeineResult<T>()
    class Failure<T>(val exception: Throwable) : CaffeineResult<T>()
}
