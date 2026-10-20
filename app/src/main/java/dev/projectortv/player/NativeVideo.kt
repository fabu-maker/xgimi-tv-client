package dev.projectortv.player

import android.app.Activity
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/** Genuine native Media3 player. Source eligibility must be verified independently. */
class NativeVideo(private val activity: Activity) {
    private var exo: ExoPlayer? = null
    private var view: PlayerView? = null

    fun start(host: FrameLayout, url: String, positionMs: Long = 0L) {
        stop()
        val player = ExoPlayer.Builder(activity).build()
        exo = player
        val playerView = PlayerView(activity).apply {
            useController = true
            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
            this.player = player
            isFocusable = true
            isFocusableInTouchMode = true
        }
        view = playerView
        host.addView(playerView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT))
        player.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
        player.prepare()
        if (positionMs > 0L) player.seekTo(positionMs)
        player.playWhenReady = true
        playerView.requestFocus()
    }

    fun position(): Long = exo?.currentPosition ?: 0L
    fun pause() { exo?.pause() }
    fun resume() { /* Pause remains a user decision after interruption. */ }
    fun stop() {
        view?.player = null
        (view?.parent as? ViewGroup)?.removeView(view)
        view = null
        exo?.release()
        exo = null
    }
}

/** Only explicitly approved demo streams are playable; BingeBang extraction is not implemented. */
object PlaybackPolicy {
    const val DEMO_URL = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
    fun canPlay(url: String) = url == DEMO_URL
}
