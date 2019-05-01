package tv.caffeine.app.profile


import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import tv.caffeine.app.R
import tv.caffeine.app.databinding.FragmentEditBioBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.util.dismissKeyboard

class EditBioFragment : CaffeineFragment(R.layout.fragment_edit_bio) {

    private lateinit var binding: FragmentEditBioBinding
    private val viewModel: MyProfileViewModel by viewModels { viewModelFactory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentEditBioBinding.bind(view)
        binding.lifecycleOwner = viewLifecycleOwner
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
