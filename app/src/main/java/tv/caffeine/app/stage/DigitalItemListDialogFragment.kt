package tv.caffeine.app.stage

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.squareup.picasso.Picasso
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.AndroidSupportInjection
import dagger.android.support.HasSupportFragmentInjector
import kotlinx.android.synthetic.main.fragment_digitalitem_list_dialog.*
import tv.caffeine.app.R
import tv.caffeine.app.api.DigitalItem
import tv.caffeine.app.databinding.FragmentDigitalitemListDialogItemBinding
import tv.caffeine.app.di.ViewModelFactory
import javax.inject.Inject

class DigitalItemListDialogFragment : BottomSheetDialogFragment(), HasSupportFragmentInjector {
    @Inject lateinit var childFragmentInjector: DispatchingAndroidInjector<Fragment>

    @Inject lateinit var viewModelFactory: ViewModelFactory
    private val viewModelProvider by lazy { ViewModelProviders.of(this, viewModelFactory) }
    private val adapter = DigitalItemAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = viewModelProvider.get(DICatalogViewModel::class.java)
        viewModel.items.observe(this, Observer {
            adapter.submitList(it.digitalItems.state)
        })
        viewModel.refresh()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_digitalitem_list_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        list.adapter = adapter
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = childFragmentInjector

    private inner class ViewHolder internal constructor(val binding: FragmentDigitalitemListDialogItemBinding)
        : RecyclerView.ViewHolder(binding.root) {

        internal val nameTextView: TextView = binding.nameTextView
        internal val costTextView: TextView = binding.goldCostTextView
        internal val previewImage: ImageView = binding.previewImageView

        fun bind(digitalItem: DigitalItem) {
            binding.digitalItem = digitalItem
            nameTextView.text = digitalItem.name
            costTextView.text = digitalItem.goldCost.toString()
            Picasso.get()
                    .load(digitalItem.staticImageUrl)
                    .into(previewImage)
        }
    }

    val diffCallback = object : DiffUtil.ItemCallback<DigitalItem?>() {
        override fun areItemsTheSame(oldItem: DigitalItem, newItem: DigitalItem) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: DigitalItem, newItem: DigitalItem) = oldItem == newItem
    }

    private inner class DigitalItemAdapter internal constructor() : ListAdapter<DigitalItem, ViewHolder>(diffCallback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = FragmentDigitalitemListDialogItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }

}
