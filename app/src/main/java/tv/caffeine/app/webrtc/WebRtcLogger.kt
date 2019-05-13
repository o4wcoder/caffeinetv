package tv.caffeine.app.webrtc

import org.webrtc.Loggable
import org.webrtc.Logging
import timber.log.Timber

class WebRtcLogger : Loggable {
    override fun onLogMessage(message: String?, severity: Logging.Severity?, tag: String?) {
        val logString = "$tag: $message"
        when (severity) {
            Logging.Severity.LS_VERBOSE -> Timber.v(logString)
            Logging.Severity.LS_INFO -> Timber.i(logString)
            Logging.Severity.LS_WARNING -> Timber.w(logString)
            Logging.Severity.LS_ERROR -> Timber.e(logString)
            Logging.Severity.LS_NONE -> Unit
            else -> Unit
        }
    }
}
