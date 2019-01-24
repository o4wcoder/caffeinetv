package tv.caffeine.app.stage

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import tv.caffeine.app.R

class HardwareNotSupportedFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_hardware_not_supported, container, false)
        view.findViewById<TextView>(R.id.hardware_not_supported_text_view).apply {
            text = HtmlCompat.fromHtml(getString(R.string.hardware_not_supported_message), HtmlCompat.FROM_HTML_MODE_LEGACY)
            movementMethod = LinkMovementMethod.getInstance()
        }
        return view
    }
}
