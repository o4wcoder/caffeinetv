package tv.caffeine.app.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import tv.caffeine.app.R
import tv.caffeine.app.api.FollowRecord
import tv.caffeine.app.session.FollowManager

class NotificationsAdapter(private val followManager: FollowManager) : ListAdapter<FollowRecord, NotificationViewHolder>(object : DiffUtil.ItemCallback<FollowRecord?>() {
    override fun areItemsTheSame(oldItem: FollowRecord, newItem: FollowRecord) = oldItem === newItem

    override fun areContentsTheSame(oldItem: FollowRecord, newItem: FollowRecord) = oldItem.caid == newItem.caid
}) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.search_user_item, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val item = getItem(position)
//        holder.bind(item, followManager)
    }

}

class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

