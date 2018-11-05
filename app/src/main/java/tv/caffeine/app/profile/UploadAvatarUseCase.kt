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
        val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val width = (1024 * aspectRatio).toInt()
        val height = 1024
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false)
        val stream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
        val body = RequestBody.create(MediaType.get("image/png"), stream.toByteArray())
        stream.close()
        val result = accountsService.uploadAvatar(body).awaitAndParseErrors(gson)
        return result
    }
}
