package com.yidian.player.view.video

import android.app.Activity
import android.content.Intent
import android.graphics.Matrix
import android.graphics.RectF
import android.os.Bundle
import android.view.TextureView
import android.view.View
import android.widget.SeekBar
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.video.VideoSize
import com.yidian.player.base.BaseActivity
import com.yidian.player.base.getIntExtraExt
import com.yidian.player.base.getParcelableListExtra
import com.yidian.player.databinding.LayoutTestVideoActivityBinding
import com.yidian.player.view.video.model.VideoEntity
import com.yidian.player.view.video.preview.VideoPreviewBar
import com.yidian.player.widget.AbsSeekBarChangeListener
import com.yidian.player.widget.SimpleVideoPreviewView

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2022/1/16
 */
class TestVideoActivity : BaseActivity(), VideoPreviewBar.PreviewBarCallback, Player.Listener,
    View.OnLayoutChangeListener {

    private val viewBinding: LayoutTestVideoActivityBinding by lazy {
        LayoutTestVideoActivityBinding.inflate(layoutInflater)
    }

    private val player: ExoPlayer by lazy {
        ExoPlayer.Builder(this).build().apply {
            trackSelectionParameters = trackSelectionParameters
                .buildUpon()
                .setDisabledTrackTypes(setOf(C.TRACK_TYPE_AUDIO, C.TRACK_TYPE_TEXT))
                .build()
        }
    }

    private var textureViewRotation: Int = 0

    private val contentFrame: AspectRatioFrameLayout
        get() = viewBinding.contentFrame

    private val textureView: TextureView
        get() = viewBinding.textureView

    private val seekBar: SeekBar
        get() = viewBinding.seekBar

    private val videoPreview: SimpleVideoPreviewView
        get() = viewBinding.videoPreview

    private var progressUpdateEnabled: Boolean = false
    private val progressUpdateRunnable = object : Runnable {
        override fun run() {
            val currentPosition = player.currentPosition.toLong()
            val duration = player.duration.toLong()
            onProgressChanged(currentPosition, duration)
            if (progressUpdateEnabled) {
                viewBinding.root.postDelayed(this, 200)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        initVideoView()
    }

    private fun initVideoView() {
        val videoList = intent.getParcelableListExtra<VideoEntity>(KEY_VIDEO_LIST)
        val initIndex = intent.getIntExtraExt(KEY_VIDEO_INIT_INDEX, 0, 0, videoList.size - 1)
        val videoEntity = videoList[initIndex]

        player.addListener(this)
        player.setVideoTextureView(textureView)
        player.addMediaItem(MediaItem.fromUri(videoEntity.uri))
        player.prepare()

        videoPreview.setDataSource(videoEntity.uri)

        seekBar.max = videoEntity.duration.toInt()
        seekBar.progress = 0

        seekBar.setOnSeekBarChangeListener(object : AbsSeekBarChangeListener() {
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                setProgressUpdateState(false)
                videoPreview.isVisible = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                setProgressUpdateState(true)
                player.seekTo(seekBar.progress.toLong())
                videoPreview.stop()
                videoPreview.isInvisible = true
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    videoPreview.seekTo(progress.toLong())
                }
            }
        })
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        updateAspectRatio(videoSize)
    }

    private fun updateAspectRatio(videoSize: VideoSize) {
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

    override fun onLayoutChange(
        v: View,
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

    override fun onStopTracking(progress: Long) {
        player.seekTo(progress)
    }

    override fun onResume() {
        super.onResume()
        player.playWhenReady = true
        setProgressUpdateState(true)
    }

    override fun onPause() {
        super.onPause()
        player.pause()
        setProgressUpdateState(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
        videoPreview.release()
    }

    private fun setProgressUpdateState(isEnabled: Boolean) {
        progressUpdateEnabled = isEnabled
        with(viewBinding.root) {
            removeCallbacks(progressUpdateRunnable)
            if (isEnabled) {
                post(progressUpdateRunnable)
            }
        }
    }

    private fun onProgressChanged(currentPosition: Long, duration: Long) {
        if (progressUpdateEnabled) {
            seekBar.max = duration.toInt()
            seekBar.progress = currentPosition.toInt()
        }
    }

    companion object {

        private const val KEY_VIDEO_LIST = "video_list"
        private const val KEY_VIDEO_INIT_INDEX = "video_init_index"

        fun start(activity: Activity, videoEntity: VideoEntity) {
            start(activity, listOf(videoEntity), 0)
        }

        fun start(activity: Activity, videoList: List<VideoEntity>, videoInitIndex: Int) {
            val intent = Intent(activity, TestVideoActivity::class.java)
            intent.putParcelableArrayListExtra(KEY_VIDEO_LIST, ArrayList(videoList))
            intent.putExtra(KEY_VIDEO_INIT_INDEX, videoInitIndex)
            activity.startActivity(intent)
        }

    }

}