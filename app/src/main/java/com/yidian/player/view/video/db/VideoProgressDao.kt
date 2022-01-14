package com.yidian.player.view.video.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2022/1/15
 */
@Dao
interface VideoProgressDao {

    @Query("select * from t_video_progress where id in (:ids)")
    fun getVideoProgressBeansByIds(ids: List<Long>): List<VideoProgressBean>?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertVideoProgressBean(bean: VideoProgressBean)

}