package com.yidian.player.view.video.model

import android.os.Parcelable
import androidx.recyclerview.widget.DiffUtil
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2022/1/8
 */
@Parcelize
data class VideoListItemEntity(
    val parentDirName: String,
    val relativePath: String,
    val updateTime: Long,
    val videoList: List<VideoEntity>
) : Parcelable, Comparable<VideoListItemEntity> {

    val dirName: String
        get() = if (parentDirName.isEmpty()) {
            "内部存储"
        } else {
            parentDirName
        }

    val updateTimeStr: String
        get() {
            val format = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            return format.format(updateTime)
        }

    override fun compareTo(other: VideoListItemEntity): Int {
        return dirName.compareTo(other.dirName)
    }

    companion object {

        val COMPARATOR = object : DiffUtil.ItemCallback<VideoListItemEntity>() {

            override fun areItemsTheSame(oldItem: VideoListItemEntity, newItem: VideoListItemEntity): Boolean {
                return oldItem.relativePath == newItem.relativePath
            }

            override fun areContentsTheSame(oldItem: VideoListItemEntity, newItem: VideoListItemEntity): Boolean {
                return oldItem == newItem
            }

        }

    }

}