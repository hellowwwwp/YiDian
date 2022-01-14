package com.yidian.player.base

import android.content.Context
import android.content.Intent
import android.graphics.Outline
import android.net.Uri
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.Toast
import androidx.core.math.MathUtils
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.yidian.player.YiDianApp
import com.yidian.player.utils.ActivityUtils
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 设置 view 的圆角
 */
fun View.setRoundCorner(radius: Float) {
    this.clipToOutline = true
    this.outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, view.width, view.height, radius)
        }
    }
}

/**
 * 显示图片
 */
fun ImageView.displayImage(imageUri: Uri, options: RequestOptions? = null) {
    var requestBuilder = Glide.with(this).load(imageUri)
    if (options != null) {
        requestBuilder = requestBuilder.apply(options)
    }
    requestBuilder.into(this)
}


fun View.setWidthEx(width: Int) {
    layoutParams = layoutParams.also {
        it.width = width
    }
}

fun View.setHeightEx(height: Int) {
    layoutParams = layoutParams.also {
        it.height = height
    }
}

fun View.setWidthAndHeight(width: Int, height: Int) {
    layoutParams = layoutParams.also {
        it.width = width
        it.height = height
    }
}

val Context.layoutInflater: LayoutInflater
    get() = LayoutInflater.from(this)

val View.layoutInflater: LayoutInflater
    get() = LayoutInflater.from(context)

suspend fun View.awaitPost() {
    return suspendCancellableCoroutine { cont: CancellableContinuation<Unit> ->
        val action = object : Runnable {
            override fun run() {
                removeCallbacks(this)
                cont.resume(Unit)
            }
        }
        cont.invokeOnCancellation {
            removeCallbacks(action)
        }
        post(action)
    }
}

suspend fun View.awaitNextLayout() {
    return suspendCancellableCoroutine { cont: CancellableContinuation<Unit> ->
        val listener = object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                view: View, left: Int, top: Int, right: Int, bottom: Int,
                oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
            ) {
                view.removeOnLayoutChangeListener(this)
                cont.resume(Unit)
            }

        }
        cont.invokeOnCancellation {
            removeOnLayoutChangeListener(listener)
        }
        addOnLayoutChangeListener(listener)
    }
}

suspend fun View.awaitGlobalLayout() {
    return suspendCancellableCoroutine { cont: CancellableContinuation<Unit> ->
        val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                cont.resume(Unit)
            }
        }
        cont.invokeOnCancellation {
            viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
        viewTreeObserver.addOnGlobalLayoutListener(listener)
    }
}

fun <T : Parcelable> Intent.getParcelableListExtra(name: String): List<T> {
    return getParcelableArrayListExtra<T>(name)?.toList() ?: emptyList()
}

fun Intent.getIntExtraExt(name: String, defaultValue: Int, minValue: Int, maxValue: Int): Int {
    return MathUtils.clamp(getIntExtra(name, defaultValue), minValue, maxValue)
}

fun View.getFragmentActivity(): FragmentActivity? {
    return ActivityUtils.getActivity(context) as? FragmentActivity
}

fun String.toast(duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(YiDianApp.application, this, duration).show()
}