package tv.caffeine.app.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import tv.caffeine.app.LobbyDirections
import tv.caffeine.app.R
import tv.caffeine.app.api.TransactionHistoryItem
import tv.caffeine.app.api.costString
import tv.caffeine.app.api.digitalItemStaticImageUrl
import tv.caffeine.app.api.titleResId
import tv.caffeine.app.databinding.TransactionHistoryItemBinding
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.formatUsernameAsHtml
import tv.caffeine.app.ui.htmlText
import tv.caffeine.app.util.DispatchConfig
import tv.caffeine.app.util.safeNavigate
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class TransactionHistoryAdapter @Inject constructor(
        private val dispatchConfig: DispatchConfig,
        private val followManager: FollowManager
): ListAdapter<TransactionHistoryItem, TransactionHistoryViewHolder>(
        object: DiffUtil.ItemCallback<TransactionHistoryItem?>() {
            override fun areItemsTheSame(oldItem: TransactionHistoryItem, newItem: TransactionHistoryItem) =
                    oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: TransactionHistoryItem, newItem: TransactionHistoryItem) =
                    oldItem == newItem
        }
), CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + dispatchConfig.main

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionHistoryViewHolder {
        val binding = TransactionHistoryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionHistoryViewHolder(binding, followManager, this)
    }

    override fun onBindViewHolder(holder: TransactionHistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        job.cancel()
    }
}

class TransactionHistoryViewHolder(
        private val binding: TransactionHistoryItemBinding,
        private val followManager: FollowManager,
        private val scope: CoroutineScope
) : RecyclerView.ViewHolder(binding.root) {

    var job: Job? = null

    fun bind(item: TransactionHistoryItem) {
        job?.cancel()
        val zoneId = ZoneId.systemDefault()
        val dateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(item.createdAt.toLong()), zoneId)
        binding.timestampTextView.text = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(dateTime)
        binding.digitalItemImageUrl = item.digitalItemStaticImageUrl
        binding.goldCostTextView.htmlText = item.costString(itemView.resources)
        binding.transactionTitle.setText(item.titleResId)
        val userCaid = when(item) {
            is TransactionHistoryItem.SendDigitalItem -> item.recipient
            is TransactionHistoryItem.ReceiveDigitalItem -> item.sender
            else -> null
        }
        if (userCaid != null) {
            val usernameTextAppearance = when {
                followManager.isFollowing(userCaid) -> R.style.ExploreUsername_Following
                else -> R.style.ExploreUsername_NotFollowing
            }
            binding.usernameTextView.setTextAppearance(usernameTextAppearance)
            binding.usernameTextView.text = null
            job = scope.launch {
                val user = followManager.userDetails(userCaid) ?: return@launch
                val usernameStringResId = when {
                    user.isVerified -> R.string.user_avatar_username_verified
                    else -> R.string.user_avatar_username_not_verified
                }
                binding.usernameTextView.formatUsernameAsHtml(itemView.resources.getString(usernameStringResId, user.username, user.avatarImageUrl), followManager.isFollowing(userCaid), R.dimen.tx_history_avatar_size)
            }
            itemView.setOnClickListener {
                val action = LobbyDirections.actionGlobalProfileFragment(userCaid)
                itemView.findNavController().safeNavigate(action)
            }
        } else {
            binding.usernameTextView.text = null
            itemView.setOnClickListener(null)
        }
        binding.digitalItemImageView.isVisible = item.digitalItemStaticImageUrl != null
    }
}
