package tv.caffeine.app.profile

import android.graphics.Bitmap
import com.google.gson.Gson
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.UploadAvatarResult
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.util.toJpegRequestBody
import javax.inject.Inject

class UploadAvatarUseCase @Inject constructor(
    private val accountsService: AccountsService,
    private val gson: Gson
) {

    suspend operator fun invoke(bitmap: Bitmap): CaffeineResult<UploadAvatarResult> {
        val body = bitmap.toJpegRequestBody()
        return accountsService.uploadAvatar(body).awaitAndParseErrors(gson)
    }
}
