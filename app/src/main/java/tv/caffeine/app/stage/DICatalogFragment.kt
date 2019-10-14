package tv.caffeine.app.stage

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import tv.caffeine.app.R
import tv.caffeine.app.api.DigitalItem
import tv.caffeine.app.databinding.DiCatalogItemBinding
import tv.caffeine.app.databinding.FragmentDiCatalogBinding
import tv.caffeine.app.settings.BuyGoldOption
import tv.caffeine.app.settings.GoldBundlesFragment
import tv.caffeine.app.settings.ReleaseDesignConfig
import tv.caffeine.app.ui.AlertDialogFragment
import tv.caffeine.app.ui.AlertDialogViewModel
import tv.caffeine.app.ui.CaffeineBottomSheetDialogFragment
import tv.caffeine.app.util.maybeShow
import tv.caffeine.app.wallet.WalletViewModel
import java.text.NumberFormat
import javax.inject.Inject

class DICatalogFragment @Inject constructor(
    private val picasso: Picasso,
    private val releaseDesignConfig: ReleaseDesignConfig
) : CaffeineBottomSheetDialogFragment(), AlertDialogFragment.Callbacks {

    interface Callback {
        fun digitalItemSelected(digitalItem: DigitalItem, message: String? = null)
        fun onDismissMessageDialog()
    }

    private val callback get() = targetFragment as? Callback
    private val args by navArgs<DICatalogFragmentArgs>()

    private val adapter by lazy {
        DigitalItemAdapter(picasso, object : DigitalItemViewHolder.Callback {
            override fun digitalItemSelected(digitalItem: DigitalItem) {
                if (alertDialogViewModel.isUserVerified()) {
                    val message = args.message
                    callback?.digitalItemSelected(digitalItem, message)
                    dismiss()
                } else {
                    alertDialogViewModel.showVerifyEmailDialog(this@DICatalogFragment, fragmentManager)
                }
            }
        })
    }
    private val viewModel: DICatalogViewModel by viewModels { viewModelFactory }
    private val walletViewModel: WalletViewModel by viewModels { viewModelFactory }
    private val alertDialogViewModel: AlertDialogViewModel by viewModels { viewModelFactory }
    private lateinit var binding: FragmentDiCatalogBinding

    override fun getTheme() = if (releaseDesignConfig.isReleaseDesignActive()) {
        R.style.DarkBottomSheetDialog_Release
    } else {
        R.style.DarkBottomSheetDialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDiCatalogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        walletViewModel.wallet.observe(viewLifecycleOwner, Observer { wallet ->
            val numberFormat = NumberFormat.getIntegerInstance()
            binding.walletBalanceTextView.text = numberFormat.format(wallet.gold)
        })
        binding.username = args.broadcasterUsername
        binding.list.adapter = adapter
        binding.lifecycleOwner = viewLifecycleOwner
        viewModel.items.observe(viewLifecycleOwner, Observer {
            adapter.submitList(it.digitalItems.state)
        })
        binding.buyGoldButton.setOnClickListener(buyGoldListener)
        if (releaseDesignConfig.isReleaseDesignActive()) {
            binding.buyGoldButton.text = getString(R.string.buy_gold_lowercase)
            binding.diCatalogTitle.text = getString(R.string.send_gold_to_user_lowercase, binding.username)
        } else {
            binding.diCatalogTitle.text = getString(R.string.send_gold_to_user, binding.username)
        }
    }

    private var buyGoldListener = View.OnClickListener {
        if (alertDialogViewModel.isUserVerified()) {
            val action = DICatalogFragmentDirections.actionDigitalItemListDialogFragmentToGoldBundlesFragment(BuyGoldOption.UsingPlayStore, true)
            val fragment = GoldBundlesFragment(picasso, releaseDesignConfig)
            fragment.arguments = action.arguments
            fragment.maybeShow(fragmentManager, "buyGold")
        } else {
            alertDialogViewModel.showVerifyEmailDialog(this, fragmentManager)
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        callback?.onDismissMessageDialog()
    }

    override fun onResendEmail() {
        alertDialogViewModel.resendEmail()
    }
}

private class DigitalItemViewHolder(
    val binding: DiCatalogItemBinding,
    val picasso: Picasso,
    val callback: Callback
) : RecyclerView.ViewHolder(binding.root) {

    private val numberFormat = NumberFormat.getInstance()

    interface Callback {
        fun digitalItemSelected(digitalItem: DigitalItem)
    }

    fun bind(digitalItem: DigitalItem) {
        binding.digitalItem = digitalItem
        binding.nameTextView.text = digitalItem.name
        binding.goldCostTextView.text = numberFormat.format(digitalItem.goldCost)
        picasso
                .load(digitalItem.staticImageUrl)
                .into(binding.previewImageView)
        itemView.setOnClickListener {
            callback.digitalItemSelected(digitalItem)
        }
    }
}

private class DigitalItemAdapter(
    private val picasso: Picasso,
    private val callback: DigitalItemViewHolder.Callback
) : ListAdapter<DigitalItem, DigitalItemViewHolder>(
        object : DiffUtil.ItemCallback<DigitalItem>() {
            override fun areItemsTheSame(oldItem: DigitalItem, newItem: DigitalItem) = oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: DigitalItem, newItem: DigitalItem) = oldItem == newItem
        }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DigitalItemViewHolder {
        val binding = DiCatalogItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DigitalItemViewHolder(binding, picasso, callback)
    }

    override fun onBindViewHolder(holder: DigitalItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
