package tv.caffeine.app.stage


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_stage.*
import org.webrtc.EglBase
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RendererCommon
import tv.caffeine.app.R
import javax.inject.Inject

class StageFragment : DaggerFragment() {
    lateinit var accessToken: String
    lateinit var xCredential: String
    lateinit var stageIdentifier : String
    lateinit var broadcaster: String
    var primaryPeerConnection: PeerConnection? = null
    var secondaryPeerConnection: PeerConnection? = null

    @Inject lateinit var realtime: Realtime
    @Inject lateinit var peerConnectionFactory: PeerConnectionFactory
    @Inject lateinit var eglBase: EglBase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        arguments?.run {
            accessToken = getString("ACCESS_TOKEN")!!
            xCredential = getString("X_CREDENTIAL")!!
            stageIdentifier = getString("STAGE_IDENTIFIER")!!
            broadcaster = getString("BROADCASTER")!!
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_stage, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        connectStreams()

        configureSurfaceViewRenderer()
    }

    private fun configureSurfaceViewRenderer() {
        primary_view_renderer.init(eglBase.eglBaseContext, null)
        primary_view_renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        primary_view_renderer.setEnableHardwareScaler(true)
    }

    private fun connectStreams() {
        val stageHandshake = StageHandshake(accessToken, xCredential)
        val streamController = StreamController(realtime, accessToken, xCredential, peerConnectionFactory)
        stageHandshake.connect(stageIdentifier) { event ->
            val primaryStream = event.streams.find { it.type == "primary" } ?: return@connect
            streamController.connect(primaryStream) { peerConnection, videoTrack, audioTrack ->
                this.primaryPeerConnection = peerConnection
                videoTrack?.run { addSink(primary_view_renderer) }
                audioTrack?.setVolume(1.5) //TODO: make it possible to control volume
            }
            val webcamStream = event.streams.find { it.type != "primary" } ?: return@connect
            streamController.connect(webcamStream) { peerConnection, videoTrack, audioTrack ->
                this.secondaryPeerConnection = peerConnection
                videoTrack?.run { addSink(secondary_view_renderer) }
                audioTrack?.setVolume(0.5) //TODO: make it possible to control volume
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        primary_view_renderer.release()
        primaryPeerConnection?.dispose()
        secondaryPeerConnection?.dispose()
    }

}

