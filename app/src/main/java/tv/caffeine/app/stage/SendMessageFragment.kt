package tv.caffeine.app.stage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.navigation.fragment.navArgs
import tv.caffeine.app.R
import tv.caffeine.app.databinding.FragmentSendMessageBinding
import tv.caffeine.app.ui.CaffeineBottomSheetDialogFragment
import tv.caffeine.app.ui.prepopulateText
import tv.caffeine.app.ui.setOnAction
import tv.caffeine.app.util.dismissKeyboard

class SendMessageFragment : CaffeineBottomSheetDialogFragment() {

    interface Callback {
        fun sendDigitalItemWithMessage(message: String?)
        fun sendMessage(message: String?)
    }

    private lateinit var binding: FragmentSendMessageBinding
    private val callback get() = targetFragment as? Callback
    private val args by navArgs<SendMessageFragmentArgs>()

    override fun getTheme() = R.style.DarkBottomSheetDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSendMessageBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        // The following 2 lines must be executed before before we return@onCreateView().
        binding.messageEditText.requestFocus()
        dialog?.window?.setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.messageEditText.apply {
            prepopulateText(args.message)
            setOnAction(EditorInfo.IME_ACTION_SEND) { sendMessage() }
        }
        binding.sendButton.setOnClickListener { sendMessage() }
        binding.digitalItemButton.apply {
            isVisible = args.canSendDI
            setOnClickListener { sendDigitalItem() }
        }
    }

    private fun sendMessage() {
        context?.dismissKeyboard(binding.sendButton)
        val message = binding.messageEditText.text.toString()
        callback?.sendMessage(message)
        dismiss()
    }

    private fun sendDigitalItem() {
        context?.dismissKeyboard(binding.sendButton)
        val message = binding.messageEditText.text.toString()
        callback?.sendDigitalItemWithMessage(message)
        dismiss()
    }
}
