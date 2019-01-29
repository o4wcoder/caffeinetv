package tv.caffeine.app.broadcast

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import tv.caffeine.app.databinding.FragmentBroadcastBinding
import tv.caffeine.app.ui.CaffeineFragment

class BroadcastFragment : CaffeineFragment() {
    private lateinit var binding: FragmentBroadcastBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentBroadcastBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

}
