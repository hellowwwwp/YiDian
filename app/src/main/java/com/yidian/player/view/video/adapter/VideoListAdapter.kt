package com.yidian.player.view.video.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yidian.player.base.asConfig
import com.yidian.player.databinding.LayoutVideoListItemBinding
import com.yidian.player.view.video.model.VideoListItemEntity

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2022/1/9
 */
class VideoListAdapter : ListAdapter<VideoListItemEntity, VideoListAdapter.VideoListViewHolder>(
    VideoListItemEntity.COMPARATOR.asConfig()
) {

    var onItemClick: ((Int, VideoListItemEntity) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoListViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return VideoListViewHolder(LayoutVideoListItemBinding.inflate(inflater, parent, false)).apply {
            viewBinding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) {
                    return@setOnClickListener
                }
                val item = getItem(position)
                onItemClick?.invoke(position, item)
            }
        }
    }

    override fun onBindViewHolder(holder: VideoListViewHolder, position: Int) {
        val item = getItem(position)
        holder.viewBinding.run {
            nameTv.text = item.dirName
            countTv.text = "${item.videoList.size} 项"
            updateTimeTv.text = item.updateTimeStr
        }
    }

    class VideoListViewHolder(
        val viewBinding: LayoutVideoListItemBinding
    ) : RecyclerView.ViewHolder(viewBinding.root)
}