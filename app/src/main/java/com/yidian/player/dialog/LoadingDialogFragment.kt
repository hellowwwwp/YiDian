package com.yidian.player.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.blankj.utilcode.util.ScreenUtils
import com.blankj.utilcode.util.SizeUtils
import com.yidian.player.R
import com.yidian.player.base.setWidthEx
import com.yidian.player.databinding.LayoutCommonLoadingDialogBinding

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2022/1/10
 */
class LoadingDialogFragment : DialogFragment() {

    private lateinit var viewBinding: LayoutCommonLoadingDialogBinding

    override fun getTheme(): Int {
        return R.style.CommonDialogAlert
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = LayoutCommonLoadingDialogBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val loadingText = arguments?.getString(KEY_LOADING_TEXT) ?: ""
        viewBinding.loadingTv.text = loadingText
        if (loadingText.isEmpty()) {
            viewBinding.root.setWidthEx(ViewGroup.LayoutParams.WRAP_CONTENT)
        } else {
            viewBinding.root.setWidthEx(ViewGroup.LayoutParams.MATCH_PARENT)
        }
        dialog?.window?.apply {
            attributes = attributes.apply {
                width = if (loadingText.isEmpty()) {
                    WindowManager.LayoutParams.WRAP_CONTENT
                } else {
                    ScreenUtils.getScreenWidth() - SizeUtils.dp2px(80f)
                }
                height = WindowManager.LayoutParams.WRAP_CONTENT
            }
        }
    }

    companion object {

        val TAG: String = LoadingDialogFragment::class.java.name
        const val KEY_LOADING_TEXT = "loading_text"

        fun show(fm: FragmentManager, msg: String? = null, tag: String = TAG) {
            val fragment = LoadingDialogFragment()
            fragment.arguments = bundleOf(Pair(KEY_LOADING_TEXT, msg ?: ""))
            fm.beginTransaction()
                .add(fragment, tag)
                .commitNowAllowingStateLoss()
        }

    }

}