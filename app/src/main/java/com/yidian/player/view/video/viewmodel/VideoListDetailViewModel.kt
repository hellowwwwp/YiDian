package com.yidian.player.view.video.viewmodel

import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yidian.player.base.BaseViewModel
import com.yidian.player.view.video.model.VideoEntity
import kotlinx.coroutines.launch

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2022/1/8
 */
class VideoListDetailViewModel : BaseViewModel() {

    val videoListDetailLiveData: MutableLiveData<List<VideoEntity>> = MutableLiveData(emptyList())

    private var needDeleteVideoEntity: VideoEntity? = null

    fun initVideoList(videoList: List<VideoEntity>) {
        videoListDetailLiveData.value = videoList
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

}