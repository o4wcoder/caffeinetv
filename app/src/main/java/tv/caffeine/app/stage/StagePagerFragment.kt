package tv.caffeine.app.stage

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.webrtc.MediaCodecVideoDecoder
import timber.log.Timber
import tv.caffeine.app.MainNavDirections
import tv.caffeine.app.R
import tv.caffeine.app.databinding.FragmentStagePagerBinding
import tv.caffeine.app.util.safeNavigate
import tv.caffeine.app.util.setDarkMode
import tv.caffeine.app.util.setImmersiveSticky
import tv.caffeine.app.util.unsetImmersiveSticky

class StagePagerFragment : Fragment(R.layout.fragment_stage_pager) {

    private val args by navArgs<StagePagerFragmentArgs>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        if (!MediaCodecVideoDecoder.isH264HwSupported()) {
            Timber.e(Exception("Failed to decode H264"))
            findNavController().safeNavigate(MainNavDirections.actionGlobalHardwareNotSupportedFragment())
            return
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.apply {
            setDarkMode(true)
            setImmersiveSticky()
        }
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentStagePagerBinding.bind(view)
        binding.stageViewPager.adapter = StagePagerAdapter(this, args.broadcastLink)
    }

    override fun onDestroyView() {
        activity?.apply {
            unsetImmersiveSticky()
            setDarkMode(false)

            getPreferences(Context.MODE_PRIVATE)?.let {
                val key = getString(R.string.is_first_time_on_stage)
                if (it.getBoolean(key, true)) {
                    // This will re-enable the immersive mode function in MainActivity.onWindowFocusChanged().
                    it.edit().putBoolean(key, false).apply()
                }
            }
        }
        super.onDestroyView()
    }

}

class StagePagerAdapter(fragment: Fragment, private val broadcasterName: String) : FragmentStateAdapter(fragment) {
    override fun getItem(position: Int): Fragment {
        val stageFragment = StageFragment()
        stageFragment.arguments = bundleOf("broadcastLink" to broadcasterName)
        return stageFragment
    }

    override fun getItemCount(): Int = 1

}
