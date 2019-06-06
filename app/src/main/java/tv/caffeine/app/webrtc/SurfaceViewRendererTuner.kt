package tv.caffeine.app.webrtc

import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject

interface SurfaceViewRendererTuner {
    fun configure(surfaceViewRenderer: SurfaceViewRenderer)
}

class StageSurfaceViewRendererTuner @Inject constructor(
    private val eglBase: EglBase
) : SurfaceViewRendererTuner {
    override fun configure(surfaceViewRenderer: SurfaceViewRenderer) {
        surfaceViewRenderer.init(eglBase.eglBaseContext, null)
        surfaceViewRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        surfaceViewRenderer.setEnableHardwareScaler(true)
    }
}
