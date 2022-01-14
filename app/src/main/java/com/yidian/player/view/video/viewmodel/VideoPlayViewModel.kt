package com.yidian.player.view.video.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.yidian.player.YiDianApp
import com.yidian.player.base.BaseViewModel
import com.yidian.player.view.video.db.VideoProgressBean
import com.yidian.player.view.video.db.VideoProgressDao
import com.yidian.player.view.video.db.YiDianRoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2022/1/15
 */
class VideoPlayViewModel : BaseViewModel() {

    private val videoProgressDao: VideoProgressDao by lazy {
        val database = YiDianRoomDatabase.get(YiDianApp.application)
        database.getVideoProgressDao()
    }

    fun updateVideoProgress(videoId: Long, position: Long, duration: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val videoProgressBean = VideoProgressBean(
                    videoId, position, duration, System.currentTimeMillis()
                )
                videoProgressDao.insertVideoProgressBean(videoProgressBean)
                Log.e("tag", "updateVideoProgress success: $videoId, $position, $duration")
            }
        }
    }

}