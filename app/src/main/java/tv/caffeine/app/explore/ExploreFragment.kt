package tv.caffeine.app.explore


import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_explore.*
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.di.ViewModelFactory
import tv.caffeine.app.session.FollowManager
import javax.inject.Inject

class ExploreFragment : DaggerFragment() {

    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var followManager: FollowManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_explore, container, false)
    }

    lateinit var exploreAdapter: ExploreAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        exploreAdapter = ExploreAdapter(followManager)
        explore_recycler_view.layoutManager = LinearLayoutManager(context)
        explore_recycler_view.adapter = exploreAdapter
        val viewModel = ViewModelProviders.of(this, viewModelFactory).get(ExploreViewModel::class.java)
        search_edit_text.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.queryString = s.toString()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })
        viewModel.data.observe(this, Observer {
            Timber.d("Got results $it")
            exploreAdapter.submitList(it.toList())
        })
    }

}
