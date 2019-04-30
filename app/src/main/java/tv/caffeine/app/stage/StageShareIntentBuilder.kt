package tv.caffeine.app.stage

import android.content.Intent
import android.content.res.Resources
import org.threeten.bp.Clock
import tv.caffeine.app.R
import tv.caffeine.app.profile.UserProfile
import java.util.concurrent.TimeUnit

/**
 * Share other's stage.
 * TODO: implement sharing the user's own stage.
 */

private const val stageUrlPrefix = "https://www.caffeine.tv"

class StageShareIntentBuilder(val userProfile: UserProfile, val sharerId: String?, val resources: Resources, val clock: Clock) {

    fun build(): Intent = Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, buildShareText())
                type = "text/plain"
            }, resources.getString(R.string.share_chooser_title))

    private fun buildShareText(): String {
        val username = userProfile.username
        val broadcastName = userProfile.broadcastName
        val twitterUsername = userProfile.twitterUsername
        val url = buildShareUrl()
        return when {
            twitterUsername != null -> resources.getString(R.string.share_others_stage_with_twitter_username,
                    username, broadcastName, twitterUsername, url)
            else -> resources.getString(R.string.share_others_stage_without_twitter_username,
                    username, broadcastName, url)
        }
    }

    private fun buildShareUrl(): String {
        val timestamp = TimeUnit.MILLISECONDS.toSeconds(clock.millis())
        val url = "$stageUrlPrefix/${userProfile.username}?bst=true&share_time=$timestamp"
        return if (sharerId == null) url else "$url&sharer_id=$sharerId"
    }
}

