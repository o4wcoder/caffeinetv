package tv.caffeine.app.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.MenuItem
import android.widget.ImageView
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import com.squareup.picasso.Callback
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import tv.caffeine.app.R
import tv.caffeine.app.util.CropBorderedCircleTransformation
import tv.caffeine.app.util.getPicasso
import java.lang.Exception

class BottomNavigationAvatar(val context: Context, val menuItem: MenuItem) {

    @VisibleForTesting var selectedAvatarDrawable: Drawable? = null
    @VisibleForTesting var unselectedAvatarDrawable: Drawable? = null
    private val selectedImageView = ImageView(context)
    private val unselectedImageView = ImageView(context)
    private var avatarImageUrl: String? = null

    fun updateSelectedState() {
        if (menuItem.isChecked) {
            selectedAvatarDrawable?.let { menuItem.icon = it }
        } else {
            unselectedAvatarDrawable?.let { menuItem.icon = it }
        }
    }

    fun loadAvatar(imageUrl: String) {
        if (avatarImageUrl == imageUrl) {
            return
        }
        avatarImageUrl = imageUrl
        val picasso = context.getPicasso()
        val requestCreator = picasso
            .load(avatarImageUrl)
            .resizeDimen(R.dimen.avatar_toolbar, R.dimen.avatar_toolbar)
            .centerCrop()

        requestCreator
            .transform(CropCircleTransformation())
            .into(unselectedImageView, object : Callback {
                override fun onSuccess() {
                    unselectedAvatarDrawable = unselectedImageView.drawable
                    if (!menuItem.isChecked) {
                        menuItem.icon = unselectedAvatarDrawable
                    }
                }
                override fun onError(e: Exception?) {
                }
            })

        requestCreator
            .transform(CropBorderedCircleTransformation(
                ContextCompat.getColor(context, R.color.almost_black),
                context.resources.getDimension(R.dimen.avatar_rim_size_regular)))
            .into(selectedImageView, object : Callback {
                override fun onSuccess() {
                    selectedAvatarDrawable = selectedImageView.drawable
                    if (menuItem.isChecked) {
                        menuItem.icon = selectedAvatarDrawable
                    }
                }
                override fun onError(e: Exception?) {
                }
            })
    }
}