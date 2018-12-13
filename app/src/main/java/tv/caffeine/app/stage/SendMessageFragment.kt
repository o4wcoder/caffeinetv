package tv.caffeine.app.stage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import tv.caffeine.app.R
import tv.caffeine.app.databinding.FragmentSendMessageBinding
import tv.caffeine.app.ui.CaffeineBottomSheetDialogFragment
import tv.caffeine.app.ui.prepopulateText
import tv.caffeine.app.ui.setOnAction

class SendMessageFragment : CaffeineBottomSheetDialogFragment() {

    interface Callback {
        fun sendDigitalItemWithMessage(message: String?)
        fun sendMessage(message: String?)
    }

    private lateinit var binding: FragmentSendMessageBinding
    private val callback get() = targetFragment as? Callback
    private val message by lazy { SendMessageFragmentArgs.fromBundle(arguments).message }

    override fun getTheme() = R.style.DarkBottomSheetDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSendMessageBinding.inflate(inflater, container, false)
        binding.setLifecycleOwner(viewLifecycleOwner)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.messageEditText.apply {
            prepopulateText(message)
            setOnAction(EditorInfo.IME_ACTION_SEND) { sendMessage() }
        }
        binding.sendButton.setOnClickListener { sendMessage() }
        binding.digitalItemButton.setOnClickListener { sendDigitalItem() }
    }

    private fun sendMessage() {
        val message = binding.messageEditText.text.toString()
        callback?.sendMessage(message)
        dismiss()
    }

    private fun sendDigitalItem() {
        val message = binding.messageEditText.text.toString()
        callback?.sendDigitalItemWithMessage(message)
        dismiss()
    }

}
