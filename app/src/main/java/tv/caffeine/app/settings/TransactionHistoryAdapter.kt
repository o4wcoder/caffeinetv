package tv.caffeine.app.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import tv.caffeine.app.R
import tv.caffeine.app.api.TransactionHistoryItem
import tv.caffeine.app.api.costString
import tv.caffeine.app.api.digitalItemStaticImageUrl
import tv.caffeine.app.api.titleResId
import tv.caffeine.app.databinding.TransactionHistoryItemBinding
import tv.caffeine.app.di.ThemeFollowedExplore
import tv.caffeine.app.di.ThemeNotFollowedExplore
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.UserAvatarImageGetter
import tv.caffeine.app.util.UserTheme
import tv.caffeine.app.util.configure
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class TransactionHistoryAdapter @Inject constructor(
        private val followManager: FollowManager,
        @ThemeFollowedExplore private val followedTheme: UserTheme,
        @ThemeNotFollowedExplore private val notFollowedTheme: UserTheme
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
        get() = job + Dispatchers.Main

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionHistoryViewHolder {
        val binding = TransactionHistoryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionHistoryViewHolder(binding, followManager, this, followedTheme, notFollowedTheme)
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
        private val scope: CoroutineScope,
        private val followedTheme: UserTheme,
        private val notFollowedTheme: UserTheme
) : RecyclerView.ViewHolder(binding.root) {

    var job: Job? = null

    fun bind(item: TransactionHistoryItem) {
        job?.cancel()
        clear()
        val zoneId = ZoneId.systemDefault()
        val dateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(item.createdAt.toLong()), zoneId)
        binding.timestampTextView.text = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(dateTime)
        binding.digitalItemImageUrl = item.digitalItemStaticImageUrl
        val rawString = item.costString(itemView.resources)
        val imageGetter = UserAvatarImageGetter(binding.goldCostTextView)
        val html = HtmlCompat.fromHtml(rawString, HtmlCompat.FROM_HTML_MODE_LEGACY, imageGetter, null)
        binding.goldCostTextView.text = html
        binding.transactionTitle.setText(item.titleResId)
        val userCaid = when(item) {
            is TransactionHistoryItem.SendDigitalItem -> item.recipient
            is TransactionHistoryItem.ReceiveDigitalItem -> item.sender
            else -> null
        }
        binding.usernameTextView.isVisible = userCaid != null
        binding.avatarImageView.isVisible = userCaid != null
        binding.digitalItemImageView.isVisible = item.digitalItemStaticImageUrl != null
        job = scope.launch {
            userCaid?.let { caid ->
                val user = followManager.userDetails(caid) ?: return@launch
                withContext(Dispatchers.Main) {
                    user.configure(binding.avatarImageView, binding.usernameTextView,
                            null, followManager, false,
                            R.dimen.avatar_size, followedTheme, notFollowedTheme)
                }
            }
        }
    }

    private fun clear() {
        binding.avatarImageView.setImageResource(R.drawable.default_avatar_round)
        binding.usernameTextView.text = null
        binding.usernameTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
        binding.avatarImageView.isVisible = false
        binding.usernameTextView.isVisible = false
    }
}
