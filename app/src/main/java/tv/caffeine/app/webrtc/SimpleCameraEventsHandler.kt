package tv.caffeine.app.webrtc

import org.webrtc.CameraVideoCapturer
import timber.log.Timber

class SimpleCameraEventsHandler : CameraVideoCapturer.CameraEventsHandler {
    override fun onCameraError(p0: String?) {
        Timber.d("Camera error $p0")
    }

    override fun onCameraOpening(p0: String?) {
        Timber.d("Camera opening $p0")
    }

    override fun onCameraDisconnected() {
        Timber.d("Camera disconnected")
    }

    override fun onCameraFreezed(p0: String?) {
        Timber.d("Camera freeze $p0")
    }

    override fun onFirstFrameAvailable() {
        Timber.d("Camera first frame available")
    }

    override fun onCameraClosed() {
        Timber.d("Camera closed")
    }
}
