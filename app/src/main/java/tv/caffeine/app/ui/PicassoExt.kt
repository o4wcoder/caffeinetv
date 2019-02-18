package tv.caffeine.app.ui

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation


@BindingAdapter("imageUrl", "placeholder", "cornerRadius", requireAll = false)
fun ImageView.loadImage(imageUrl: String?, placeholder: Drawable? = null, cornerRadius: Float? = null) {
    if (imageUrl == null) return setImageDrawable(null)
    val picasso = Picasso.Builder(context)
            .requestTransformer(ImgixRequestTransformer())
            .build()
    picasso
            .load(imageUrl)
            .centerCrop()
            .fit()
            .maybePlaceholder(placeholder)
            .maybeRoundedCorners(cornerRadius)
            .into(this)
}

@BindingAdapter("roundedImageUrl", "placeholder", requireAll = false)
fun ImageView.loadRoundedImage(imageUrl: String?, placeholder: Drawable? = null) {
    if (imageUrl == null) return
    val picasso = Picasso.Builder(context)
            .requestTransformer(ImgixRequestTransformer())
            .build()
    picasso
            .load(imageUrl)
            .centerCrop()
            .fit()
            .maybePlaceholder(placeholder)
            .transform(CropCircleTransformation())
            .into(this)
}

fun RequestCreator.maybePlaceholder(placeholder: Drawable?): RequestCreator {
    if (placeholder == null) return this
    return placeholder(placeholder)
}

fun RequestCreator.maybeRoundedCorners(cornerRadius: Float?): RequestCreator {
    if (cornerRadius == null) return this
    return transform(RoundedCornersTransformation(cornerRadius.toInt(), 0))
}
