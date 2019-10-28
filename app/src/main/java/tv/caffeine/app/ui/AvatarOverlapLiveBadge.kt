package tv.caffeine.app.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import tv.caffeine.app.R
import tv.caffeine.app.api.model.User
import tv.caffeine.app.databinding.LayoutAvatarOverlapLiveBadgeBinding
import tv.caffeine.app.lobby.release.OnlineBroadcaster

class AvatarOverlapLiveBadge @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(
    context,
    attrs,
    defStyleAttr
) {

    val binding: LayoutAvatarOverlapLiveBadgeBinding
    val viewModel: AvatarOverlapLiveBadgeViewModel

    /** The Lobby OnlineBroadcaster associated with this component. */
    var lobbyBroadcaster: OnlineBroadcaster? = null
        set(value) {
            field = value
            viewModel.lobbyBroadcaster = value
        }

    /** The friends who are following on the Stage associated with this component */
    var stageFollowers: List<User>? = null
        set(value) {
            field = value
            viewModel.stageFollowers = value
        }

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        viewModel = AvatarOverlapLiveBadgeViewModel(context)
        binding = DataBindingUtil.inflate(inflater, R.layout.layout_avatar_overlap_live_badge, this, true)
        binding.viewModel = viewModel
    }
}

@BindingAdapter("onlineBroadcaster")
fun AvatarOverlapLiveBadge.setOnlineBroadcaster(broadcaster: OnlineBroadcaster) {
    this.lobbyBroadcaster = broadcaster
}
