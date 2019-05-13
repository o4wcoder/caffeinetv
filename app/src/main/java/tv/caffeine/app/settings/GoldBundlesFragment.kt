package tv.caffeine.app.settings

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import com.squareup.picasso.Picasso
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.GoldBundle
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.databinding.FragmentGoldBundlesBinding
import tv.caffeine.app.ui.AlertDialogFragment
import tv.caffeine.app.ui.CaffeineBottomSheetDialogFragment
import tv.caffeine.app.ui.formatUsernameAsHtml
import tv.caffeine.app.util.maybeShow
import tv.caffeine.app.util.showSnackbar
import java.text.NumberFormat
import javax.inject.Inject

class GoldBundlesFragment @Inject constructor(
    private val picasso: Picasso
) : CaffeineBottomSheetDialogFragment(), BuyGoldUsingCreditsDialogFragment.Callback {

    private lateinit var binding: FragmentGoldBundlesBinding
    private val viewModel: GoldBundlesViewModel by activityViewModels { viewModelFactory }
    private val goldBundlesAdapter by lazy {
        GoldBundlesAdapter(buyGoldOption, picasso, object : GoldBundleClickListener {
            override fun onClick(goldBundle: GoldBundle) {
                purchaseGoldBundle(goldBundle)
            }
        })
    }

    private var availableCredits = 0
    private val args by navArgs<GoldBundlesFragmentArgs>()
    private val buyGoldOption by lazy { args.buyGoldOption }

    override fun getTheme() = R.style.DarkBottomSheetDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentGoldBundlesBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            goldBundlesRecyclerView.adapter = goldBundlesAdapter
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.wallet.observe(viewLifecycleOwner, Observer { wallet ->
            availableCredits = wallet.credits
            val goldCount = NumberFormat.getIntegerInstance().format(wallet.gold)
            val creditBalance = NumberFormat.getIntegerInstance().format(wallet.credits)
            binding.currentBalanceTextView.formatUsernameAsHtml(picasso, when (buyGoldOption) {
                BuyGoldOption.UsingCredits -> getString(R.string.you_have_gold_and_credits_balance, goldCount, creditBalance)
                BuyGoldOption.UsingPlayStore -> getString(R.string.you_have_gold_balance, goldCount)
            })
        })
        val handler = Handler()
        viewModel.getGoldBundles(buyGoldOption).observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is CaffeineResult.Success -> handler.post { goldBundlesAdapter.submitList(result.value) }
                else -> showSnackbar(R.string.error_loading_gold_bundles)
            }
        })
        viewModel.events.observe(viewLifecycleOwner, Observer { event ->
            val purchaseStatus = event.getContentIfNotHandled() ?: return@Observer
            when (purchaseStatus) {
                is PurchaseStatus.GooglePlaySuccess -> {
                    Timber.d("Successfully processed purchase")
                    showSnackbar(R.string.success_buying_gold_using_in_app_billing)
                }
                is PurchaseStatus.GooglePlayError -> {
                    Timber.e(Exception("Error purchasing gold via Google Play ${purchaseStatus.responseCode}"))
                    showSnackbar(R.string.failure_buying_gold_using_in_app_billing)
                }
                is PurchaseStatus.CanceledByUser -> {
                    Timber.d("Purchase flow canceled by user")
                    showSnackbar(R.string.user_cancel_buying_gold_using_in_app_billing)
                }
                is PurchaseStatus.Error -> {
                    Timber.e("Failed to process purchase ${getString(purchaseStatus.error)}")
                    showSnackbar(R.string.failure_processing_in_app_purchase)
                }
                is PurchaseStatus.CreditsSuccess -> {
                    Timber.d("Successfully purchased gold using credits")
                    showSnackbar(R.string.success_buying_gold_using_credits)
                }
            }.toString() // TODO: hack to force exhaustiveness
        })
    }

    private fun purchaseGoldBundle(goldBundle: GoldBundle) {
        // TODO wallet balance check
        when (buyGoldOption) {
            BuyGoldOption.UsingCredits -> {
                if (goldBundle.usingCredits?.canPurchase == true) {
                    if (availableCredits >= goldBundle.usingCredits.cost) {
                        promptPurchaseGoldBundleUsingCredits(goldBundle)
                    } else {
                        AlertDialogFragment.withMessage(R.string.cannot_purchase_not_enough_credits).maybeShow(fragmentManager, "cannotPurchase")
                    }
                } else {
                    AlertDialogFragment.withMessage(R.string.cannot_purchase_using_credits).maybeShow(fragmentManager, "cannotPurchase")
                }
            }
            BuyGoldOption.UsingPlayStore -> {
                if (goldBundle.usingInAppBilling?.canPurchase == true) {
                    purchaseGoldBundleUsingPlayStore(goldBundle)
                } else {
                    AlertDialogFragment.withMessage(R.string.cannot_purchase_using_play_store).maybeShow(fragmentManager, "cannotPurchase")
                }
            }
        }
    }

    private fun promptPurchaseGoldBundleUsingCredits(goldBundle: GoldBundle) {
        val usingCredits = goldBundle.usingCredits ?: return
        val goldBundleId = usingCredits.id
        val gold = goldBundle.amount
        val credits = usingCredits.cost
        val action = GoldBundlesFragmentDirections.actionGoldBundlesFragmentToBuyGoldUsingCreditsDialogFragment(goldBundleId, gold, credits)
        val fragment = BuyGoldUsingCreditsDialogFragment()
        fragment.arguments = action.arguments
        fragment.setTargetFragment(this, 0)
        fragment.maybeShow(fragmentManager, "buyGoldUsingCredits")
    }

    override fun buyGoldBundle(goldBundleId: String) {
        viewModel.purchaseGoldBundleUsingCredits(goldBundleId)
    }

    private fun purchaseGoldBundleUsingPlayStore(goldBundle: GoldBundle) {
        val activity = activity ?: return
        viewModel.purchaseGoldBundleUsingPlayStore(activity, goldBundle)
    }
}
