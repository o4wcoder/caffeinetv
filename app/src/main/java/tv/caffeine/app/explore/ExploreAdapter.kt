package tv.caffeine.app.explore

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import tv.caffeine.app.R
import tv.caffeine.app.api.SearchUserItem
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.util.CropBorderedCircleTransformation

class ExploreAdapter(private val followManager: FollowManager) : ListAdapter<SearchUserItem, ExploreViewHolder>(object: DiffUtil.ItemCallback<SearchUserItem?>() {
    override fun areItemsTheSame(oldItem: SearchUserItem, newItem: SearchUserItem) = oldItem === newItem

    override fun areContentsTheSame(oldItem: SearchUserItem, newItem: SearchUserItem) = oldItem.id == newItem.id
}) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExploreViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.search_user_item, parent, false)
        return ExploreViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExploreViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, followManager)
    }

}

class ExploreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val avatarImageView: ImageView = itemView.findViewById(R.id.avatar_image_view)
    private val usernameTextView: TextView = itemView.findViewById(R.id.username_text_view)
    private val followButton: Button = itemView.findViewById(R.id.follow_button)

    private val cropBorderedCircleTransformation = CropBorderedCircleTransformation(
            itemView.resources.getColor(R.color.colorPrimary, null),
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, itemView.resources.displayMetrics))

    private val cropCircleTransformation = CropCircleTransformation()

    fun bind(item: SearchUserItem, followManager: FollowManager) {
        val following = followManager.isFollowing(item.user.caid)
        val transformation = if (following) {
            cropBorderedCircleTransformation
        } else {
            cropCircleTransformation
        }
        if (followManager.followersLoaded() && !following) {
            followButton.isVisible = true
            followButton.setOnClickListener {
                followButton.isVisible = false
                followManager.followUser(item.user.caid)
            }
        } else {
            followButton.isVisible = false
            followButton.setOnClickListener(null)
        }
        Picasso.get()
                .load(item.user.avatarImageUrl)
                .centerCrop()
                .resizeDimen(R.dimen.avatar_size, R.dimen.avatar_size)
                .placeholder(R.drawable.default_avatar)
                .transform(transformation)
                .into(avatarImageView)
        usernameTextView.text = item.user.username
        if (item.user.isVerified) {
            usernameTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.verified_large, 0)
        } else {
            usernameTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
        }
    }
}