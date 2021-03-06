package com.yidian.player.view.video

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.gyf.immersionbar.BarHide
import com.gyf.immersionbar.ktx.immersionBar
import com.yidian.player.base.BaseActivity
import com.yidian.player.base.getIntExtraExt
import com.yidian.player.base.getParcelableListExtra
import com.yidian.player.databinding.LayoutVideoPlayActivityBinding
import com.yidian.player.view.video.model.VideoEntity
import com.yidian.player.view.video.viewmodel.VideoPlayViewModel
import com.yidian.player.widget.OnVideoPlayListener
import com.yidian.player.widget.YiDianVideoView

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2022/1/10
 */
class VideoPlayActivity : BaseActivity(), OnVideoPlayListener {

    private val viewBinding: LayoutVideoPlayActivityBinding by lazy {
        LayoutVideoPlayActivityBinding.inflate(layoutInflater)
    }

    private val videoPlayViewModel: VideoPlayViewModel by viewModels()

    private val videoView: YiDianVideoView
        get() = viewBinding.videoView

    override val isCommonBarEnabled: Boolean
        get() = false

    private var currentVideoEntity: VideoEntity? = null

    private var currentPosition: Long = 0
    private var currentDuration: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSystemBarVisible(true)
        setContentView(viewBinding.root)
        initVideoView()
    }

    private fun setSystemBarVisible(isVisible: Boolean) {
        immersionBar {
            if (isVisible) {
                hideBar(BarHide.FLAG_SHOW_BAR)
            } else {
                hideBar(BarHide.FLAG_HIDE_BAR)
            }
            transparentBar()
        }
    }

    private fun initVideoView() {
        val videoList = intent.getParcelableListExtra<VideoEntity>(KEY_VIDEO_LIST)
        val initIndex = intent.getIntExtraExt(KEY_VIDEO_INIT_INDEX, 0, 0, videoList.size - 1)
        videoView.trySystemBarVisibleChanged = { isVisible ->
            setSystemBarVisible(isVisible)
        }
        videoView.addOnVideoPlayListener(this)
        videoView.bindLifecycle(this.lifecycle)
        videoView.setVideoList(videoList, initIndex)
        videoView.startPlay(false)
    }

    override fun onProgressChanged(position: Long, duration: Long) {
        this.currentPosition = position
        this.currentDuration = duration
    }

    override fun onVideoSourceChanged(videoEntity: VideoEntity, hasPrevious: Boolean, hasNext: Boolean) {
        this.currentVideoEntity = videoEntity
    }

    override fun onPause() {
        super.onPause()
        currentVideoEntity?.let {
            videoPlayViewModel.updateVideoProgress(
                it.id, currentPosition, currentDuration
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        videoView.removeOnVideoPlayListener(this)
    }

    companion object {

        private const val KEY_VIDEO_LIST = "video_list"
        private const val KEY_VIDEO_INIT_INDEX = "video_init_index"

        fun start(activity: Activity, videoEntity: VideoEntity) {
            start(activity, listOf(videoEntity), 0)
        }

        fun start(activity: Activity, videoList: List<VideoEntity>, videoInitIndex: Int) {
            val intent = Intent(activity, VideoPlayActivity::class.java)
            intent.putParcelableArrayListExtra(KEY_VIDEO_LIST, ArrayList(videoList))
            intent.putExtra(KEY_VIDEO_INIT_INDEX, videoInitIndex)
            activity.startActivity(intent)
        }

    }

}