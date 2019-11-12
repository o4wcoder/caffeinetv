package tv.caffeine.app.ui

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.DimenRes
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.databinding.BindingAdapter
import com.squareup.picasso.RequestCreator
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import tv.caffeine.app.R
import tv.caffeine.app.util.getPicasso

@BindingAdapter("rawImageUrl")
fun ImageView.loadRawImage(imageUrl: String?) {
    if (imageUrl == null) return setImageDrawable(null)
    val picasso = context.getPicasso()
    picasso.load(imageUrl).into(this)
}

@BindingAdapter("imageUrl", "placeholder", "cornerRadius", requireAll = false)
fun ImageView.loadImage(imageUrl: String?, placeholder: Drawable? = null, cornerRadius: Float? = null) {
    if (imageUrl == null) return setImageDrawable(null)
    val picasso = context.getPicasso()
    picasso
            .load(imageUrl)
            .fit()
            .centerInside()
            .maybePlaceholder(placeholder)
            .maybeRoundedCorners(cornerRadius)
            .into(this)
}

@BindingAdapter("roundedImageUrl", "placeholder", "imageSizeRes", requireAll = false)
fun ImageView.loadRoundedImage(imageUrl: String?, placeholder: Drawable? = null, @DimenRes imageSizeRes: Int? = null) {
    if (imageUrl == null) return
    val picasso = context.getPicasso()
    picasso
            .load(imageUrl)
            .apply {
                if (imageSizeRes != null) {
                    resizeDimen(imageSizeRes, imageSizeRes)
                } else {
                    fit()
                }
            }
            .centerCrop()
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

fun ImageView.loadAvatar(avatarImageUrl: String, isFollowing: Boolean, @DimenRes avatarImageSizeRes: Int, hasRim: Boolean = false) {
    val resources = context.resources
    val isAvatarLarge = resources.getDimension(avatarImageSizeRes) >
            resources.getDimension(R.dimen.avatar_rim_size_threshold_on_avatar_size)
    val rimSize = resources.getDimensionPixelSize(
            if (isFollowing) {
                if (isAvatarLarge) R.dimen.avatar_rim_size_large else R.dimen.avatar_rim_size_regular
            } else if (hasRim) {
                R.dimen.avatar_no_rim
            } else {
                R.dimen.avatar_rim_size_not_following
            })
    setPadding(rimSize)

    setBackgroundResource(
            if (isFollowing) {
                if (isAvatarLarge) R.drawable.avatar_rim_following_large else R.drawable.avatar_rim_following_regular
            } else {
                R.drawable.avatar_rim_not_following
            }
    )
    loadRoundedImage(avatarImageUrl, ContextCompat.getDrawable(context, R.drawable.default_avatar_round), avatarImageSizeRes)
}
