package tv.caffeine.app.update

import com.google.gson.Gson
import tv.caffeine.app.api.VersionCheckService
import tv.caffeine.app.api.isVersionCheckError
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import javax.inject.Inject

class IsVersionSupportedCheckUseCase @Inject constructor(
        private val gson: Gson,
        private val versionCheckService: VersionCheckService
) {
    suspend operator fun invoke(): Boolean {
        val result = versionCheckService.versionCheck().awaitAndParseErrors(gson)
        return when(result) {
            is CaffeineResult.Success -> !result.value.isVersionCheckError()
            is CaffeineResult.Error -> !result.error.isVersionCheckError()
            is CaffeineResult.Failure -> false
        }
    }
}
