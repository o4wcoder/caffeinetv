package tv.caffeine.app.stage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.navArgs
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.BuyDigitalItemBody
import tv.caffeine.app.api.DigitalItem
import tv.caffeine.app.api.PaymentsClientService
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.databinding.FragmentSendDigitalItemBinding
import tv.caffeine.app.settings.ReleaseDesignConfig
import tv.caffeine.app.ui.CaffeineBottomSheetDialogFragment
import tv.caffeine.app.ui.prepopulateText
import tv.caffeine.app.ui.setOnAction
import tv.caffeine.app.wallet.DigitalItemRepository
import tv.caffeine.app.wallet.WalletViewModel
import java.text.NumberFormat
import javax.inject.Inject

class SendDigitalItemFragment @Inject constructor(
    private val picasso: Picasso,
    private val releaseDesignConfig: ReleaseDesignConfig
) : CaffeineBottomSheetDialogFragment() {

    private lateinit var binding: FragmentSendDigitalItemBinding
    private lateinit var digitalItemId: String
    private lateinit var recipientCaid: CAID
    private var message: String? = null

    private val walletViewModel: WalletViewModel by viewModels { viewModelFactory }
    private val sendDigitalItemViewModel: SendDigitalItemViewModel by viewModels { viewModelFactory }
    private val args by navArgs<SendDigitalItemFragmentArgs>()

    override fun getTheme() = if (releaseDesignConfig.isReleaseDesignActive()) {
        R.style.DarkBottomSheetDialog_Release
    } else {
        R.style.DarkBottomSheetDialog
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        digitalItemId = args.digitalItemId
        recipientCaid = args.recipientCaid
        message = args.message
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSendDigitalItemBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    private val numberFormat = NumberFormat.getInstance()
    private var itemGoldCost: Int = 0
    private var quantity: Int = 1
    private var walletBalance: Int = 0
    private val total get() = itemGoldCost * quantity

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.messageEditText.apply {
            prepopulateText(message)
            if (releaseDesignConfig.isReleaseDesignActive()) {
                hint = getString(R.string.message_lowercase)
            }
        }
        sendDigitalItemViewModel.load(digitalItemId).observe(viewLifecycleOwner, Observer { digitalItem ->
            itemGoldCost = digitalItem.goldCost
            binding.goldCostTextView.text = numberFormat.format(total)
            picasso.load(digitalItem.staticImageUrl).into(binding.diImageView)
            binding.messageEditText.setOnAction(EditorInfo.IME_ACTION_SEND) { sendDigitalItem(digitalItem) }
            binding.sendButton.isVisible = !releaseDesignConfig.isReleaseDesignActive()
            binding.sendButton.setOnClickListener { sendDigitalItem(digitalItem) }
            binding.sendTextView.isVisible = releaseDesignConfig.isReleaseDesignActive()
            binding.sendTextView.setOnClickListener { sendDigitalItem(digitalItem) }
            binding.diImageView.contentDescription = digitalItem.name
            checkAbilityToPurchase()
        })
        walletViewModel.wallet.observe(viewLifecycleOwner, Observer { wallet ->
            walletBalance = wallet.gold
            val numberFormat = NumberFormat.getIntegerInstance()
            binding.walletBalanceTextView.text = numberFormat.format(wallet.gold)
            checkAbilityToPurchase()
        })
        binding.diQuantityNumberPicker.apply {
            minValue = 1
            maxValue = 99
            wrapSelectorWheel = false
            setOnValueChangedListener { _, _, newVal ->
                quantity = newVal
                binding.goldCostTextView.text = numberFormat.format(total)
                checkAbilityToPurchase()
            }
        }
    }

    private fun checkAbilityToPurchase() {
        binding.sendButton.isEnabled = walletBalance >= total
    }

    private fun sendDigitalItem(digitalItem: DigitalItem) {
        val message = binding.messageEditText.text.toString()
        sendDigitalItemViewModel.send(digitalItem, quantity, recipientCaid, message)
        dismiss()
    }
}

class SendDigitalItemViewModel @Inject constructor(
    private val gson: Gson,
    private val digitalItemRepository: DigitalItemRepository,
    private val paymentsClientService: PaymentsClientService
) : ViewModel() {

    init {
        digitalItemRepository.refresh()
    }

    fun load(digitalItemId: String): LiveData<DigitalItem> {
        return digitalItemRepository.items.switchMap { payload ->
            liveData {
                payload.digitalItems.state.find { digitalItem ->
                    digitalItem.id == digitalItemId
                }?.let { emit(it) }
            }
        }
    }

    fun send(digitalItem: DigitalItem, quantity: Int, recipientCaid: CAID, message: String) = viewModelScope.launch {
        val body = BuyDigitalItemBody(digitalItem.id, quantity, recipientCaid, message)
        val result = paymentsClientService.buyDigitalItem(body).awaitAndParseErrors(gson)
        when (result) {
            is CaffeineResult.Success -> Timber.d("Successfully sent a digital item")
            is CaffeineResult.Error -> Timber.e(result.error.toString())
            is CaffeineResult.Failure -> Timber.e(result.throwable)
        }
        // TODO show errors
    }
}
