package com.yidian.player.base

import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

fun <T> DiffUtil.ItemCallback<T>.asConfig(): AsyncDifferConfig<T> {
    return AsyncDifferConfig.Builder(this)
        .setBackgroundThreadExecutor(Dispatchers.IO.asExecutor())
        .build()
}