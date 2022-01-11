package com.yidian.player.view.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.yidian.player.base.BaseActivity
import com.yidian.player.utils.ActivityHook
import com.yidian.player.view.video.VideoListActivity

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2022/1/7
 */
class SplashActivity : BaseActivity() {

    private val handler: Handler = Handler(Looper.getMainLooper())

    private val toVideoList: Runnable by lazy {
        Runnable {
            startActivity(Intent(this, VideoListActivity::class.java))
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        //修复8.0的设备window不能同时设置为透明和竖屏的问题
        ActivityHook.hookOrientation(this)
        super.onCreate(savedInstanceState)
        handler.postDelayed(toVideoList, 500)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(toVideoList)
    }

}