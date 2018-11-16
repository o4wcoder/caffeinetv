package tv.caffeine.app.webrtc

import kotlinx.coroutines.suspendCancellableCoroutine
import org.webrtc.*

suspend fun PeerConnection.setRemoteDescription(sessionDescription: SessionDescription): Boolean = suspendCancellableCoroutine { cont ->
    setRemoteDescription(object : SdpObserver {
        override fun onSetSuccess() {
            cont.resumeWith(Result.success(true))
        }

        override fun onSetFailure(errorMessage: String?) {
            cont.resumeWith(Result.failure(Exception("Failed to set remote description $errorMessage")))
        }

        override fun onCreateSuccess(p0: SessionDescription?) {
            cont.resumeWith(Result.failure(IllegalStateException()))
        }

        override fun onCreateFailure(p0: String?) {
            cont.resumeWith(Result.failure(IllegalStateException()))
        }
    }, sessionDescription)
}

suspend fun PeerConnection.createAnswer(mediaConstraints: MediaConstraints) : SessionDescription? = suspendCancellableCoroutine { cont ->
    createAnswer(object : SdpObserver {
        override fun onCreateSuccess(localSessionDescription: SessionDescription?) {
            cont.resumeWith(Result.success(localSessionDescription))
        }

        override fun onCreateFailure(errorMessage: String?) {
            cont.resumeWith(Result.failure(Exception("Failed to create answer $errorMessage")))
        }

        override fun onSetSuccess() {
            cont.resumeWith(Result.failure(IllegalStateException()))
        }

        override fun onSetFailure(p0: String?) {
            cont.resumeWith(Result.failure(IllegalStateException()))
        }
    }, mediaConstraints)
}

suspend fun PeerConnection.setLocalDescription(sessionDescription: SessionDescription): Boolean = suspendCancellableCoroutine { cont ->
    setLocalDescription(object : SdpObserver {
        override fun onSetSuccess() {
            cont.resumeWith(Result.success(true))
        }

        override fun onSetFailure(errorMessage: String?) {
            cont.resumeWith(Result.failure(Exception("Failed to set local description $errorMessage")))
        }

        override fun onCreateSuccess(p0: SessionDescription?) {
            cont.resumeWith(Result.failure(IllegalStateException()))
        }

        override fun onCreateFailure(p0: String?) {
            cont.resumeWith(Result.failure(IllegalStateException()))
        }
    }, sessionDescription)
}

suspend fun PeerConnection.getStats(): RTCStatsReport = suspendCancellableCoroutine { cont ->
    getStats { statsReport ->
        when(statsReport) {
            null -> cont.resumeWith(Result.failure(Exception("Failed to get stats")))
            else -> cont.resumeWith(Result.success(statsReport))
        }
    }
}

