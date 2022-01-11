package com.yidian.player.view.video.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.SizeUtils
import com.yidian.player.base.asConfig
import com.yidian.player.base.displayImage
import com.yidian.player.base.setRoundCorner
import com.yidian.player.databinding.LayoutVideoListDetailItemBinding
import com.yidian.player.view.video.model.VideoEntity

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2022/1/9
 */
class VideoListDetailAdapter : ListAdapter<VideoEntity, VideoListDetailAdapter.VideoListDetailViewHolder>(
    VideoEntity.COMPARATOR.asConfig()
) {

    private val coverRound: Float by lazy {
        SizeUtils.dp2px(4f).toFloat()
    }

    var onItemClick: ((Int, VideoEntity) -> Unit)? = null

    var onItemLongClick: ((Int, VideoEntity) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoListDetailViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return VideoListDetailViewHolder(LayoutVideoListDetailItemBinding.inflate(inflater, parent, false)).apply {
            viewBinding.coverFl.setRoundCorner(coverRound)
            //点击
            viewBinding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) {
                    return@setOnClickListener
                }
                val item = getItem(position)
                onItemClick?.invoke(position, item)
            }
            //长按
            viewBinding.root.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) {
                    return@setOnLongClickListener false
                }
                val item = getItem(position)
                onItemLongClick?.invoke(position, item)
                return@setOnLongClickListener true
            }
        }
    }

    override fun onBindViewHolder(holder: VideoListDetailViewHolder, position: Int) {
        val item = getItem(position)
        holder.viewBinding.run {
            coverIv.displayImage(item.uri)
            nameTv.text = item.fileName
            sizeTv.text = item.sizeStr
            durationTv.text = item.durationStr
        }
    }

    class VideoListDetailViewHolder(
        val viewBinding: LayoutVideoListDetailItemBinding
    ) : RecyclerView.ViewHolder(viewBinding.root)

}