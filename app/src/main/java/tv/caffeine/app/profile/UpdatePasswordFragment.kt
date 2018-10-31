package tv.caffeine.app.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.databinding.FragmentUpdatePasswordBinding
import tv.caffeine.app.ui.CaffeineFragment

class UpdatePasswordFragment : CaffeineFragment() {
    private lateinit var binding: FragmentUpdatePasswordBinding
    private val viewModel by lazy { viewModelProvider.get(UpdateProfileViewModel::class.java) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentUpdatePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.updateButton.setOnClickListener {
            val currentPassword = binding.currentPasswordEditText.text.toString()
            val password1 = binding.password1EditText.text.toString()
            val password2 = binding.password2EditText.text.toString()
            viewModel.updatePassword(currentPassword, password1, password2).observe(viewLifecycleOwner, Observer { result ->
                when (result) {
                    is CaffeineResult.Success -> findNavController().navigateUp()
                    is CaffeineResult.Error -> onError(result, view)
                    is CaffeineResult.Failure -> handleFailure(result, view)
                }
            })
        }
    }

    private fun <T> onError(result: CaffeineResult.Error<T>, view: View) {
        val error = result.error
        error.errors._error?.joinToString("\n")?.let { binding.formErrorTextView.text = it }
        error.errors.password?.joinToString("\n")?.let { binding.password1TextInputLayout.error = it }
        error.errors.currentPassword?.joinToString("\n")?.let { binding.currentPasswordTextInputLayout.error = it }
        super.handleError(result, view)
    }

}
