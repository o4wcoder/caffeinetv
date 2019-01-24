package tv.caffeine.app.profile

import android.graphics.Bitmap
import com.google.gson.Gson
import okhttp3.MediaType
import okhttp3.RequestBody
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.UploadAvatarResult
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import java.io.ByteArrayOutputStream
import javax.inject.Inject

class UploadAvatarUseCase @Inject constructor(
        private val accountsService: AccountsService,
        private val gson: Gson
) {

    suspend operator fun invoke(bitmap: Bitmap): CaffeineResult<UploadAvatarResult> {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        val body = RequestBody.create(MediaType.get("image/jpeg"), stream.toByteArray())
        stream.close()
        return accountsService.uploadAvatar(body).awaitAndParseErrors(gson)
    }
}
