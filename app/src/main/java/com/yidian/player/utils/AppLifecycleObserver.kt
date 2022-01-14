package com.yidian.player.utils

import androidx.annotation.CallSuper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2022/1/14
 */
abstract class AppLifecycleObserver : DefaultLifecycleObserver {

    private var isAppBackground: Boolean = false

    @CallSuper
    override fun onStart(owner: LifecycleOwner) {
        if (isAppBackground) {
            onRestart(owner)
        }
        isAppBackground = false
    }

    open fun onRestart(owner: LifecycleOwner) {

    }

    @CallSuper
    override fun onStop(owner: LifecycleOwner) {
        isAppBackground = true
    }

}