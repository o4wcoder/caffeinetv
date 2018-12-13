package tv.caffeine.app.stage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.BuyDigitalItemBody
import tv.caffeine.app.api.DigitalItem
import tv.caffeine.app.api.PaymentsClientService
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.databinding.FragmentSendDigitalItemBinding
import tv.caffeine.app.profile.WalletViewModel
import tv.caffeine.app.ui.*
import tv.caffeine.app.util.DispatchConfig
import tv.caffeine.app.wallet.DigitalItemRepository
import java.text.NumberFormat

class SendDigitalItemFragment : CaffeineBottomSheetDialogFragment() {
    private lateinit var binding: FragmentSendDigitalItemBinding

    private lateinit var digitalItemId: String
    private lateinit var recipientCaid: String
    private var message: String? = null

    private val walletViewModel by lazy { viewModelProvider.get(WalletViewModel::class.java) }
    private val sendDigitalItemViewModel by lazy { viewModelProvider.get(SendDigitalItemViewModel::class.java) }

    override fun getTheme() = R.style.DarkBottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = SendDigitalItemFragmentArgs.fromBundle(arguments)
        digitalItemId = args.digitalItemId
        recipientCaid = args.recipientCaid
        message = args.message
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSendDigitalItemBinding.inflate(inflater, container, false)
        binding.setLifecycleOwner(viewLifecycleOwner)
        return binding.root
    }

    private var itemGoldCost: Int = 0
    private var quantity: Int = 1
    private val total get() = itemGoldCost * quantity

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.messageEditText.prepopulateText(message)
        sendDigitalItemViewModel.load(digitalItemId).observe(viewLifecycleOwner, Observer { digitalItem ->
            itemGoldCost = digitalItem.goldCost
            binding.goldCostTextView.text = total.toString()
            Picasso.get().load(digitalItem.staticImageUrl).into(binding.diImageView)
            binding.messageEditText.setOnAction(EditorInfo.IME_ACTION_SEND) { sendDigitalItem(digitalItem) }
            binding.sendButton.setOnClickListener { sendDigitalItem(digitalItem) }
        })
        walletViewModel.wallet.observe(viewLifecycleOwner, Observer { wallet ->
            val numberFormat = NumberFormat.getIntegerInstance()
            binding.walletBalanceTextView.htmlText = getString(R.string.wallet_balance, numberFormat.format(wallet.gold))
        })
        binding.diQuantityNumberPicker.apply {
            minValue = 1
            maxValue = 9
            wrapSelectorWheel = false
            setOnValueChangedListener { _, _, newVal ->
                quantity = newVal
                binding.goldCostTextView.text = total.toString()
            }
        }
    }

    private fun sendDigitalItem(digitalItem: DigitalItem) {
        val message = binding.messageEditText.text.toString()
        sendDigitalItemViewModel.send(digitalItem, quantity, recipientCaid, message)
        dismiss()
    }

}

class SendDigitalItemViewModel(
        dispatchConfig: DispatchConfig,
        private val gson: Gson,
        private val digitalItemRepository: DigitalItemRepository,
        private val paymentsClientService: PaymentsClientService
) : CaffeineViewModel(dispatchConfig) {

    init {
        digitalItemRepository.refresh()
    }

    fun load(digitalItemId: String): LiveData<DigitalItem> {
        return Transformations.map(digitalItemRepository.items) { payload ->
            payload.digitalItems.state.find {
                it.id == digitalItemId
            }
        }
    }

    fun send(digitalItem: DigitalItem, quantity: Int, recipientCaid: String, message: String) = launch {
        val body = BuyDigitalItemBody(digitalItem.id, quantity, recipientCaid, message)
        val result = paymentsClientService.buyDigitalItem(body).awaitAndParseErrors(gson)
        when(result) {
            is CaffeineResult.Success -> Timber.d("Successfully sent a digital item")
            is CaffeineResult.Error -> Timber.e(Exception(result.error.toString()))
            is CaffeineResult.Failure -> Timber.e(result.throwable)
        }
        // TODO show errors
    }

}
