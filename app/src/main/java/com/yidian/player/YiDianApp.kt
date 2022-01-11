package com.yidian.player

import android.app.Application

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2022/1/8
 */
class YiDianApp : Application() {

    override fun onCreate() {
        super.onCreate()
        application = this
    }

    companion object {

        lateinit var application: Application

    }

}