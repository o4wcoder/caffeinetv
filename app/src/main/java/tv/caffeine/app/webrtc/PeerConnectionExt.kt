package tv.caffeine.app.webrtc

import kotlinx.coroutines.suspendCancellableCoroutine
import org.webrtc.*
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun PeerConnection.setRemoteDescription(sessionDescription: SessionDescription): Boolean = suspendCancellableCoroutine { cont ->
    setRemoteDescription(object : SdpObserver {
        override fun onSetSuccess() {
            cont.resume(true)
        }

        override fun onSetFailure(errorMessage: String?) {
            Timber.e(Exception("Failed to set remote description: $errorMessage"))
            cont.resume(false)
        }

        override fun onCreateSuccess(p0: SessionDescription?) {
            cont.resumeWithException(IllegalStateException())
        }

        override fun onCreateFailure(p0: String?) {
            cont.resumeWithException(IllegalStateException())
        }
    }, sessionDescription)
}

suspend fun PeerConnection.createAnswer(mediaConstraints: MediaConstraints) : SessionDescription? = suspendCancellableCoroutine { cont ->
    createAnswer(object : SdpObserver {
        override fun onCreateSuccess(localSessionDescription: SessionDescription?) {
            cont.resume(localSessionDescription)
        }

        override fun onCreateFailure(errorMessage: String?) {
            cont.resumeWithException(Exception("Failed to create answer: $errorMessage"))
        }

        override fun onSetSuccess() {
            cont.resumeWithException(IllegalStateException())
        }

        override fun onSetFailure(p0: String?) {
            cont.resumeWithException(IllegalStateException())
        }
    }, mediaConstraints)
}

suspend fun PeerConnection.setLocalDescription(sessionDescription: SessionDescription): Boolean = suspendCancellableCoroutine { cont ->
    setLocalDescription(object : SdpObserver {
        override fun onSetSuccess() {
            cont.resume(true)
        }

        override fun onSetFailure(errorMessage: String?) {
            cont.resumeWithException(Exception("Failed to set local description: $errorMessage"))
        }

        override fun onCreateSuccess(p0: SessionDescription?) {
            cont.resumeWithException(IllegalStateException())
        }

        override fun onCreateFailure(p0: String?) {
            cont.resumeWithException(IllegalStateException())
        }
    }, sessionDescription)
}

suspend fun PeerConnection.getStats(): RTCStatsReport = suspendCancellableCoroutine { cont ->
    getStats { statsReport ->
        when(statsReport) {
            null -> cont.resumeWithException(Exception("Failed to get stats"))
            else -> cont.resume(statsReport)
        }
    }
}

