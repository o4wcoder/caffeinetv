package tv.caffeine.app.broadcast

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import tv.caffeine.app.R
import tv.caffeine.app.ui.CaffeineBottomSheetDialogFragment

class UpcomingBroadcastFragment : CaffeineBottomSheetDialogFragment() {

    override fun getTheme() = R.style.DarkBottomSheetDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = tv.caffeine.app.databinding.FragmentUpcomingBroadcastBinding.inflate(inflater, container, false)
        binding.actionBar.apply {
            applyDarkMode()
            setTitle(R.string.upcoming_broadcasts_dialog_title)
        }
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }
}