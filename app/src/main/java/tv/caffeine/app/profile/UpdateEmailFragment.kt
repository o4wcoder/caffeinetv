package tv.caffeine.app.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import tv.caffeine.app.R
import tv.caffeine.app.api.currentPasswordErrorsString
import tv.caffeine.app.api.emailErrorsString
import tv.caffeine.app.api.generalErrorsString
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.databinding.FragmentUpdateEmailBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.util.showSnackbar

class UpdateEmailFragment : CaffeineFragment() {

    private lateinit var binding: FragmentUpdateEmailBinding
    private val viewModel: UpdateProfileViewModel by viewModels { viewModelFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentUpdateEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.updateButton.setOnClickListener {
            val currentPassword = binding.currentPasswordEditText.text.toString()
            val email = binding.emailEditText.text.toString()
            viewModel.updateEmail(currentPassword, email).observe(viewLifecycleOwner, Observer { result ->
                when (result) {
                    is CaffeineResult.Success -> {
                        activity?.showSnackbar(R.string.email_sent_message)
                        findNavController().navigateUp()
                    }
                    is CaffeineResult.Error -> onError(result)
                    is CaffeineResult.Failure -> handleFailure(result)
                }
            })
        }
    }

    private fun <T> onError(result: CaffeineResult.Error<T>) {
        val error = result.error
        binding.formErrorTextView.text = error.generalErrorsString
        binding.emailTextInputLayout.error = error.emailErrorsString
        binding.currentPasswordTextInputLayout.error = error.currentPasswordErrorsString
        super.handleError(result)
    }

}
