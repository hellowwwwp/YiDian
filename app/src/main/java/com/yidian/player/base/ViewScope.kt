package com.yidian.player.base

import android.view.View
import com.yidian.player.R
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

val View.viewScope: ViewScope
    get() {
        val existing = getTag(R.id.key_view_scope) as? ViewScope
        if (existing != null) {
            return existing
        }
        val newScope = ViewScope(this, SupervisorJob() + Dispatchers.Main.immediate)
        setTag(R.id.key_view_scope, newScope)
        newScope.register()
        return newScope
    }

class ViewScope(
    private val view: View,
    override val coroutineContext: CoroutineContext
) : CoroutineScope, View.OnAttachStateChangeListener {

    fun register() {
        launch(Dispatchers.Main.immediate) {
            if (view.isAttachedToWindow) {
                view.addOnAttachStateChangeListener(this@ViewScope)
            } else {
                coroutineContext.cancel()
            }
        }
    }

    override fun onViewAttachedToWindow(v: View) {
        //no op
    }

    override fun onViewDetachedFromWindow(v: View) {
        view.removeOnAttachStateChangeListener(this)
        view.setTag(R.id.key_view_scope, null)
        coroutineContext.cancel()
    }

}