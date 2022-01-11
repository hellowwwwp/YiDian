package com.yidian.player.view.video.model

import android.net.Uri
import android.os.Parcelable
import androidx.recyclerview.widget.DiffUtil
import com.yidian.player.utils.VideoUtils
import kotlinx.parcelize.Parcelize

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2022/1/8
 */
@Parcelize
data class VideoEntity(
    val id: Long,
    val uri: Uri,
    val filePath: String,
    val fileName: String,
    val title: String,
    val duration: Long = 0,
    val size: Long,
    val updateTime: Long
) : Parcelable, Comparable<VideoEntity> {

    val relativePath: String
        get() = VideoUtils.getRelativePath(filePath)

    val sizeStr: String
        get() = VideoUtils.formatFileSize(size)

    val durationStr: String
        get() = VideoUtils.formatTimeMillis(duration)

    override fun compareTo(other: VideoEntity): Int {
        return updateTime.compareTo(other.updateTime)
    }

    companion object {

        val COMPARATOR = object : DiffUtil.ItemCallback<VideoEntity>() {

            override fun areItemsTheSame(oldItem: VideoEntity, newItem: VideoEntity): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: VideoEntity, newItem: VideoEntity): Boolean {
                return oldItem == newItem
            }

        }

    }

}