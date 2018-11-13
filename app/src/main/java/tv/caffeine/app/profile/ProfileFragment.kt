package tv.caffeine.app.profile


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import tv.caffeine.app.R
import tv.caffeine.app.databinding.FragmentProfileBinding
import tv.caffeine.app.ui.CaffeineFragment

class ProfileFragment : CaffeineFragment() {
    private val viewModel by lazy { viewModelProvider.get(ProfileViewModel::class.java) }
    private lateinit var caid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        caid = ProfileFragmentArgs.fromBundle(arguments).caid
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val binding = FragmentProfileBinding.inflate(inflater, container, false)
        viewModel.load(caid)
        binding.profileViewModel = viewModel
        binding.setLifecycleOwner(viewLifecycleOwner)
        binding.numberFollowingTextView.setOnClickListener { showFollowingList() }
        binding.numberOfFollowersTextView.setOnClickListener { showFollowersList() }
        binding.stageImageView.setOnClickListener { watchBroadcast() }
        return binding.root
    }

    private fun showFollowingList() {
        val action = ProfileFragmentDirections.actionProfileFragmentToFollowingFragment(caid)
        findNavController().navigate(action)
    }

    private fun showFollowersList() {
        val action = ProfileFragmentDirections.actionProfileFragmentToFollowersFragment(caid)
        findNavController().navigate(action)
    }

    private fun watchBroadcast() {
        val action = ProfileFragmentDirections.actionProfileFragmentToStageFragment(caid)
        findNavController().navigate(action, NavOptions.Builder().setPopUpTo(R.id.lobbyFragment, false).build())
    }

}
