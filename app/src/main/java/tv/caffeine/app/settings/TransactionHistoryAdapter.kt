package tv.caffeine.app.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import tv.caffeine.app.MainNavDirections
import tv.caffeine.app.R
import tv.caffeine.app.api.TransactionHistoryItem
import tv.caffeine.app.databinding.TransactionHistoryItemBinding
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.formatUsernameAsHtml
import tv.caffeine.app.util.DispatchConfig
import tv.caffeine.app.util.getHexColor
import tv.caffeine.app.util.safeNavigate
import java.text.NumberFormat
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class TransactionHistoryAdapter @Inject constructor(
    private val dispatchConfig: DispatchConfig,
    private val followManager: FollowManager,
    private val picasso: Picasso
) : ListAdapter<TransactionHistoryItem, TransactionHistoryViewHolder>(
        object : DiffUtil.ItemCallback<TransactionHistoryItem?>() {
            override fun areItemsTheSame(oldItem: TransactionHistoryItem, newItem: TransactionHistoryItem) =
                    oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: TransactionHistoryItem, newItem: TransactionHistoryItem) =
                    oldItem.id == newItem.id
        }
), CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = job + dispatchConfig.main

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionHistoryViewHolder {
        val binding = TransactionHistoryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionHistoryViewHolder(binding, followManager, this, picasso)
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
    private val picasso: Picasso
) : RecyclerView.ViewHolder(binding.root) {

    private var job: Job? = null
    private val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
    private val usernamePlaceholder = itemView.resources.getString(R.string.transaction_history_username_placeholder)
    private val defaultColor = itemView.context.getHexColor(R.color.black)

    fun bind(item: TransactionHistoryItem) {
        job?.cancel()
        val zoneId = ZoneId.systemDefault()
        val dateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(item.createdAt.toLong()), zoneId)
        binding.timestampTextView.text = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(dateTime)
        binding.goldCostTextView.formatUsernameAsHtml(picasso, item.costString(itemView.resources, numberFormat, usernamePlaceholder, defaultColor), avatarSizeDimen = R.dimen.tx_history_digital_item_size)
        val userCaid = when (item) {
            is TransactionHistoryItem.SendDigitalItem -> item.recipient
            is TransactionHistoryItem.ReceiveDigitalItem -> item.sender
            else -> null
        }
        if (userCaid != null) {
            job = scope.launch {
                val user = followManager.userDetails(userCaid)
                val username = user?.username ?: itemView.resources.getString(R.string.transaction_history_deleted_account)
                val colorRes = when {
                    user == null -> R.color.medium_gray
                    followManager.isFollowing(userCaid) -> R.color.caffeine_blue
                    else -> R.color.black
                }
                if (user != null) {
                    itemView.setOnClickListener {
                        val action = MainNavDirections.actionGlobalProfileFragment(userCaid)
                        itemView.findNavController().safeNavigate(action)
                    }
                }
                val fontColor = itemView.context.getHexColor(colorRes)
                binding.goldCostTextView.formatUsernameAsHtml(picasso, item.costString(itemView.resources, numberFormat, username, fontColor), avatarSizeDimen = R.dimen.tx_history_digital_item_size)
            }
        } else {
            itemView.setOnClickListener(null)
        }
    }
}
