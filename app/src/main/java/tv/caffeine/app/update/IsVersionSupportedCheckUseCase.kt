package tv.caffeine.app.update

import com.google.gson.Gson
import tv.caffeine.app.api.ApiErrorResult
import tv.caffeine.app.api.VersionCheckError
import tv.caffeine.app.api.VersionCheckService
import tv.caffeine.app.api.isVersionCheckError
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import javax.inject.Inject

private const val MAXIMUM_VERSION_CHECK_VALIDITY = 10 * 60 * 1000L // 10 minutes

class IsVersionSupportedCheckUseCase @Inject constructor(
        private val gson: Gson,
        private val versionCheckService: VersionCheckService
) {
    private val successResult = CaffeineEmptyResult.Success
    private val versionCheckErrorResult = CaffeineEmptyResult.Error(VersionCheckError())

    suspend operator fun invoke(): CaffeineEmptyResult {
        val result = versionCheckService.versionCheck().awaitAndParseErrors(gson)
        return when(result) {
            is CaffeineResult.Success -> convertError(result.value)
            is CaffeineResult.Error -> convertError(result.error)
            is CaffeineResult.Failure -> CaffeineEmptyResult.Failure(result.throwable)
        }
    }

    private fun convertError(error: ApiErrorResult) = when {
        error.isVersionCheckError() -> versionCheckErrorResult
        else -> successResult
    }
}
