package com.yidian.player.view.video

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.yidian.player.base.BaseActivity
import com.yidian.player.databinding.LayoutVideoListActivityBinding
import com.yidian.player.view.video.adapter.VideoListAdapter
import com.yidian.player.view.video.model.VideoListItemEntity
import com.yidian.player.view.video.viewmodel.VideoListViewModel

class VideoListActivity : BaseActivity() {

    private val viewBinding: LayoutVideoListActivityBinding by lazy {
        LayoutVideoListActivityBinding.inflate(layoutInflater)
    }

    private val videoListViewModel: VideoListViewModel by viewModels()

    private val videoListAdapter: VideoListAdapter by lazy {
        VideoListAdapter().apply {
            onItemClick = ::onVideoListItemClick
        }
    }

    private val storagePermission: String by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Manifest.permission.READ_EXTERNAL_STORAGE
        } else {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        }
    }

    private val requestPermission = ActivityResultContracts.RequestPermission()

    /**
     * 判断是否授予了存储权限
     */
    private val isStoragePermissionGranted: Boolean
        get() = requestPermission.getSynchronousResult(this, storagePermission)?.value ?: false

    /**
     * 权限申请
     */
    private val storagePermissionLauncher = registerForActivityResult(requestPermission) { isGranted ->
        if (isGranted) {
            //权限被授予
            initData()
        } else {
            //权限被拒绝
            val showRationale = shouldShowRequestPermissionRationale(
                storagePermission
            )
            showStoragePermissionRationaleDialog(showRationale)
        }
    }

    /**
     * 跳转应用设置界面启动器
     */
    private val startAppDetailsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        //检查权限
        if (isStoragePermissionGranted) {
            //权限被授予
            initData()
        } else {
            //权限被拒绝
            storagePermissionLauncher.launch(storagePermission)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        initView()
        bindData()
        //检查权限
        if (isStoragePermissionGranted) {
            //权限被授予
            initData()
        } else {
            //权限被拒绝
            storagePermissionLauncher.launch(storagePermission)
        }
    }

    private fun initView() {
        with(viewBinding.toolbar) {
            title = "本地视频"
            setSupportActionBar(this)
        }
        with(viewBinding.refreshLayout) {
            setOnRefreshListener {
                videoListViewModel.refreshVideoList()
            }
        }
        with(viewBinding.videoListRcv) {
            layoutManager = LinearLayoutManager(this@VideoListActivity)
            itemAnimator = null
            adapter = videoListAdapter
        }
    }

    private fun bindData() {
        lifecycle.addObserver(videoListViewModel)
        videoListViewModel.videoListStateLiveData.observe(this, { state ->
            state.isLoading?.let {
                setLoadingLayoutVisible(it)
            }
            if (state.isRefreshing == false) {
                finishRefresh()
            }
        })
        videoListViewModel.videoListLiveData.observe(this, { data ->
            videoListAdapter.submitList(data)
        })
    }

    private fun initData() {
        videoListViewModel.initVideoList()
    }

    private fun showStoragePermissionRationaleDialog(isRationale: Boolean) {
        val message = if (isRationale) {
            "请前往应用设置页授予权限."
        } else {
            "没有权限真就运行不下去了呀~"
        }
        AlertDialog.Builder(this).apply {
            setTitle("权限说明")
            setMessage(message)
            setCancelable(false)
            setPositiveButton("去授权") { _, _ ->
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", packageName, null)
                )
                startAppDetailsLauncher.launch(intent)
            }
            setNegativeButton("取消") { _, _ ->
                finish()
            }
        }.show()
    }

    private fun setLoadingLayoutVisible(isVisible: Boolean) {
        with(viewBinding.loadingLayout) {
            root.isVisible = isVisible
        }
    }

    private fun finishRefresh() {
        viewBinding.refreshLayout.isRefreshing = false
    }

    private fun onVideoListItemClick(position: Int, videoListItemEntity: VideoListItemEntity) {
        VideoListDetailActivity.start(this, videoListItemEntity)
    }
}