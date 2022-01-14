package com.yidian.player.view.video.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2022/1/15
 */
@Entity(tableName = "t_video_progress")
data class VideoProgressBean(
    @PrimaryKey
    val id: Long,
    val progress: Long,
    val duration: Long,
    val updateTime: Long
)