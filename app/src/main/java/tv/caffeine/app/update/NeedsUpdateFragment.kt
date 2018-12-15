package tv.caffeine.app.update

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import tv.caffeine.app.ui.CaffeineFragment

class NeedsUpdateFragment : CaffeineFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(tv.caffeine.app.R.layout.fragment_needs_update, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val playStoreButton = view.findViewById<Button>(tv.caffeine.app.R.id.play_store_button)
        playStoreButton.setOnClickListener { openPlayStore() }
    }

    private fun openPlayStore() {
        val context = this.context ?: return
        val appId = context.packageName?.removeSuffix(".debug") ?: return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appId"))
        val playStore = context.packageManager
                ?.queryIntentActivities(intent, 0)
                ?.find { it.activityInfo.applicationInfo.packageName == "com.android.vending" }

        if (playStore != null) {
            val otherAppActivity = playStore.activityInfo
            val componentName = ComponentName(
                    otherAppActivity.applicationInfo.packageName,
                    otherAppActivity.name
            )
            // make sure it does NOT open in the stack of your activity
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // task reparenting if needed
            intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            // if the Google Play was already open in a search result
            //  this make sure it still go to the app page you requested
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // this make sure only the Google Play app is allowed to
            // intercept the intent
            intent.component = componentName
            context.startActivity(intent)
        } else {
            // if GP not present on device, open web browser
            val uri = Uri.parse("https://play.google.com/store/apps/details?id=$appId")
            val webIntent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(webIntent)
        }
    }
}
