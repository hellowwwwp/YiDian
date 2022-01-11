package com.yidian.player.base

import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import com.gyf.immersionbar.ktx.immersionBar
import com.yidian.player.R
import com.yidian.player.dialog.LoadingDialogFragment

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2022/1/7
 */
abstract class BaseActivity : AppCompatActivity() {

    protected open val isCommonBarEnabled: Boolean
        get() = true

    override fun onContentChanged() {
        super.onContentChanged()
        initBar()
    }

    private fun initBar() {
        if (!isCommonBarEnabled) return
        immersionBar {
            fitsSystemWindows(true)
            statusBarColor(R.color.background_normal_primary)
            navigationBarColor(R.color.background_normal_primary)
            statusBarDarkFont(true)
            navigationBarDarkIcon(true)
        }
    }

    protected fun showLoading(msg: String? = null, tag: String = LoadingDialogFragment.TAG) {
        hideLoading()
        val fragment = LoadingDialogFragment()
        if (!msg.isNullOrEmpty()) {
            fragment.arguments = bundleOf(Pair(LoadingDialogFragment.KEY_LOADING_TEXT, msg))
        }
        supportFragmentManager.beginTransaction()
            .add(fragment, tag)
            .commitNowAllowingStateLoss()
    }

    protected fun hideLoading(tag: String = LoadingDialogFragment.TAG) {
        val fragment = supportFragmentManager.findFragmentByTag(tag)
        if (fragment is LoadingDialogFragment && fragment.isAdded) {
            supportFragmentManager.beginTransaction()
                .remove(fragment)
                .commitNowAllowingStateLoss()
        }
    }

}