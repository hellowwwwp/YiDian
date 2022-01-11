package com.yidian.player.base

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.yidian.player.YiDianApp

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2022/1/9
 */
abstract class BaseViewModel : ViewModel() {

    protected val application: Application
        get() = YiDianApp.application

    val <T> LiveData<T>.requireValue: T
        get() = value ?: throw NullPointerException("$this value is null")

}