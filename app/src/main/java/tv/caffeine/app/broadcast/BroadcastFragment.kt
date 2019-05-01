package tv.caffeine.app.broadcast

import android.Manifest
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerationAndroid
import org.webrtc.CameraEnumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.EglBase
import org.webrtc.PeerConnectionFactory
import org.webrtc.Size
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoTrack
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.databinding.FragmentBroadcastBinding
import tv.caffeine.app.ui.AlertDialogFragment
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.util.showSnackbar
import tv.caffeine.app.webrtc.SimpleCameraEventsHandler
import javax.inject.Inject

private const val PICK_LIVE_BROADCAST = 0
private const val RC_BROADCAST_PERMISSIONS = 0
private val broadcastPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

private const val VIDEO_WIDTH = 1920
private const val VIDEO_HEIGHT = 1080
private const val VIDEO_FPS = 30

class BroadcastFragment : CaffeineFragment(R.layout.fragment_broadcast), EasyPermissions.PermissionCallbacks {
    @Inject lateinit var eglBase: EglBase
    private val eglBaseContext by lazy { eglBase.eglBaseContext }
    private val surfaceTextureHelper by lazy { SurfaceTextureHelper.create("captureHelper", eglBaseContext) }

    private lateinit var binding: FragmentBroadcastBinding
    private lateinit var cameraEnumerator: CameraEnumerator

    @Inject lateinit var peerConnectionFactory: PeerConnectionFactory
    @Inject lateinit var application: Application

    private var cameraCapture: CameraVideoCapturer? = null
    private var videoTrack: VideoTrack? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentBroadcastBinding.bind(view)
        binding.lifecycleOwner = viewLifecycleOwner
        if (!Camera2Enumerator.isSupported(context)) {
            AlertDialogFragment.withMessage(R.string.camera2_api_not_supported)
            return Timber.e("Camera2 API not supported")
        }

        cameraAndMicrophonePermissionsRequired()
        binding.toggleCameraButton.setOnClickListener {
            toggleCamera()
        }
        binding.liveHostButton.setOnClickListener {
            openLiveBroadcastPicker()
        }
    }

    private fun toggleCamera() {
        cameraCapture?.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
            override fun onCameraSwitchDone(isFrontCamera: Boolean) {
                Timber.d("Camera switched to ${if (isFrontCamera) "front" else "back"}")
            }

            override fun onCameraSwitchError(errorDescription: String?) {
                showSnackbar(R.string.could_not_switch_camera)
                Timber.e("Could not switch camera: $errorDescription")
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.primaryViewRenderer.release()
        binding.secondaryViewRenderer.release()
        cameraCapture?.run {
            stopCapture()
            dispose()
        }
        videoTrack?.run {
            removeSink(binding.primaryViewRenderer)
            dispose()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            // TODO user returned from the app settings screen
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        cameraEnumerator = Camera2Enumerator(context)
    }

    private fun hasAllPermissions() = context?.let { EasyPermissions.hasPermissions(it, *broadcastPermissions) } ?: false

    @AfterPermissionGranted(RC_BROADCAST_PERMISSIONS)
    private fun cameraAndMicrophonePermissionsRequired() {
        if (hasAllPermissions()) {
            setupCamera()
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.camera_microphone_permissions_rationale), RC_BROADCAST_PERMISSIONS, *broadcastPermissions)
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        Timber.d("onPermissionsGranted: $requestCode: $perms")
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        Timber.d("onPermissionsDenied: $requestCode: $perms")
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        }
    }

    private fun setupCamera() {
        val deviceNames = cameraEnumerator.deviceNames.toList()
        Timber.d("Devices available: $deviceNames")
        deviceNames.forEach { deviceName ->
            val isFrontFacing = cameraEnumerator.isFrontFacing(deviceName)
            val isBackFacing = cameraEnumerator.isBackFacing(deviceName)
            val supportedFormats = cameraEnumerator.getSupportedFormats(deviceName)
            val printableFormats = supportedFormats.joinToString("\t\n")
            Timber.d("Device $deviceName, is front facing: $isFrontFacing, is back facing: $isBackFacing")
            Timber.d("Supported formats:\n$printableFormats")
            val supportedSizes = supportedFormats.map { Size(it.width, it.height) }
            val closestSize = CameraEnumerationAndroid.getClosestSupportedSize(supportedSizes, VIDEO_WIDTH, VIDEO_HEIGHT)
            val bestFormat = supportedFormats.firstOrNull { it.width == closestSize.width && it.height == closestSize.height }
            Timber.d("Best format: $bestFormat")
        }

        // prefer the front-facing camera, but if unavailable get whatever is there
        val frontFacingDevice = deviceNames.firstOrNull { cameraEnumerator.isFrontFacing(it) }
        val selectedCamera = frontFacingDevice ?: deviceNames.firstOrNull() ?: return
        Timber.d("Device selected: $selectedCamera")
        createCapturer(selectedCamera)
    }

    private fun createCapturer(deviceName: String) {
        cameraCapture?.run {
            stopCapture()
            dispose()
        }
        cameraCapture = cameraEnumerator.createCapturer(deviceName, SimpleCameraEventsHandler())
        val videoSource = peerConnectionFactory.createVideoSource(cameraCapture!!.isScreencast)
        cameraCapture?.initialize(surfaceTextureHelper, application, videoSource.capturerObserver)
        val renderer = binding.primaryViewRenderer
        renderer.init(eglBaseContext, null)
        renderer.isVisible = true

        videoTrack?.run {
            removeSink(renderer)
            dispose()
        }
        videoTrack = peerConnectionFactory.createVideoTrack("video: $deviceName", videoSource)
        videoTrack?.addSink(renderer)

        cameraCapture?.startCapture(VIDEO_WIDTH, VIDEO_HEIGHT, VIDEO_FPS)
    }

    private fun openLiveBroadcastPicker() {
        val fragmentManager = fragmentManager ?: return
        LiveBroadcastPickerFragment().apply {
            setTargetFragment(this@BroadcastFragment, PICK_LIVE_BROADCAST)
            show(fragmentManager, "liveBroadcastPicker")
        }
    }
}
