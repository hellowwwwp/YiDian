package com.yidian.player.widget

import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.util.AttributeSet
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSourceFactory
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.video.VideoSize
import com.yidian.player.R
import com.yidian.player.base.getFragmentActivity
import com.yidian.player.base.layoutInflater
import com.yidian.player.databinding.LayoutVideoViewBinding
import com.yidian.player.view.video.model.VideoEntity

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2022/1/10
 */
class YiDianVideoView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) :
    FrameLayout(context, attrs, defStyleAttr),
    Player.Listener,
    View.OnLayoutChangeListener,
    DefaultLifecycleObserver {

    private val viewBinding = LayoutVideoViewBinding.inflate(layoutInflater, this)

    private val mediaSourceFactory: MediaSourceFactory = DefaultMediaSourceFactory(context)

    private val player: ExoPlayer = kotlin.run {
        val renderersFactory = DefaultRenderersFactory(context)
        //允许扩展软解
        renderersFactory.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        ExoPlayer.Builder(context, renderersFactory, mediaSourceFactory).build()
    }

    private var currentState: PlayState = PlayState.Idle

    private val listeners: MutableList<OnVideoPlayListener> = mutableListOf()

    private val contentFrame: AspectRatioFrameLayout
        get() = viewBinding.contentFrame

    private val textureView: TextureView
        get() = viewBinding.textureView

    val controllerView: YiDianVideoControllerView
        get() = viewBinding.controllerView

    private var textureViewRotation: Int = 0

    @AspectRatioFrameLayout.ResizeMode
    var resizeMode: Int = 0
        set(value) {
            field = value
            contentFrame.resizeMode = value
        }

    private var isPauseByClick: Boolean = false

    private val videoList: MutableList<VideoEntity> = mutableListOf()

    /**
     * 是否需要更新进度
     */
    private var progressUpdateEnabled: Boolean = false

    /**
     * 进度更新的时间间隔
     */
    var progressUpdateIntervalTimeMillis: Long = 0

    /**
     * 进度更新
     */
    private val progressUpdateRunnable = object : Runnable {
        override fun run() {
            onProgressChanged(player.currentPosition, player.duration)
            if (progressUpdateEnabled && currentState == PlayState.Start) {
                postDelayed(this, progressUpdateIntervalTimeMillis)
            }
        }
    }

    var trySystemBarVisibleChanged: ((Boolean) -> Unit)? = null

    private val controllerListener = object : YiDianVideoControllerView.OnControllerListener {
        override fun onBackClick(controllerView: YiDianVideoControllerView) {
            getFragmentActivity()?.apply {
                onBackPressedDispatcher.onBackPressed()
            }
        }

        override fun onScreenShotClick(controllerView: YiDianVideoControllerView) {
            //TODO
        }

        override fun onSeekChanged(controllerView: YiDianVideoControllerView, currentPosition: Long) {
            player.seekTo(currentPosition)
        }

        override fun onPreviousClick(controllerView: YiDianVideoControllerView) {
            player.seekToPreviousMediaItem()
        }

        override fun onNextClick(controllerView: YiDianVideoControllerView) {
            player.seekToNextMediaItem()
        }

        override fun onPlayClick(controllerView: YiDianVideoControllerView) {
            startPlay(false)
        }

        override fun onPauseClick(controllerView: YiDianVideoControllerView) {
            pausePlay(true)
        }

        override fun onRePlayClick(controllerView: YiDianVideoControllerView) {
            startPlay(true)
        }

        override fun onSpeedChanged(controllerView: YiDianVideoControllerView, speed: Float) {
            player.setPlaybackSpeed(speed)
        }

        override fun trySystemBarVisibleChanged(isVisible: Boolean) {
            trySystemBarVisibleChanged?.invoke(isVisible)
        }
    }

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.YiDianVideoView)
        resizeMode = ta.getInt(
            R.styleable.YiDianVideoView_yvv_resizeMode,
            AspectRatioFrameLayout.RESIZE_MODE_FIT
        )
        progressUpdateIntervalTimeMillis = ta.getInt(
            R.styleable.YiDianVideoView_yvv_progressUpdateInterval,
            250
        ).toLong()
        ta.recycle()
        player.addListener(this)
        player.setVideoTextureView(textureView)
        controllerView.addOnControllerListener(controllerListener)
        controllerView.bindVideoView(this)
    }

    private fun onProgressChanged(currentPosition: Long, duration: Long) {
        listeners.forEach {
            it.onProgressChanged(currentPosition, duration)
        }
    }

    private fun startProgressUpdate() {
        progressUpdateEnabled = true
        removeCallbacks(progressUpdateRunnable)
        post(progressUpdateRunnable)
    }

    private fun stopProgressUpdate() {
        progressUpdateEnabled = false
        removeCallbacks(progressUpdateRunnable)
    }

    fun bindLifecycle(lifecycle: Lifecycle) {
        lifecycle.addObserver(this)
    }

    override fun onResume(owner: LifecycleOwner) {
        if (!player.playWhenReady && !isPauseByClick) {
            startPlay(false)
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        if (player.playWhenReady) {
            pausePlay(false)
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        owner.lifecycle.removeObserver(this)
    }

    private fun onVideoSourceChanged(videoIndex: Int, hasPrevious: Boolean, hasNext: Boolean) {
        if (videoIndex >= 0 && videoIndex < videoList.size) {
            val videoEntity = videoList[videoIndex]
            listeners.forEach {
                it.onVideoSourceChanged(videoEntity, hasPrevious, hasNext)
            }
        }
    }

    fun setVideoList(videoList: List<VideoEntity>, initIndex: Int) {
        this.videoList.clear()
        this.videoList.addAll(videoList)
        //更新当前播放的视频
        val hasPrevious = initIndex > 0
        val hasNext = initIndex < videoList.size - 1
        onVideoSourceChanged(initIndex, hasPrevious, hasNext)
        val concatMediaSource = ConcatenatingMediaSource()
        videoList.forEach {
            concatMediaSource.addMediaSource(
                mediaSourceFactory.createMediaSource(
                    MediaItem.fromUri(it.uri)
                )
            )
        }
        val progress = videoList[initIndex].progress
        player.setMediaSource(concatMediaSource)
        player.prepare()
        player.seekTo(initIndex, progress)
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        when {
            player.playbackState == Player.STATE_READY && playWhenReady -> {
                onPlayStateChanged(PlayState.Start)
            }
            player.playbackState == Player.STATE_READY && !playWhenReady -> {
                onPlayStateChanged(PlayState.Pause)
            }
        }
    }

    override fun onPlaybackStateChanged(state: Int) {
        when {
            state == Player.STATE_BUFFERING -> {
                onPlayStateChanged(PlayState.Buffering)
            }
            state == Player.STATE_READY && player.playWhenReady -> {
                onPlayStateChanged(PlayState.Start)
            }
            state == Player.STATE_ENDED -> {
                onPlayStateChanged(PlayState.End)
            }
        }
    }

    private fun onPlayStateChanged(newState: PlayState) {
        if (newState != currentState) {
            val oldState = currentState
            currentState = newState
            //屏幕常亮
            keepScreenOn = currentState != PlayState.Idle && currentState != PlayState.End
            //播放结束时手动更新一次进度
            if (currentState == PlayState.End) {
                onProgressChanged(player.duration, player.duration)
            }
            //进度更新
            if (currentState == PlayState.Start) {
                startProgressUpdate()
            } else {
                stopProgressUpdate()
            }
            //回调
            listeners.forEach {
                it.onPlayStateChanged(oldState, newState)
            }
        }
    }

    fun addOnVideoPlayListener(listener: OnVideoPlayListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeOnVideoPlayListener(listener: OnVideoPlayListener) {
        listeners.remove(listener)
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        if (oldPosition.mediaItemIndex != newPosition.mediaItemIndex) {
            onVideoSourceChanged(
                newPosition.mediaItemIndex,
                player.hasPreviousMediaItem(),
                player.hasNextMediaItem()
            )
        }
    }

    fun startPlay(rePlay: Boolean) {
        if (rePlay) {
            player.seekTo(0)
        }
        player.playWhenReady = true
    }

    fun pausePlay(isPauseByClick: Boolean) {
        this.isPauseByClick = isPauseByClick
        player.playWhenReady = false
    }

    fun stopPlay() {
        player.stop()
    }

    fun release() {
        player.release()
        stopProgressUpdate()
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        updateAspectRatio()
    }

    /** textureView.addOnLayoutChangeListener(this) **/
    override fun onLayoutChange(
        view: View,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        oldLeft: Int,
        oldTop: Int,
        oldRight: Int,
        oldBottom: Int
    ) {
        applyTextureViewRotation(textureView, textureViewRotation)
    }

    private fun updateAspectRatio() {
        val videoSize = player.videoSize
        val unAppliedRotationDegrees = videoSize.unappliedRotationDegrees
        var videoAspectRatio = if (videoSize.width == 0 || videoSize.height == 0) {
            0f
        } else {
            (videoSize.width * videoSize.pixelWidthHeightRatio) / videoSize.height
        }
        // Try to apply rotation transformation.
        if (videoAspectRatio > 0 && (unAppliedRotationDegrees == 90 || unAppliedRotationDegrees == 270)) {
            // We will apply a rotation 90/270 degree to the output texture of the TextureView.
            // In this case, the output video's width and height will be swapped.
            videoAspectRatio = 1f / videoAspectRatio
        }
        if (textureViewRotation != 0) {
            textureView.removeOnLayoutChangeListener(this)
        }
        textureViewRotation = unAppliedRotationDegrees
        if (textureViewRotation != 0) {
            // The texture view's dimensions might be changed after layout step.
            // So add an OnLayoutChangeListener to apply rotation after layout step.
            textureView.addOnLayoutChangeListener(this)
        }
        applyTextureViewRotation(textureView, textureViewRotation)
        contentFrame.setAspectRatio(videoAspectRatio)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        player.removeListener(this)
        controllerView.removeOnControllerListener(controllerListener)
        controllerView.unbindVideoView(this)
        release()
    }

    /** Applies a texture rotation to a {@link TextureView}. */
    private fun applyTextureViewRotation(textureView: TextureView, textureViewRotation: Int) {
        val transformMatrix = Matrix()
        val textureViewWidth = textureView.width.toFloat()
        val textureViewHeight = textureView.height.toFloat()
        if (textureViewWidth != 0f && textureViewHeight != 0f && textureViewRotation != 0) {
            val pivotX = textureViewWidth / 2
            val pivotY = textureViewHeight / 2
            transformMatrix.postRotate(textureViewRotation.toFloat(), pivotX, pivotY)

            // After rotation, scale the rotated texture to fit the TextureView size.
            val originalTextureRect = RectF(0f, 0f, textureViewWidth, textureViewHeight)
            val rotatedTextureRect = RectF()
            transformMatrix.mapRect(rotatedTextureRect, originalTextureRect)
            transformMatrix.postScale(
                textureViewWidth / rotatedTextureRect.width(),
                textureViewHeight / rotatedTextureRect.height(),
                pivotX,
                pivotY
            )
        }
        textureView.setTransform(transformMatrix)
    }
}

enum class VideoSpeed(
    val speedName: String,
    val speedValue: Float
) {
    SPEED_0_8("0.8 x", 0.8f),
    SPEED_1_0("1.0 x", 1.0f),
    SPEED_1_2_5("1.25 x", 1.25f),
    SPEED_1_5("1.5 x", 1.5f),
    SPEED_2_0("2.0 x", 2.0f),
    SPEED_3_0("3.0 x", 3.0f),
}

enum class PlayState {
    Idle, Buffering, Start, Pause, End
}

interface OnVideoPlayListener {

    fun onPlayStateChanged(oldState: PlayState, newState: PlayState) {

    }

    fun onProgressChanged(position: Long, duration: Long) {

    }

    fun onVideoSourceChanged(videoEntity: VideoEntity, hasPrevious: Boolean, hasNext: Boolean) {

    }

}