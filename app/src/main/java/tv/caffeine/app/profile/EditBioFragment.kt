package tv.caffeine.app.profile


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import tv.caffeine.app.databinding.FragmentEditBioBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.util.dismissKeyboard

class EditBioFragment : CaffeineFragment() {

    private lateinit var binding: FragmentEditBioBinding
    private val viewModel: MyProfileViewModel by viewModels { viewModelFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentEditBioBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.userProfile.observe(viewLifecycleOwner, Observer { userProfile ->
            binding.userProfile = userProfile
        })
        binding.saveBioButton.setOnClickListener {
            context?.dismissKeyboard(it)
            val updatedBio = binding.editBioEditText.text.toString()
            viewModel.updateBio(updatedBio)
        }
    }
}
