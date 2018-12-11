package tv.caffeine.app.profile


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import tv.caffeine.app.databinding.FragmentEditBioBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.util.dismissKeyboard

class EditBioFragment : CaffeineFragment() {

    private lateinit var binding: FragmentEditBioBinding
    private val viewModel by lazy { viewModelProvider.get(MyProfileViewModel::class.java) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentEditBioBinding.inflate(inflater, container, false)
        binding.setLifecycleOwner(viewLifecycleOwner)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.viewModel = viewModel
        binding.saveBioButton.setOnClickListener {
            context?.dismissKeyboard(it)
            val updatedBio = binding.editBioEditText.text.toString()
            viewModel.updateBio(updatedBio)
        }
    }
}
