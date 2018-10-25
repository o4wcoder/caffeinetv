package tv.caffeine.app.api.model

sealed class CaffeineResult<T> {
    class Success<T>(val value: T) : CaffeineResult<T>()
    class Failure<T>(val exception: Throwable) : CaffeineResult<T>()
}
