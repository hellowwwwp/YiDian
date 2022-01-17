package com.yidian.player.widget

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.yidian.player.base.layoutInflater
import com.yidian.player.databinding.LayoutVideoPreviewViewBinding
import com.yidian.player.utils.VideoUtils

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2022/1/16
 */
class VideoPreviewView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), TextureView.SurfaceTextureListener, View.OnLayoutChangeListener {

    private val viewBinding = LayoutVideoPreviewViewBinding.inflate(layoutInflater, this)

    private val contentFrame: AspectRatioFrameLayout
        get() = viewBinding.contentFrame

    private val textureView: TextureView
        get() = viewBinding.textureView

    private var videoUri: Uri? = null
    private var pendingSeek: Long = -1

    private var textureViewRotation: Int = 0

    private val innerHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                VIDEO_PREPARED -> {
                    (msg.obj as? VideoSize)?.let {
                        updateAspectRatio(it.width, it.height, 0)
                    }
                }
                VIDEO_DECODE_FAIL -> {
                    Log.e("tag", "video decode fail")
                }
            }
        }
    }

    private var videoDecodeThread: VideoDecodeThread? = null

    init {
        textureView.surfaceTextureListener = this
    }

    fun setUri(uri: Uri) {
        release()
        this.videoUri = uri
        this.pendingSeek = -1
        val surfaceTexture = textureView.surfaceTexture
        if (textureView.isAvailable && surfaceTexture != null) {
            startVideoDecode(Surface(surfaceTexture), uri)
        }
    }

    private fun startVideoDecode(surface: Surface, uri: Uri) {
        val videoDecode = VideoDecodeThread(context.applicationContext, surface, uri, innerHandler).also {
            videoDecodeThread = it
            it.start()
        }
        if (pendingSeek != -1L) {
            videoDecode.seekTo(pendingSeek)
            pendingSeek = -1
        }
    }

    fun seekTo(position: Long) {
        videoDecodeThread?.apply {
            seekTo(position)
            pendingSeek = -1
        } ?: kotlin.run {
            pendingSeek = position
        }
    }

    fun finishSeek() {
        videoDecodeThread?.finishSeek()
    }

    fun release() {
        videoDecodeThread?.apply {
            release()
            videoDecodeThread = null
        }
    }

    fun updateAspectRatio(videoWidth: Int, videoHeight: Int, videoDegrees: Int) {
        var videoAspectRatio = if (videoWidth == 0 || videoHeight == 0) {
            0f
        } else {
            (videoWidth * 1f) / videoHeight
        }
        // Try to apply rotation transformation.
        if (videoAspectRatio > 0 && (videoDegrees == 90 || videoDegrees == 270)) {
            // We will apply a rotation 90/270 degree to the output texture of the TextureView.
            // In this case, the output video's width and height will be swapped.
            videoAspectRatio = 1f / videoAspectRatio
        }
        if (textureViewRotation != 0) {
            textureView.removeOnLayoutChangeListener(this)
        }
        textureViewRotation = videoDegrees
        if (textureViewRotation != 0) {
            // The texture view's dimensions might be changed after layout step.
            // So add an OnLayoutChangeListener to apply rotation after layout step.
            textureView.addOnLayoutChangeListener(this)
        }
        VideoUtils.applyTextureViewRotation(textureView, textureViewRotation)
        contentFrame.setAspectRatio(videoAspectRatio)
    }

    override fun onLayoutChange(
        v: View, left: Int, top: Int, right: Int, bottom: Int,
        oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
    ) {
        VideoUtils.applyTextureViewRotation(textureView, textureViewRotation)
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        videoUri?.let {
            startVideoDecode(Surface(surface), it)
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        //no op
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        Log.w("tag", "onSurfaceTextureDestroyed")
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        //no op
    }

    private class VideoDecodeThread(
        private val context: Context,
        private val surface: Surface,
        private val uri: Uri,
        private val handler: Handler
    ) : Thread() {

        private var mediaExtractor: MediaExtractor? = null
        private var mediaCodec: MediaCodec? = null

        @Volatile
        private var needSeek: Boolean = false

        fun seekTo(position: Long) {
            needSeek = true
            try {
                mediaExtractor?.seekTo(position, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            } catch (e: Exception) {
                handler.sendEmptyMessage(VIDEO_DECODE_FAIL)
            } finally {
                //needSeek = false
            }
        }

        fun finishSeek() {
            needSeek = false
        }

        fun release() {
            interrupt()
            mediaExtractor?.apply {
                release()
                mediaExtractor = null
            }
            mediaCodec?.apply {
                stop()
                release()
                mediaCodec = null
            }
        }

        override fun run() {
            super.run()

            val extractor = MediaExtractor().also {
                mediaExtractor = it
            }
            try {
                extractor.setDataSource(context, uri, null)
            } catch (e: Exception) {
                handler.sendEmptyMessage(VIDEO_DECODE_FAIL)
                return
            }
            var mediaFormat: MediaFormat? = null
            var mimeType: String? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("video/") == true) {
                    extractor.selectTrack(i)
                    mediaFormat = format
                    mimeType = mime
                    break
                }
            }
            if (mediaFormat == null || mimeType == null) {
                handler.sendEmptyMessage(VIDEO_DECODE_FAIL)
                return
            }

            val width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH)
            val height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT)
            val duration = mediaFormat.getLong(MediaFormat.KEY_DURATION)
            Log.d("tag", "width: $width, height: $height, duration: $duration")

            val message = handler.obtainMessage()
            message.what = VIDEO_PREPARED
            message.obj = VideoSize(width, height, duration)
            handler.sendMessage(message)

            //设置预览缩放比例
            setPreviewRatio(mediaFormat)

            val codec: MediaCodec = try {
                MediaCodec.createDecoderByType(mimeType).also {
                    mediaCodec = it
                    it.configure(mediaFormat, surface, null, 0)
                    it.start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                handler.sendEmptyMessage(VIDEO_DECODE_FAIL)
                return
            }

            val bufferInfo = MediaCodec.BufferInfo()
            var retryCount = 1

            while (!isInterrupted) {
                if (!needSeek || retryCount >= 50) {
                    SystemClock.sleep(10)
                    continue
                }
                //从缓冲区取出一个缓冲块，如果当前无可用缓冲块，返回 inputIndex < 0
                val inputIndex = codec.dequeueInputBuffer(DEQUEUE_TIME)
                Log.d("tag", "inputIndex: $inputIndex")
                if (inputIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputIndex)
                    if (inputBuffer == null) {
                        Log.e("tag", "inputBuffer is null")
                        needSeek = false
                        continue
                    }
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    //入队列
                    if (sampleSize >= 0) {
                        codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    } else {
                        codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    }
                }

                //出队列
                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, DEQUEUE_TIME)
                Log.d("tag", "outputIndex: $outputIndex")
                when (outputIndex) {
                    MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        Log.e("tag", "try again later")
                        retryCount++
//                        continue
                    }
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        Log.e("tag", "output format changed")
                        val outputFormat = codec.getOutputFormat()
                        setPreviewRatio(outputFormat)
                    }
                    MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                        Log.e("tag", "output buffer changed")
                    }
                    else -> {
                        codec.releaseOutputBuffer(outputIndex, true)
//                        needSeek = false
//                        continue
                    }
                }
            }
        }

        private fun setPreviewRatio(mediaFormat: MediaFormat) {
            val width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH)
            val height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT)
            val previewRatio = when {
                width >= RATIO_1080 -> 10f
                width >= RATIO_480 -> 6f
                width >= RATIO_240 -> 4f
                else -> 1f
            }
            val previewWidth = (width / previewRatio).toInt()
            val previewHeight = (height / previewRatio).toInt()
            Log.d("tag", "previewRatio: $previewRatio, previewWidth: $previewWidth, $previewHeight")
            mediaFormat.setInteger(MediaFormat.KEY_WIDTH, previewWidth)
            mediaFormat.setInteger(MediaFormat.KEY_HEIGHT, previewHeight)
        }

    }

    private data class VideoSize(
        val width: Int,
        val height: Int,
        val duration: Long
    )

    companion object {
        private const val VIDEO_PREPARED = 1
        private const val VIDEO_DECODE_FAIL = 2
        private const val DEQUEUE_TIME: Long = 10 * 1000

        private const val RATIO_1080 = 1080
        private const val RATIO_480 = 480
        private const val RATIO_240 = 240
    }

}