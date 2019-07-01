package tv.caffeine.app.lobby

import androidx.core.view.isVisible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.webrtc.EglRenderer
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import tv.caffeine.app.api.NewReyes
import tv.caffeine.app.stage.NewReyesController

interface LiveInTheLobbyCapable {
    var frameListener: EglRenderer.FrameListener?

    suspend fun startLiveVideo(
        renderer: SurfaceViewRenderer,
        controller: NewReyesController,
        onConnectCallback: () -> Unit
    ) {
        coroutineScope {
            launch {
                controller.connectionChannel.consumeEach { feedInfo ->
                    if (feedInfo.role == NewReyes.Feed.Role.primary) {
                        val videoTrack = feedInfo.connectionInfo.videoTrack
                        configureRenderer(renderer, feedInfo.feed, videoTrack)
                        frameListener = EglRenderer.FrameListener {
                            launch(Dispatchers.Main) {
                                renderer.removeFrameListener(frameListener)
                                onConnectCallback()
                            }
                        }
                        renderer.addFrameListener(frameListener, 1.0f)
                    }
                }
            }
            launch {
                controller.stateChangeChannel.consumeEach { list ->
                    list.forEach { stateChange ->
                        when (stateChange) {
                            is NewReyesController.StateChange.FeedRemoved -> {
                                // TODO remove sink for primary video
                                // TODO videoTrack.removeSink(binding.primaryViewRenderer)
                            }
                        }
                    }
                }
            }
            launch { controller.feedChannel.consumeEach { } }
            launch { controller.errorChannel.consumeEach { } }
            launch { controller.feedQualityChannel.consumeEach { } }
        }
    }

    private fun configureRenderer(renderer: SurfaceViewRenderer, feed: NewReyes.Feed?, videoTrack: VideoTrack?) {
        val hasVideo = videoTrack != null && (feed?.capabilities?.video ?: false)
        renderer.isVisible = hasVideo
        if (hasVideo) {
            videoTrack?.addSink(renderer)
        } else {
            videoTrack?.removeSink(renderer)
        }
    }
}
