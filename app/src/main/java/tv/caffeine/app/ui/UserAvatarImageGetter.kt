package tv.caffeine.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.Html
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import tv.caffeine.app.R

private class ImageSpec(@DrawableRes val drawable: Int, val width: Int, val height: Int)

private val inlineImages: Map<String, ImageSpec> = mapOf(
        "checkmark" to ImageSpec(R.drawable.verified_small_white, R.dimen.chat_avatar_size, R.dimen.chat_avatar_size),
        "goldcoin" to ImageSpec(R.drawable.gold_coin, R.dimen.coin_size, R.dimen.coin_size),
        "purplecoin" to ImageSpec(R.drawable.purple_coin, R.dimen.coin_size, R.dimen.coin_size)
)

class UserAvatarImageGetter(private val textView: TextView) : Html.ImageGetter {
    override fun getDrawable(source: String?): Drawable? {
        inlineImages[source]?.let {
            return ContextCompat.getDrawable(textView.context, it.drawable).apply {
                this?.setBounds(0, 0, textView.resources.getDimensionPixelSize(it.width), textView.resources.getDimensionPixelSize(it.height))
            }
        }
        return BitmapDrawablePlaceholder(textView).apply {
            Picasso.get()
                    .load(source)
                    .resizeDimen(R.dimen.chat_avatar_size, R.dimen.chat_avatar_size)
                    .transform(CropCircleTransformation())
                    // .placeholder(R.drawable.placeholder)
                    .into(this)
        }
    }

    private class BitmapDrawablePlaceholder(private val textView: TextView) : BitmapDrawable(textView.resources, null as Bitmap?), Target {
        private var drawable: Drawable? = null
        set(value) {
            field = value
            val width = value?.intrinsicWidth ?: 0
            val height = value?.intrinsicHeight ?: 0
            value?.setBounds(0, 0, width, height)
            setBounds(0, 0, width, height)
            textView.text = textView.text
        }

        override fun draw(canvas: Canvas) {
            drawable?.draw(canvas)
        }

        override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
        }

        override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
        }

        override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
            drawable = BitmapDrawable(textView.resources, bitmap)
        }

    }

}
