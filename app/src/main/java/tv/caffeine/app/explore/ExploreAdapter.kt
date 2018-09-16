package tv.caffeine.app.explore

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import tv.caffeine.app.R
import tv.caffeine.app.api.SearchUserItem
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.util.CropBorderedCircleTransformation
import tv.caffeine.app.util.UserTheme
import tv.caffeine.app.util.configure

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
            itemView.resources.getColor(R.color.caffeineBlue, null),
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, itemView.resources.displayMetrics))

    private val cropCircleTransformation = CropCircleTransformation()

    fun bind(item: SearchUserItem, followManager: FollowManager) {
        val followedTheme = UserTheme(cropBorderedCircleTransformation, R.style.ExploreUsername_Following)
        val notFollowedTheme = UserTheme(cropCircleTransformation, R.style.ExploreUsername_NotFollowing)
        item.user.configure(avatarImageView, usernameTextView, followButton, followManager, true, R.dimen.avatar_size, followedTheme, notFollowedTheme)
        itemView.setOnClickListener {
            val action = ExploreFragmentDirections.actionExploreFragmentToProfileFragment(item.user.caid)
            Navigation.findNavController(itemView).navigate(action)
        }
    }
}