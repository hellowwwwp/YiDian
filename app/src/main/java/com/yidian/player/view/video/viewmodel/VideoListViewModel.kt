package com.yidian.player.view.video.viewmodel

import android.annotation.SuppressLint
import android.content.ContentUris
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yidian.player.base.BaseViewModel
import com.yidian.player.utils.VideoUtils
import com.yidian.player.view.video.helper.VideoScanHelper
import com.yidian.player.view.video.model.VideoEntity
import com.yidian.player.view.video.model.VideoListItemEntity
import com.yidian.player.viewstate.VideoListViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2022/1/8
 */
class VideoListViewModel : BaseViewModel(), DefaultLifecycleObserver, VideoScanHelper.OnScanCompletedListener {

    val videoListLiveData: MutableLiveData<List<VideoListItemEntity>> = MutableLiveData(emptyList())

    val videoListStateLiveData: MutableLiveData<VideoListViewState> = MutableLiveData(VideoListViewState())

    private var stoped: Boolean = false

    init {
        VideoScanHelper.addOnScanCompletedListener(this)
    }

    override fun onCleared() {
        super.onCleared()
        VideoScanHelper.removeOnScanCompletedListener(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        if (stoped) {
            refreshVideoListWithoutTips()
        }
        stoped = false
    }

    override fun onStop(owner: LifecycleOwner) {
        stoped = true
    }

    override fun onDestroy(owner: LifecycleOwner) {
        owner.lifecycle.removeObserver(this)
    }

    fun initVideoList() {
        viewModelScope.launch {
            videoListStateLiveData.value = VideoListViewState(isLoading = true)
            val videoList = loadVideos()
            Log.e("tag", "initVideoList: ${videoList.size}")
            val videoListItemList = covertVideoListItemList(videoList)
            videoListStateLiveData.value = VideoListViewState(isLoading = false)
            videoListLiveData.value = videoListItemList
        }
    }

    fun refreshVideoList() {
        viewModelScope.launch {
            videoListStateLiveData.value = VideoListViewState(isRefreshing = true)
            val videoList = loadVideos()
            Log.e("tag", "refreshVideoList: ${videoList.size}")
            val videoDirItemList = covertVideoListItemList(videoList)
            videoListStateLiveData.value = VideoListViewState(isRefreshing = false)
            videoListLiveData.value = videoDirItemList
        }
    }

    fun refreshVideoListWithoutTips() {
        viewModelScope.launch {
            val videoList = loadVideos()
            Log.e("tag", "refreshVideoListWithoutTips: ${videoList.size}")
            val videoListItemList = covertVideoListItemList(videoList)
            videoListLiveData.value = videoListItemList
        }
    }

    private fun covertVideoListItemList(videoList: List<VideoEntity>): List<VideoListItemEntity> {
        val map = mutableMapOf<String, MutableList<VideoEntity>>()
        for (videoEntity in videoList) {
            val videoListItemList = map.getOrPut(videoEntity.relativePath, { mutableListOf() })
            videoListItemList.add(videoEntity)
        }
        val videoListItemList = mutableListOf<VideoListItemEntity>()
        map.entries.forEach {
            val relativePath: String = it.key
            val dirVideoList: List<VideoEntity> = it.value.apply { sort() }
            val parentDirName = relativePath.split(File.separator).last()
            val updateTime = dirVideoList.last().updateTime
            videoListItemList.add(VideoListItemEntity(parentDirName, relativePath, updateTime, dirVideoList))
        }
        return videoListItemList.apply { sort() }
    }

    @SuppressLint("InlinedApi")
    private suspend fun loadVideos(): List<VideoEntity> {
        return withContext(Dispatchers.IO) {
            val projection = mutableListOf(
                MediaStore.Video.VideoColumns._ID,
                MediaStore.Video.VideoColumns.DATA,
                MediaStore.Video.VideoColumns.DISPLAY_NAME,
                MediaStore.Video.VideoColumns.TITLE,
                MediaStore.Video.VideoColumns.SIZE,
                MediaStore.Video.VideoColumns.DATE_ADDED,
                MediaStore.Video.VideoColumns.DURATION,
            )
            val videoList = mutableListOf<VideoEntity>()
            application.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection.toTypedArray(),
                null,
                null,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns._ID)
                val dataColumn = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DATA)
                val nameColumn = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DISPLAY_NAME)
                val titleColumn = cursor.getColumnIndex(MediaStore.Video.VideoColumns.TITLE)
                val sizeColumn = cursor.getColumnIndex(MediaStore.Video.VideoColumns.SIZE)
                val dateAddedColumn = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DATE_ADDED)
                val durationColumn = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val filePath = cursor.getString(dataColumn) ?: ""
                    val name = cursor.getString(nameColumn) ?: ""
                    val title = cursor.getString(titleColumn) ?: ""
                    var size = cursor.getInt(sizeColumn).toLong()
                    if (size < 0) {
                        //获取视频大小
                        size = VideoUtils.getVideoLength(filePath)
                    }
                    val dateAdded = cursor.getLong(dateAddedColumn) * 1000
                    var duration = cursor.getLong(durationColumn)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    if (duration < 0) {
                        //获取视频时长
                        duration = VideoUtils.getVideoDurationWithTimeMillis(contentUri)
                    }
                    videoList.add(VideoEntity(id, contentUri, filePath, name, title, duration, size, dateAdded))
                }
            }
            return@withContext videoList
        }
    }

    override fun onScanCompleted() {

    }

}