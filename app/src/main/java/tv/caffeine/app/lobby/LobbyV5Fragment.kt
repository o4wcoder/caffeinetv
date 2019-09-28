package tv.caffeine.app.lobby

import android.os.Bundle
import android.view.View
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tv.caffeine.app.MainActivity
import tv.caffeine.app.R
import tv.caffeine.app.databinding.FragmentLobbyBinding
import tv.caffeine.app.lobby.classic.LobbyViewHolder
import tv.caffeine.app.lobby.release.LargeOnlineBroadcasterCard
import tv.caffeine.app.lobby.release.ReleaseLobbyAdapter
import tv.caffeine.app.notifications.NotificationCountViewModel
import tv.caffeine.app.ui.CaffeineFragment
import java.util.concurrent.TimeUnit
import javax.inject.Provider

abstract class LobbyV5Fragment constructor(
    private val releaseLobbyAdapterFactoryProvider: Provider<ReleaseLobbyAdapter.Factory>
) : CaffeineFragment(R.layout.fragment_lobby) {

    protected val viewModel: LobbyViewModel by viewModels { viewModelFactory }
    private val notificationCountViewModel: NotificationCountViewModel by viewModels { viewModelFactory }
    private var binding: FragmentLobbyBinding? = null
    private var refreshJob: Job? = null
    private lateinit var lobbyAdapter: ReleaseLobbyAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentLobbyBinding.bind(view)
        configure(binding)
        this.binding = binding
        observeNotificationsCount()
    }

    abstract fun loadLobby()

    private fun configure(binding: FragmentLobbyBinding) {
        lobbyAdapter = releaseLobbyAdapterFactoryProvider.get().create(viewLifecycleOwner, findNavController())
        val itemDecorator = ReleaseLobbyItemDecoration(resources)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.lobbyRecyclerView.run {
            adapter = lobbyAdapter
            addItemDecoration(itemDecorator)
        }
        binding.lobbySwipeRefreshLayout.setOnRefreshListener { refreshLobby() }
        binding.lobbyRecyclerView.setRecyclerListener { viewHolder ->
            when (viewHolder) {
                is LobbyViewHolder -> viewHolder.recycle()
                is LargeOnlineBroadcasterCard -> viewHolder.turnOffLiveVideo()
            }
        }
        viewModel.lobbyV5.observe(viewLifecycleOwner, Observer { result ->
            binding.lobbySwipeRefreshLayout.isRefreshing = false
            handle(result) { lobby ->
                val items = LobbyItem.parse(lobby)
                lobbyAdapter.submitList(items, mapOf(), mapOf(), "")
                binding.lobbyLoadingIndicator.isVisible = false
            }
        })
        viewModel.lobbyDetail.observe(viewLifecycleOwner, Observer { result ->
            binding.lobbySwipeRefreshLayout.isRefreshing = false
            handle(result) { detailPage ->
                val items = LobbyItem.parse(detailPage)
                lobbyAdapter.submitList(items, mapOf(), mapOf(), "")
                binding.lobbyLoadingIndicator.isVisible = false
            }
        })
        viewModel.emailVerificationUser.observe(viewLifecycleOwner, Observer { user ->
            val email = user.email ?: return@Observer
            binding.verifyEmailContainer.isVisible = (user.emailVerified == false)
            binding.resendEmailButton.setOnClickListener {
                viewModel.sendVerificationEmail()
                binding.verifyEmailTextView.text = getString(R.string.sending_verification_email_message, email)
                it.isInvisible = true
            }
        })
    }

    override fun onDestroyView() {
        binding?.lobbyRecyclerView?.adapter = null
        binding = null
        super.onDestroyView()
    }

    private fun refreshLobby() {
        refreshJob?.cancel()
        refreshJob = launch {
            while (isActive) {
                loadLobby()
                delay(TimeUnit.SECONDS.toMillis(30))
            }
        }
    }

    protected fun manageNotificationCount() {
        notificationCountViewModel.checkNewNotifications()
    }

    private fun observeNotificationsCount() {
        notificationCountViewModel.hasNewNotifications.observe(viewLifecycleOwner, Observer { event ->
            event.getContentIfNotHandled()?.let { hasNewNotifications -> (activity as MainActivity).binding.releaseAppBar.showNewActivityIcon(hasNewNotifications) }
        })
    }

    override fun onStart() {
        super.onStart()
        refreshLobby()
    }

    override fun onStop() {
        super.onStop()
        refreshJob?.cancel()
        refreshJob = null
    }
}
