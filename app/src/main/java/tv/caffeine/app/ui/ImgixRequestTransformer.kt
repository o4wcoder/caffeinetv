package tv.caffeine.app.ui

import android.net.Uri
import com.squareup.picasso.Picasso
import com.squareup.picasso.Request
import timber.log.Timber

class ImgixRequestTransformer(private val alwaysTransform: Boolean = true) : Picasso.RequestTransformer {

    override fun transformRequest(request: Request): Request {
        if (request.resourceId != 0) return request // don't transform resource requests
        val uri = requireNotNull(request.uri)
        val scheme = uri.scheme ?: return request
        if (scheme != "https" && scheme != "http") return request
        if (uri.host.startsWith("assets")) return request
        if (!request.hasSize() && !alwaysTransform) return request

        val imgix = Imgix(uri)
        val newRequest = request.buildUpon()

        if (request.hasSize()) {
            imgix.resize(request.targetWidth, request.targetHeight)
            newRequest.clearResize()
        }

        val updatedUri = imgix.buildUrl()
        newRequest.setUri(updatedUri)
        Timber.d("uri = $updatedUri")

        return newRequest.build()
    }

}

class Imgix(private val baseUri: Uri) {

    var width: Int? = null
    var height: Int? = null

    fun buildUrl(): Uri {
        val builder = baseUri.buildUpon()
        builder.appendQueryParameter("auto", "compress")
        builder.appendQueryParameter("fit", "min")
        if (width != null) builder.appendQueryParameter("w", width.toString())
        if (height != null) builder.appendQueryParameter("h", height.toString())
        return builder.build()
    }

    fun resize(width: Int, height: Int) {
        this.width = width
        this.height = height
    }
}
