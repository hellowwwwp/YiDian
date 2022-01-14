package com.yidian.player.view.video

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Menu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.yidian.player.R
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            //android 11 以上 读权限 + 管理所有文件权限
            Manifest.permission.READ_EXTERNAL_STORAGE
        } else {
            //android 10 以下 读写权限
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        }
    }

    private val requestPermission = ActivityResultContracts.RequestPermission()

    /**
     * 权限申请
     */
    private val storagePermissionLauncher = registerForActivityResult(requestPermission) { isGranted ->
        if (isGranted) {
            //权限被授予
            initData()
        } else {
            //权限被拒绝
            val shouldShowRationale = shouldShowRequestPermissionRationale(storagePermission)
            showStoragePermissionRationaleDialog(shouldShowRationale)
        }
    }

    /**
     * 扫描的管理所有文件权限
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private val scanPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Environment.isExternalStorageManager()) {
            //权限被授予
            onScanClick()
        } else {
            //权限被拒绝
            showScanPermissionRationaleDialog()
        }
    }

    /**
     * 跳转应用设置界面启动器
     */
    private val startAppDetailsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        //检查权限
        if (hasStoragePermissionGranted()) {
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
        if (hasStoragePermissionGranted()) {
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
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_scan -> onScanClick()
                }
                return@setOnMenuItemClickListener true
            }
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
        setScanProgressVisible(false)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.video_list_menus, menu)
        return true
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
            state.isScan?.let {
                setScanProgressVisible(it)
            }
        })
        videoListViewModel.videoListLiveData.observe(this, { data ->
            videoListAdapter.submitList(data)
        })
    }

    private fun initData() {
        videoListViewModel.initVideoList()
    }

    private fun hasStoragePermissionGranted(): Boolean {
        return requestPermission.getSynchronousResult(this, storagePermission)?.value ?: false
    }

    private fun showStoragePermissionRationaleDialog(shouldShowRationale: Boolean) {
        val message = if (shouldShowRationale) {
            "没有权限无法加载本地视频哦~"
        } else {
            "没有权限真就运行不下去了呀~"
        }
        AlertDialog.Builder(this).apply {
            setTitle("权限说明")
            setMessage(message)
            setCancelable(false)
            setPositiveButton("去授权") { _, _ ->
                if (shouldShowRationale) {
                    storagePermissionLauncher.launch(storagePermission)
                } else {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", packageName, null)
                    )
                    startAppDetailsLauncher.launch(intent)
                }
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

    private fun onScanClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                videoListViewModel.scanVideoFiles()
            } else {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.fromParts("package", packageName, null)
                )
                scanPermissionLauncher.launch(intent)
            }
        } else {
            videoListViewModel.scanVideoFiles()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun showScanPermissionRationaleDialog() {
        AlertDialog.Builder(this).apply {
            setTitle("权限说明")
            setMessage("全盘扫描功能需要管理权限哦~")
            setCancelable(false)
            setPositiveButton("去授权") { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.fromParts("package", packageName, null)
                )
                scanPermissionLauncher.launch(intent)
            }
            setNegativeButton("取消") { _, _ ->
                //on op
            }
        }.show()
    }

    private fun setScanProgressVisible(isVisible: Boolean) {
        viewBinding.scanPb.isVisible = isVisible
    }

}