package tv.caffeine.app.profile

import tv.caffeine.app.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import tv.caffeine.app.ui.CaffeineDialogFragment
import tv.caffeine.app.ui.DialogActionBar

class ReportDialogFragment : CaffeineDialogFragment() {

    private lateinit var caid: String
    private lateinit var username: String
    private var shouldNavigateBackWhenDone = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullscreenDialogTheme)
        val args = ReportDialogFragmentArgs.fromBundle(arguments)
        caid = args.caid
        username = args.username
        shouldNavigateBackWhenDone = args.shouldNavigateBackWhenDone
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_report_dialog, container, false).also { view ->
            view.findViewById<DialogActionBar>(R.id.action_bar).apply {
                setTitle(getString(R.string.report_user, username))
                setActionText(R.string.submit)
                setActionListener { reportUser(caid) }
                setDismissListener { dismiss() }
            }
            view.findViewById<TextView>(R.id.report_description).text = getString(R.string.report_description, username)
        }
    }

    private fun reportUser(caid: String) {
        // TODO (david)
    }
}