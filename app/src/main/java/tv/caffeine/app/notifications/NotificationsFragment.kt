package tv.caffeine.app.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import tv.caffeine.app.R
import tv.caffeine.app.di.ViewModelFactory
import javax.inject.Inject

class NotificationsFragment : Fragment() {


    @Inject lateinit var viewModelFactory: ViewModelFactory
    private val viewModelProvider by lazy { ViewModelProviders.of(this, viewModelFactory) }
    private lateinit var viewModel: NotificationsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.notifications_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = viewModelProvider.get(NotificationsViewModel::class.java)
        viewModel.followers.observe(this, Observer {
            //
        })
        viewModel.refresh()
    }

}
