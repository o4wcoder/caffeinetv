package tv.caffeine.app.profile


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import tv.caffeine.app.databinding.FragmentProfileBinding
import tv.caffeine.app.ui.CaffeineFragment

class ProfileFragment : CaffeineFragment() {
    private val viewModel by lazy { viewModelProvider.get(ProfileViewModel::class.java) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val binding = FragmentProfileBinding.inflate(inflater, container, false)

        val caid = ProfileFragmentArgs.fromBundle(arguments).caid
        viewModel.load(caid)
        binding.profileViewModel = viewModel
        binding.setLifecycleOwner(viewLifecycleOwner)
        return binding.root
    }

}
