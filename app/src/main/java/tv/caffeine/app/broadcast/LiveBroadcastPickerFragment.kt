package tv.caffeine.app.broadcast

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import tv.caffeine.app.R
import tv.caffeine.app.ui.CaffeineBottomSheetDialogFragment
import tv.caffeine.app.databinding.FragmentLiveBroadcastPickerBinding

class LiveBroadcastPickerFragment : CaffeineBottomSheetDialogFragment() {

    override fun getTheme() = R.style.DarkBottomSheetDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentLiveBroadcastPickerBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.actionBar.apply {
            applyDarkMode()
            setTitle(R.string.live_broadcast_picker_dialog_title)
        }
        binding.viewUpcomingButton.setOnClickListener { openUpcomingBroadcastFragment() }
        return binding.root
    }

    private fun openUpcomingBroadcastFragment() {
        dismiss()
        fragmentManager?.let {
            UpcomingBroadcastFragment().show(it, "upcomingBroadcast")
        }
    }
}