package tv.caffeine.app.ui

import android.net.Uri
import com.squareup.picasso.Picasso
import com.squareup.picasso.Request
import timber.log.Timber
import tv.caffeine.app.net.ServerConfig

class ImageServerRequestTransformer(
    private val serverConfig: ServerConfig,
    private val alwaysTransform: Boolean = true
) : Picasso.RequestTransformer {

    override fun transformRequest(request: Request): Request {
        if (request.resourceId != 0) return request // don't transform resource requests
        val uri = requireNotNull(request.uri)
        val scheme = uri.scheme ?: return request
        if (scheme != "https" && scheme != "http") return request
        val imageServer = ImageServer.Factory.makeRequestBuilder(uri, serverConfig) ?: return request
        if (!request.hasSize() && !alwaysTransform) return request

        val newRequest = request.buildUpon()

        if (request.hasSize()) {
            imageServer.resize(request.targetWidth, request.targetHeight)
            newRequest.clearResize()
        }
        if (request.centerInside) {
            imageServer.centerInside = true
            newRequest.clearCenterInside()
        }
        if (request.centerCrop) {
            imageServer.centerCrop = true
            newRequest.clearCenterCrop()
        }

        val updatedUri = imageServer.buildUri()
        newRequest.setUri(updatedUri)
        Timber.d("uri = $updatedUri")

        return newRequest.build()
    }
}

sealed class ImageServer(protected val baseUri: Uri) {

    object Factory {
        fun makeRequestBuilder(uri: Uri, serverConfig: ServerConfig): ImageServer? = when (uri.host) {
            "assets.caffeine.tv", "images.caffeine.tv" -> Fastly(serverConfig.normalizeImageUri(uri))
            else -> null
        }
    }

    var width: Int? = null
    var height: Int? = null
    var centerInside: Boolean = false
    var centerCrop: Boolean = false

    abstract fun buildUri(): Uri

    fun resize(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    class Fastly(baseUri: Uri) : ImageServer(baseUri) {
        override fun buildUri(): Uri = baseUri.buildUpon().apply {
            // required for assets
            appendQueryParameter("optimize", "true")
            // https://docs.fastly.com/api/imageopto/fit
            if (centerInside) {
                appendQueryParameter("fit", "bounds")
            }
            if (centerCrop) {
                appendQueryParameter("fit", "crop")
            }
            width?.let { appendQueryParameter("width", it.toString()) }
            height?.let { appendQueryParameter("height", it.toString()) }
        }.build()
    }

    class Imgix(baseUri: Uri) : ImageServer(baseUri) {
        override fun buildUri(): Uri = baseUri.buildUpon().apply {
            appendQueryParameter("auto", "compress") // https://docs.imgix.com/apis/url/auto/auto
            appendQueryParameter("fit", "clip") // https://docs.imgix.com/apis/url/size/fit#clip
            width?.let { appendQueryParameter("w", it.toString()) }
            height?.let { appendQueryParameter("h", it.toString()) }
        }.build()
    }
}
