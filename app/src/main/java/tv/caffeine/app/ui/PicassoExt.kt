package tv.caffeine.app.ui

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.CropCircleTransformation

@BindingAdapter("imageUrl")
fun ImageView.loadImage(imageUrl: String?) {
    if (imageUrl == null) return
    Picasso.get()
            .load(imageUrl)
            .into(this)
}

@BindingAdapter("roundedImageUrl")
fun ImageView.loadRoundedImage(imageUrl: String?) {
    if (imageUrl == null) return
    Picasso.get()
            .load(imageUrl)
            .transform(CropCircleTransformation())
            .into(this)
}
