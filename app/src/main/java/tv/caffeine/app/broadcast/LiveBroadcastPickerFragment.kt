package tv.caffeine.app.broadcast

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.threeten.bp.Clock
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.analytics.EventManager
import tv.caffeine.app.api.BroadcastsService
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.databinding.FragmentLiveBroadcastPickerBinding
import tv.caffeine.app.databinding.LiveBroadcastCardBinding
import tv.caffeine.app.databinding.UpcomingButtonCardBinding
import tv.caffeine.app.di.ThemeFollowedExplore
import tv.caffeine.app.di.ThemeFollowedLobbyLight
import tv.caffeine.app.di.ThemeNotFollowedExplore
import tv.caffeine.app.di.ThemeNotFollowedLobbyLight
import tv.caffeine.app.lobby.LiveBroadcast
import tv.caffeine.app.lobby.LiveBroadcastPickerCard
import tv.caffeine.app.lobby.LobbyItem
import tv.caffeine.app.lobby.LobbyViewHolder
import tv.caffeine.app.lobby.UpcomingButtonCard
import tv.caffeine.app.lobby.UpcomingButtonItem
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.CaffeineBottomSheetDialogFragment
import tv.caffeine.app.ui.PaddingItemDecoration
import tv.caffeine.app.util.DispatchConfig
import tv.caffeine.app.util.UserTheme
import tv.caffeine.app.util.navigateToReportOrIgnoreDialog
import tv.caffeine.app.util.showSnackbar
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class LiveBroadcastPickerFragment @Inject constructor(
    private val liveBroadcastPickerAdapter: LiveBroadcastPickerAdapter,
    private val guideAdapter: GuideAdapter
) : CaffeineBottomSheetDialogFragment() {

    private var binding: FragmentLiveBroadcastPickerBinding? = null
    private val viewModel: LiveHostableBroadcastersViewModel by viewModels { viewModelFactory }

    override fun getTheme() = R.style.DarkBottomSheetDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentLiveBroadcastPickerBinding.inflate(inflater, container, false).run {
            binding = this
            configure(this)
            root
        }
    }

    override fun onDestroyView() {
        binding?.broadcastRecyclerView?.adapter = null
        binding = null
        super.onDestroyView()
    }

    private fun configure(binding: FragmentLiveBroadcastPickerBinding) {
        binding.lifecycleOwner = viewLifecycleOwner
        binding.actionBar.apply {
            applyDarkMode()
            setTitle(R.string.live_broadcast_picker_dialog_title)
            setDismissListener { dismiss() }
        }
        binding.broadcastRecyclerView.apply {
            adapter = liveBroadcastPickerAdapter.also {
                it.broadcastCardCallback = object : LiveBroadcastPickerCard.Callback {
                    override fun onPreviewImageClicked(broadcastId: String) {
                        // TODO: dismiss the dialog and start live-hosting
                        showSnackbar(R.string.broardcast_placeholder_dialog_title)
                    }

                    override fun onMoreButtonClicked(caid: CAID, username: String) {
                        findNavController().navigateToReportOrIgnoreDialog(caid, username, false)
                    }
                }
                it.upcomingButtonCallback = object : UpcomingButtonCard.Callback {
                    override fun onButtonClicked() {
                        openUpcomingBroadcastFragment()
                    }
                }
            }
            val padding = resources.getDimensionPixelSize(R.dimen.margin)
            addItemDecoration(PaddingItemDecoration(padding, 0, padding, padding))
        }
        viewModel.broadcasters.observe(viewLifecycleOwner, Observer { items ->
            liveBroadcastPickerAdapter.submitList(items)
        })
    }

    private fun openUpcomingBroadcastFragment() {
        dismiss()
        fragmentManager?.let {
            UpcomingBroadcastFragment(guideAdapter).show(it, "upcomingBroadcast")
        }
    }
}

class LiveHostableBroadcastersViewModel @Inject constructor(
    private val broadcastsService: BroadcastsService,
    private val gson: Gson
) : ViewModel() {

    private val _broadcasters = MutableLiveData<List<LobbyItem>>()
    val broadcasters: LiveData<List<LobbyItem>> = _broadcasters.map { it }

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val result = broadcastsService.liveHostableBroadcasters().awaitAndParseErrors(gson)
            when (result) {
                is CaffeineResult.Success -> {
                    _broadcasters.value = result.value.broadcasters.map {
                        LiveBroadcast(it.user.caid, it) // it.id is unavailable from this API
                    }.plus(UpcomingButtonItem("0")) // the only button
                }
                is CaffeineResult.Error -> Timber.e("Failed to fetch live-hostable broadcasters")
                is CaffeineResult.Failure -> Timber.e(result.throwable)
            }
        }
    }
}

class LiveBroadcastPickerAdapter @Inject constructor(
    private val dispatchConfig: DispatchConfig,
    private val followManager: FollowManager,
    @ThemeFollowedExplore private val followedTheme: UserTheme,
    @ThemeNotFollowedExplore private val notFollowedTheme: UserTheme,
    @ThemeFollowedLobbyLight private val followedThemeLight: UserTheme,
    @ThemeNotFollowedLobbyLight private val notFollowedThemeLight: UserTheme,
    private val picasso: Picasso,
    private val clock: Clock,
    private val eventManager: EventManager
) : ListAdapter<LobbyItem, LobbyViewHolder>(
        object : DiffUtil.ItemCallback<LobbyItem>() {
            override fun areItemsTheSame(oldItem: LobbyItem, newItem: LobbyItem) = oldItem.equals(newItem)
            override fun areContentsTheSame(oldItem: LobbyItem, newItem: LobbyItem) = oldItem.equals(newItem) // broadcasts are unique in the list
        }
), CoroutineScope {

    private val job = SupervisorJob()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "Coroutine throwable")
    }
    override val coroutineContext: CoroutineContext
        get() = dispatchConfig.main + job + exceptionHandler

    var broadcastCardCallback: LiveBroadcastPickerCard.Callback? = null
    var upcomingButtonCallback: UpcomingButtonCard.Callback? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LobbyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemType = LobbyItem.Type.values()[viewType]
        return when (itemType) {
            LobbyItem.Type.LIVE_BROADCAST_CARD -> liveBroadcastPickerCard(inflater, parent)
            else -> upcomingButtonCard(inflater, parent)
        }
    }

    override fun onBindViewHolder(holder: LobbyViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).itemType.ordinal
    }

    private fun liveBroadcastPickerCard(inflater: LayoutInflater, parent: ViewGroup) =
            LiveBroadcastPickerCard(LiveBroadcastCardBinding.inflate(inflater, parent, false), broadcastCardCallback,
                    mapOf(), mapOf(), followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight, picasso, this, clock, eventManager)

    private fun upcomingButtonCard(inflater: LayoutInflater, parent: ViewGroup) =
            UpcomingButtonCard(UpcomingButtonCardBinding.inflate(inflater, parent, false), upcomingButtonCallback,
                    mapOf(), mapOf(), followManager, followedTheme, notFollowedTheme, followedThemeLight, notFollowedThemeLight, picasso)
}
