package com.yidian.player.view.video.viewmodel

import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yidian.player.YiDianApp
import com.yidian.player.base.BaseViewModel
import com.yidian.player.view.video.db.VideoProgressBean
import com.yidian.player.view.video.db.VideoProgressDao
import com.yidian.player.view.video.db.YiDianRoomDatabase
import com.yidian.player.view.video.model.VideoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2022/1/8
 */
class VideoListDetailViewModel : BaseViewModel(), DefaultLifecycleObserver {

    val videoListDetailLiveData: MutableLiveData<List<VideoEntity>> = MutableLiveData(emptyList())

    private val videoProgressDao: VideoProgressDao by lazy {
        val database = YiDianRoomDatabase.get(YiDianApp.application)
        database.getVideoProgressDao()
    }

    private var needDeleteVideoEntity: VideoEntity? = null

    private var stoped: Boolean = false

    override fun onStart(owner: LifecycleOwner) {
        if (stoped) {
            refreshVideoProgress()
        }
        stoped = false
    }

    override fun onStop(owner: LifecycleOwner) {
        stoped = true
    }

    override fun onDestroy(owner: LifecycleOwner) {
        owner.lifecycle.removeObserver(this)
    }

    fun initVideoList(videoList: List<VideoEntity>) {
        videoListDetailLiveData.value = videoList
    }

    fun getVideoList(): List<VideoEntity> {
        return videoListDetailLiveData.requireValue
    }

    fun deleteVideo(launcher: ActivityResultLauncher<IntentSenderRequest>, videoEntity: VideoEntity) {
        viewModelScope.launch {
            val contentResolver = application.contentResolver
            try {
                contentResolver.delete(videoEntity.uri, null, null)
                deleteUIListVideo(videoEntity)
            } catch (e: SecurityException) {
                val pendingIntent: PendingIntent? = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                        val uris = listOf(videoEntity.uri)
                        MediaStore.createDeleteRequest(contentResolver, uris)
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                        if (e is RecoverableSecurityException) {
                            e.userAction.actionIntent
                        } else {
                            null
                        }
                    }
                    else -> null
                }
                if (pendingIntent != null) {
                    needDeleteVideoEntity = videoEntity
                    val request = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                    launcher.launch(request)
                }
            }
        }
    }

    fun deleteUIListVideo(videoEntity: VideoEntity? = null) {
        val deleteItem = videoEntity ?: needDeleteVideoEntity ?: return
        val newList = videoListDetailLiveData.requireValue.toMutableList()
        if (newList.remove(deleteItem)) {
            videoListDetailLiveData.value = newList
        }
        needDeleteVideoEntity = null
    }

    private fun refreshVideoProgress() {
        viewModelScope.launch {
            val newVideoList = withContext(Dispatchers.IO) {
                val videoList = videoListDetailLiveData.requireValue
                val videoIds = videoList.map { it.id }
                val videoProgressBeans = videoProgressDao.getVideoProgressBeansByIds(videoIds)
                return@withContext bindVideoProgress(videoList, videoProgressBeans)
            }
            videoListDetailLiveData.value = newVideoList
        }
    }

    private fun bindVideoProgress(
        videoList: List<VideoEntity>,
        videoProgressBeans: List<VideoProgressBean>?
    ): List<VideoEntity> {
        val newVideoList = videoList.toMutableList()
        if (videoProgressBeans.isNullOrEmpty()) {
            return newVideoList
        }
        newVideoList.forEachIndexed { index, videoEntity ->
            val videoProgressBean = videoProgressBeans.find { it.id == videoEntity.id }
            if (videoProgressBean != null) {
                newVideoList[index] = videoEntity.copy(progress = videoProgressBean.progress)
            }
        }
        return newVideoList
    }

}