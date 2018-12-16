package tv.caffeine.app.util

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import tv.caffeine.app.R
import tv.caffeine.app.ui.CaffeineFragment

class NoNetworkFragment : CaffeineFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_no_network, container, false)
    }

}
