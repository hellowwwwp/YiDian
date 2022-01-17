package com.yidian.player.view.video

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.yidian.player.base.BaseActivity
import com.yidian.player.databinding.LayoutVideoListDetailActivityBinding
import com.yidian.player.view.video.adapter.VideoListDetailAdapter
import com.yidian.player.view.video.model.VideoEntity
import com.yidian.player.view.video.model.VideoListItemEntity
import com.yidian.player.view.video.viewmodel.VideoListDetailViewModel

class VideoListDetailActivity : BaseActivity() {

    private val viewBinding: LayoutVideoListDetailActivityBinding by lazy {
        LayoutVideoListDetailActivityBinding.inflate(layoutInflater)
    }

    private val videoListDetailViewModel: VideoListDetailViewModel by viewModels()

    private val deleteLauncher: ActivityResultLauncher<IntentSenderRequest> =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult(), ::onDeleteResult)

    private val videoListDetailAdapter: VideoListDetailAdapter by lazy {
        VideoListDetailAdapter().apply {
            onItemClick = ::onVideoItemClick
            onItemLongClick = ::onVideoItemLongClick
        }
    }

    private val videoListItem: VideoListItemEntity
        get() = intent.getParcelableExtra(KEY_VIDEO_LIST_ITEM)
            ?: throw IllegalArgumentException("videoListItem is null")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        initView()
        initData()
    }

    private fun initView() {
        with(viewBinding.toolbar) {
            title = videoListItem.dirName
            setSupportActionBar(this)
            setNavigationOnClickListener {
                onBackPressed()
            }
        }
        with(viewBinding.videoListRcv) {
            layoutManager = LinearLayoutManager(this@VideoListDetailActivity)
            itemAnimator = null
            adapter = videoListDetailAdapter
        }
    }

    private fun initData() {
        lifecycle.addObserver(videoListDetailViewModel)
        videoListDetailViewModel.videoListDetailLiveData.observe(this) {
            videoListDetailAdapter.submitList(it)
        }
        videoListDetailViewModel.initVideoList(videoListItem.videoList)
    }

    private fun onVideoItemClick(position: Int, videoEntity: VideoEntity) {
        val videoList = videoListDetailViewModel.getVideoList()
        VideoPlayActivity.start(this, videoList, position)
    }

    private fun onVideoItemLongClick(position: Int, videoEntity: VideoEntity) {
        AlertDialog.Builder(this).apply {
            setTitle("删除提示")
            setMessage("您确定要删除这个视频吗?")
            setCancelable(true)
            setPositiveButton("删除") { dialog, _ ->
                dialog.dismiss()
                videoListDetailViewModel.deleteVideo(deleteLauncher, videoEntity)
            }
            setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
        }.show()
//        val videoList = videoListDetailViewModel.getVideoList()
//        TestVideoActivity.start(this, videoList, position)
    }

    private fun onDeleteResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            videoListDetailViewModel.deleteUIListVideo()
        }
    }

    companion object {

        const val KEY_VIDEO_LIST_ITEM = "video_list_item"

        fun start(activity: Activity, videoDirItem: VideoListItemEntity) {
            val intent = Intent(activity, VideoListDetailActivity::class.java)
            intent.putExtra(KEY_VIDEO_LIST_ITEM, videoDirItem)
            activity.startActivity(intent)
        }

    }

}