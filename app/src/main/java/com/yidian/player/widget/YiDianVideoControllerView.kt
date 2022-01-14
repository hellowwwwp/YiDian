package com.yidian.player.widget

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo.*
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.os.*
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.view.View.OnClickListener
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.core.math.MathUtils
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.blankj.utilcode.util.ScreenUtils
import com.blankj.utilcode.util.SizeUtils
import com.blankj.utilcode.util.VibrateUtils
import com.gyf.immersionbar.NotchUtils
import com.yidian.player.R
import com.yidian.player.base.*
import com.yidian.player.databinding.LayoutVideoControllerViewBinding
import com.yidian.player.utils.ActivityUtils
import com.yidian.player.utils.DeviceUtils
import com.yidian.player.utils.VideoUtils
import com.yidian.player.view.video.model.VideoEntity
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2022/1/10
 */
class YiDianVideoControllerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    private val viewBinding = LayoutVideoControllerViewBinding.inflate(layoutInflater, this)

    private val titleTv: TextView
        get() = viewBinding.titleTv

    private val topLayout: View
        get() = viewBinding.topLayout

    private val bottomLayout: View
        get() = viewBinding.bottomLayout

    private val speedLayout: View
        get() = viewBinding.speedLayout

    private val speedTv: TextView
        get() = viewBinding.speedTv

    private val fastPlayLayout: View
        get() = viewBinding.fastPlayLayout

    private val fastPlayTv: TextView
        get() = viewBinding.fastPlayTv

    private val seekBar: SeekBar
        get() = viewBinding.positionSb

    private val positionTv: TextView
        get() = viewBinding.positionTv

    private val durationTv: TextView
        get() = viewBinding.durationTv

    private val playIv: ImageView
        get() = viewBinding.playIv

    private val adjustBrightnessLayout: View
        get() = viewBinding.adjustBrightnessLayout

    private val adjustBrightnessPb: ProgressBar
        get() = viewBinding.adjustBrightnessPb

    private val adjustVolumeLayout: View
        get() = viewBinding.adjustVolumeLayout

    private val adjustVolumePb: ProgressBar
        get() = viewBinding.adjustVolumePb

    private val adjustPositionLayout: View
        get() = viewBinding.adjustPositionLayout

    private val rewindIv: ImageView
        get() = viewBinding.rewindIv

    private val fastForwardIv: ImageView
        get() = viewBinding.fastforwardIv

    private val adjustPositionTv: TextView
        get() = viewBinding.adjustPositionTv

    private val adjustDurationTv: TextView
        get() = viewBinding.adjustDurationTv

    private val adjustPositionPb: ProgressBar
        get() = viewBinding.adjustPositionPb

    private val thumbnailLayout: View
        get() = viewBinding.thumbnailLayout

    private val thumbnailIv: ImageView
        get() = viewBinding.thumbnailIv

    private val thumbnailTv: TextView
        get() = viewBinding.thumbnailTv

    private val fullscreenIv: ImageView
        get() = viewBinding.fullscreenIv

    private val screenLockIv: ImageView
        get() = viewBinding.screenLockIv

    private val screenShotIv: ImageView
        get() = viewBinding.screenShotIv

    private var currentState: PlayState = PlayState.Idle

    private val listeners: MutableList<OnControllerListener> = mutableListOf()

    /**
     * 选中的倍速 itemView
     */
    private var selectedSpeedItemView: View? = null

    /**
     * 选中的倍速
     */
    private var selectedVideoSpeed: VideoSpeed = VideoSpeed.SPEED_1_0

    private var topLayoutAnimation: ViewPropertyAnimator? = null
    private var screenLockLayoutAnimation: ViewPropertyAnimator? = null
    private var screenShotLayoutAnimation: ViewPropertyAnimator? = null
    private var bottomLayoutAnimation: ViewPropertyAnimator? = null
    private var speedLayoutAnimation: ViewPropertyAnimator? = null

    private val animationDuration: Long = 250

    private var isSpeedPanelVisible: Boolean = false

    /**
     * 注册返回按钮的事件监听
     */
    private val onBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            setSpeedLayoutVisible(false)
        }
    }

    /**
     * 控制面板是否显示
     */
    private var isControllerLayoutVisible: Boolean = false

    /**
     * 自动隐藏控制面板的时间间隔
     */
    var hideControllerPanelInterval: Long = 0

    /**
     * 自动隐藏控制面板任务
     */
    private val hideControllerPanelRunnable = object : Runnable {
        override fun run() {
            if (isControllerLayoutVisible) {
                setControllerLayoutVisible(false)
            }
            if (isScreenLockLayoutVisible) {
                setScreenLockLayoutVisible(false)
            }
            //通知隐藏系统栏
            notifySystemBarVisibleChanged(false)
        }
    }

    /**
     * 长按快速播放的速度
     */
    var fastPlayVideoSpeed: VideoSpeed = VideoSpeed.SPEED_2_0

    /**
     * 长按快速播放时震动的时长
     */
    var fastPlayVibrateDuration: Long = 0

    /**
     * 是否正在快速播放
     */
    private var isFastPlay: Boolean = false

    /**
     * 是否在拖拽进度条
     */
    private var isDragSeekBar: Boolean = false

    private var currentPosition: Long = 0
        set(value) {
            field = value
            //设置当前进度
            seekBar.progress = value.toInt()
            adjustPositionPb.progress = value.toInt()
            setPositionText(value)
        }

    private var currentDuration: Long = 0
        set(value) {
            field = value
            //设置进度最大值
            seekBar.max = value.toInt()
            adjustPositionPb.max = value.toInt()
            setDurationText(value)
        }

    private var downRawX: Float = 0f
    private var downRawY: Float = 0f
    private var gestureOperation: GestureOperation? = null

    private var startAdjustBrightness: Int = -1
    private val maxBrightness: Int by lazy {
        ActivityUtils.getSystemMaxBrightness()
    }

    private var startAdjustVolume: Int = -1

    private var startAdjustPosition: Long = -1

    private val screenWidth: Float by lazy {
        ScreenUtils.getScreenWidth().toFloat()
    }
    private val screenHeight: Float by lazy {
        ScreenUtils.getScreenHeight().toFloat()
    }

    /**
     * 手势识别
     */
    private val gestureDetector = GestureDetector(
        context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                downRawX = e.rawX
                downRawY = e.rawY
                gestureOperation = null
                return super.onDown(e)
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (isSpeedPanelVisible) {
                    //隐藏倍速面板
                    setSpeedLayoutVisible(false)
                } else {
                    if (isScreenLocked) {
                        setScreenLockLayoutVisible(!isScreenLockLayoutVisible)
                        //通知系统栏显示和隐藏
                        notifySystemBarVisibleChanged(isScreenLockLayoutVisible)
                    } else {
                        //更新控制面板
                        setControllerLayoutVisible(!isControllerLayoutVisible)
                        setScreenLockLayoutVisible(isControllerLayoutVisible)
                        //通知系统栏显示和隐藏
                        notifySystemBarVisibleChanged(isControllerLayoutVisible)
                    }
                }
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                //更新播放状态
                updatePlayState()
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                //开始快速播放
                startFastPlay()
            }

            override fun onScroll(down: MotionEvent, move: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                if (isScreenLocked || (distanceX == 0f && distanceY == 0f)) {
                    return true
                }
                when (ensureGestureOperation(down, distanceX, distanceY)) {
                    GestureOperation.AdjustBrightness -> {
                        adjustBrightness(move, distanceY)
                    }
                    GestureOperation.AdjustVolume -> {
                        adjustVolume(move, distanceY)
                    }
                    GestureOperation.AdjustPosition -> {
                        adjustPosition(move, distanceX)
                    }
                }
                return true
            }
        }
    )

    /**
     * 视频播放回调
     */
    private val videoPlayListener = object : OnVideoPlayListener {
        override fun onPlayStateChanged(oldState: PlayState, newState: PlayState) {
            currentState = newState
            when (newState) {
                PlayState.Buffering,
                PlayState.Start -> {
                    playIv.isSelected = false
                    postHideControllerPanel()
                }
                PlayState.Pause -> {
                    playIv.isSelected = true
                }
                PlayState.End,
                PlayState.Idle -> {
                    playIv.isSelected = true
                    cancelHideControllerPanel()
                    setControllerLayoutVisible(isVisible = true, animation = false)
                    setScreenLockLayoutVisible(isVisible = true, animation = false)
                }
            }
        }

        override fun onProgressChanged(position: Long, duration: Long) {
            if (!isDragSeekBar) {
                currentPosition = position
            }
        }

        override fun onVideoSourceChanged(videoEntity: VideoEntity, hasPrevious: Boolean, hasNext: Boolean) {
            titleTv.text = videoEntity.fileName
            currentDuration = videoEntity.duration
            setPreviousAndNext(hasPrevious, hasNext)
        }
    }

    private var shortPositionTextWidth: Int = 0
    private var longPositionTextWidth: Int = 0
    private var shortDurationTextWidth: Int = 0
    private var longDurationTextWidth: Int = 0

    private var shortAdjustPositionTextWidth: Int = 0
    private var longAdjustPositionTextWidth: Int = 0
    private var shortAdjustDurationTextWidth: Int = 0
    private var longAdjustDurationTextWidth: Int = 0

    private var isScreenLockLayoutVisible: Boolean = false

    private var isScreenLocked: Boolean = false
        set(value) {
            field = value
            screenLockIv.isSelected = value
            onScreenLockedChanged(value)
        }

    /**
     * 当前屏幕方向
     */
    private var controllerOrientation: Int = ORIENTATION_PORTRAIT

    /**
     * 当前屏幕旋转方向
     */
    private var currentScreenOrientation: ScreenOrientation = ScreenOrientation.TopPortrait

    /**
     * 监听屏幕旋转角度
     */
    private val orientationEventListener = object : OrientationEventListener(context) {
        override fun onOrientationChanged(orientation: Int) {
            if (orientation == ORIENTATION_UNKNOWN) return
            val screenOrientation = when {
                orientation > 315 || orientation <= 45 -> {
                    ScreenOrientation.TopPortrait
                }
                orientation in 225..315 -> {
                    ScreenOrientation.TopLandscape
                }
                orientation in 46..134 -> {
                    ScreenOrientation.BottomLandscape
                }
                else -> {
                    ScreenOrientation.BottomPortrait
                }
            }
            if (currentScreenOrientation != screenOrientation) {
                currentScreenOrientation = screenOrientation
                onScreenOrientationChanged(screenOrientation)
            }
        }
    }

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.YiDianVideoControllerView)
        hideControllerPanelInterval = ta.getInt(
            R.styleable.YiDianVideoControllerView_ycv_hideControllerPanelInterval,
            3000
        ).toLong()
        val fastPlaySpeedIndex = ta.getInt(
            R.styleable.YiDianVideoControllerView_ycv_fastPlaySpeed,
            VideoSpeed.SPEED_2_0.ordinal
        )
        fastPlayVideoSpeed = VideoSpeed.values()[fastPlaySpeedIndex]
        fastPlayVibrateDuration = ta.getInt(
            R.styleable.YiDianVideoControllerView_ycv_fastPlayVibrateDuration,
            50
        ).toLong()
        ta.recycle()
        initView()
        setControllerOrientation(context.resources.configuration.orientation)
        setControllerLayoutVisible(isVisible = true, animation = false)
        setScreenLockLayoutVisible(isVisible = true, animation = false)
        setSpeedLayoutVisible(isVisible = false, animation = false)
        setPreviousAndNext(hasPrevious = false, hasNext = false)
        setAdjustBrightnessLayoutVisible(false)
        setAdjustVolumeLayoutVisible(false)
        setAdjustPositionLayoutVisible(false)
        setThumbnailLayoutVisible(false)
        registerOnBackPressedCallback()
        orientationEventListener.enable()
    }

    private fun initView() {
        with(seekBar) {
            setOnSeekBarChangeListener(object : AbsSeekBarChangeListener() {
                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    isDragSeekBar = true
                }

                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        currentPosition = progress.toLong()
                    }
                }
            })
        }
        with(viewBinding) {
            backIv.setOnClickListener {
                listeners.forEach {
                    it.onBackClick(this@YiDianVideoControllerView)
                }
            }
            screenLockIv.setOnClickListener {
                isScreenLocked = !isScreenLocked
            }
            screenShotIv.setOnClickListener {
                listeners.forEach {
                    it.onScreenShotClick(this@YiDianVideoControllerView)
                }
            }
            previousIv.setOnClickListener {
                listeners.forEach {
                    it.onPreviousClick(this@YiDianVideoControllerView)
                }
            }
            nextIv.setOnClickListener {
                listeners.forEach {
                    it.onNextClick(this@YiDianVideoControllerView)
                }
            }
            playIv.setOnClickListener {
                updatePlayState()
            }
            fullscreenIv.setOnClickListener {
                val newControllerOrientation: Int
                val requestOrientation: Int
                if (controllerOrientation == ORIENTATION_PORTRAIT) {
                    newControllerOrientation = ORIENTATION_LANDSCAPE
                    requestOrientation = SCREEN_ORIENTATION_LANDSCAPE
                } else {
                    newControllerOrientation = ORIENTATION_PORTRAIT
                    requestOrientation = SCREEN_ORIENTATION_PORTRAIT
                }
                setControllerOrientation(newControllerOrientation)
                requestOrientation(requestOrientation)
            }
            speedTv.setOnClickListener {
                setSpeedLayoutVisible(true)
            }
            speedLayout.setOnClickListener {
                setSpeedLayoutVisible(false)
            }
        }

        with(viewBinding) {
            val speedClickListener = OnClickListener { view ->
                selectedSpeedItemView?.isSelected = false
                val speed = when (view) {
                    speed08 -> VideoSpeed.SPEED_0_8
                    speed10 -> VideoSpeed.SPEED_1_0
                    speed125 -> VideoSpeed.SPEED_1_2_5
                    speed15 -> VideoSpeed.SPEED_1_5
                    speed20 -> VideoSpeed.SPEED_2_0
                    speed30 -> VideoSpeed.SPEED_3_0
                    else -> VideoSpeed.SPEED_1_0
                }
                view.isSelected = true
                selectedSpeedItemView = view
                selectedVideoSpeed = speed
                speedTv.text = speed.speedName
                setSpeedLayoutVisible(false)
                listeners.forEach {
                    it.onSpeedChanged(this@YiDianVideoControllerView, speed.speedValue)
                }
            }
            speed08.setOnClickListener(speedClickListener)
            speed10.setOnClickListener(speedClickListener)
            speed125.setOnClickListener(speedClickListener)
            speed15.setOnClickListener(speedClickListener)
            speed20.setOnClickListener(speedClickListener)
            speed30.setOnClickListener(speedClickListener)
            //默认倍速 1.0
            post { speed10.performClick() }
        }
    }

    private fun updateControllerLayoutPadding() {
        Log.e("tag", "updateControllerLayoutPadding: $controllerOrientation")
        if (controllerOrientation == ORIENTATION_PORTRAIT) {
            //竖屏
            val statusBarHeight = DeviceUtils.getStatusBarHeight()
            val navigationBarHeight = DeviceUtils.getNavigationBarHeight()
            setPadding(0, 0, 0, 0)
            val topLayoutPaddingTop = if (statusBarHeight == 0) {
                SizeUtils.dp2px(40f)
            } else {
                statusBarHeight
            }
            topLayout.setPadding(
                topLayout.paddingLeft,
                topLayoutPaddingTop,
                topLayout.paddingRight,
                topLayout.paddingBottom
            )
            val bottomLayoutPaddingBottom = if (navigationBarHeight == 0) {
                SizeUtils.dp2px(10f)
            } else {
                navigationBarHeight + SizeUtils.dp2px(10f)
            }
            bottomLayout.setPadding(
                bottomLayout.paddingLeft,
                bottomLayout.paddingTop,
                bottomLayout.paddingRight,
                bottomLayoutPaddingBottom
            )
            speedLayout.setPadding(
                speedLayout.paddingLeft,
                speedLayout.paddingTop,
                speedLayout.paddingRight,
                bottomLayoutPaddingBottom
            )
        } else {
            //横屏
            val notchHeight = getNotchHeight()
            val statusBarHeight = DeviceUtils.getStatusBarHeight()
            val navigationBarHeight = DeviceUtils.getNavigationBarHeight()
            val contentPaddingLeft: Int
            val contentPaddingRight: Int
            if (currentScreenOrientation.isTop) {
                //横屏, 状态栏在左边
                contentPaddingLeft = max(notchHeight, statusBarHeight)
                contentPaddingRight = navigationBarHeight
            } else {
                //横屏, 状态栏在右边
                contentPaddingLeft = navigationBarHeight
                contentPaddingRight = max(notchHeight, statusBarHeight)
            }
            setPadding(contentPaddingLeft, 0, contentPaddingRight, 0)
            val topLayoutPaddingTop = if (statusBarHeight == 0) {
                SizeUtils.dp2px(40f)
            } else {
                statusBarHeight
            }
            topLayout.setPadding(
                topLayout.paddingLeft,
                topLayoutPaddingTop,
                topLayout.paddingRight,
                topLayout.paddingBottom
            )
            val bottomLayoutPaddingBottom = SizeUtils.dp2px(10f)
            bottomLayout.setPadding(
                bottomLayout.paddingLeft,
                bottomLayout.paddingTop,
                bottomLayout.paddingRight,
                bottomLayoutPaddingBottom
            )
            speedLayout.setPadding(
                speedLayout.paddingLeft,
                speedLayout.paddingTop,
                speedLayout.paddingRight,
                bottomLayoutPaddingBottom
            )
        }
    }

    private fun getNotchHeight(): Int {
        val statusBarHeight = DeviceUtils.getStatusBarHeight()
        val activity = ActivityUtils.getActivity(context)
        if (activity != null) {
            val notchHeight = NotchUtils.getNotchHeight(activity)
            return max(statusBarHeight, notchHeight)
        }
        return statusBarHeight
    }

    private fun registerOnBackPressedCallback() {
        getFragmentActivity()?.apply {
            onBackPressedDispatcher.addCallback(onBackPressedCallback)
        }
    }

    fun bindVideoView(videoView: YiDianVideoView) {
        videoView.addOnVideoPlayListener(videoPlayListener)
    }

    fun unbindVideoView(videoView: YiDianVideoView) {
        videoView.removeOnVideoPlayListener(videoPlayListener)
    }

    private fun setAdjustBrightnessLayoutVisible(isVisible: Boolean) {
        adjustBrightnessLayout.isVisible = isVisible
    }

    private fun setAdjustVolumeLayoutVisible(isVisible: Boolean) {
        adjustVolumeLayout.isVisible = isVisible
    }

    private fun setAdjustPositionLayoutVisible(isVisible: Boolean) {
        adjustPositionLayout.isVisible = isVisible
    }

    private fun setPositionText(position: Long) {
        val positionText = VideoUtils.formatTimeMillis(position)
        val positionTextWidth = when (positionText.length) {
            5 -> {
                if (shortPositionTextWidth == 0) {
                    shortPositionTextWidth = ceil(positionTv.paint.measureText("000:00")).toInt()
                }
                shortPositionTextWidth
            }
            8 -> {
                if (longPositionTextWidth == 0) {
                    longPositionTextWidth = ceil(positionTv.paint.measureText("000:00:00")).toInt()
                }
                longPositionTextWidth
            }
            else -> ViewGroup.LayoutParams.WRAP_CONTENT
        }
        if (positionTv.width != positionTextWidth) {
            positionTv.setWidthEx(positionTextWidth)
        }
        positionTv.text = positionText

        val adjustPositionTextWidth = when (positionText.length) {
            5 -> {
                if (shortAdjustPositionTextWidth == 0) {
                    shortAdjustPositionTextWidth =
                        ceil(adjustPositionTv.paint.measureText("000:00")).toInt()
                }
                shortAdjustPositionTextWidth
            }
            8 -> {
                if (longAdjustPositionTextWidth == 0) {
                    longAdjustPositionTextWidth =
                        ceil(adjustPositionTv.paint.measureText("000:00:00")).toInt()
                }
                longAdjustPositionTextWidth
            }
            else -> ViewGroup.LayoutParams.WRAP_CONTENT
        }
        if (adjustPositionTv.width != adjustPositionTextWidth) {
            adjustPositionTv.setWidthEx(adjustPositionTextWidth)
        }
        adjustPositionTv.text = positionText
    }

    private fun setDurationText(duration: Long) {
        val durationText = VideoUtils.formatTimeMillis(duration)
        val durationTextWidth = when (durationText.length) {
            5 -> {
                if (shortDurationTextWidth == 0) {
                    shortDurationTextWidth =
                        ceil(durationTv.paint.measureText("000:00")).toInt()
                }
                shortDurationTextWidth
            }
            8 -> {
                if (longDurationTextWidth == 0) {
                    longDurationTextWidth =
                        ceil(durationTv.paint.measureText("000:00:00")).toInt()
                }
                longDurationTextWidth
            }
            else -> ViewGroup.LayoutParams.WRAP_CONTENT
        }
        if (durationTv.width != durationTextWidth) {
            durationTv.setWidthEx(durationTextWidth)
        }
        durationTv.text = durationText

        val adjustDurationTextWidth = when (durationText.length) {
            5 -> {
                if (shortAdjustDurationTextWidth == 0) {
                    shortAdjustDurationTextWidth =
                        ceil(adjustDurationTv.paint.measureText("000:00")).toInt()
                }
                shortAdjustDurationTextWidth
            }
            8 -> {
                if (longAdjustDurationTextWidth == 0) {
                    longAdjustDurationTextWidth =
                        ceil(adjustDurationTv.paint.measureText("000:00:00")).toInt()
                }
                longAdjustDurationTextWidth
            }
            else -> ViewGroup.LayoutParams.WRAP_CONTENT
        }
        if (adjustDurationTv.width != adjustDurationTextWidth) {
            adjustDurationTv.setWidthEx(adjustDurationTextWidth)
        }
        adjustDurationTv.text = durationText
    }

    private fun setPreviousAndNext(hasPrevious: Boolean, hasNext: Boolean) {
        with(viewBinding) {
            previousIv.isInvisible = !hasPrevious
            nextIv.isInvisible = !hasNext
        }
    }

    private fun updatePlayState() {
        when (currentState) {
            PlayState.Start -> {
                //当前在播放, 那就暂停
                listeners.forEach {
                    it.onPauseClick(this@YiDianVideoControllerView)
                }
            }
            PlayState.Pause -> {
                //当前在暂停, 那就开始播放
                listeners.forEach {
                    it.onPlayClick(this@YiDianVideoControllerView)
                }
            }
            PlayState.End -> {
                //当前播放已完成, 那就重新播放
                listeners.forEach {
                    it.onRePlayClick(this@YiDianVideoControllerView)
                }
            }
            else -> Unit
        }
    }

    fun addOnControllerListener(listener: OnControllerListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeOnControllerListener(listener: OnControllerListener) {
        listeners.remove(listener)
    }

    private fun notifySystemBarVisibleChanged(isVisible: Boolean) {
        listeners.forEach {
            it.trySystemBarVisibleChanged(isVisible)
        }
    }

    private fun setScreenLockLayoutVisible(isVisible: Boolean, animation: Boolean = true) {
        isScreenLockLayoutVisible = isVisible
        if (isVisible) {
            if (animation) {
                showScreenLockLayoutAnimation(true)
            } else {
                screenLockIv.isVisible = true
                screenLockIv.alpha = 1f
            }
        } else {
            if (animation) {
                showScreenLockLayoutAnimation(false)
            } else {
                screenLockIv.isVisible = false
            }
        }
    }

    private fun setScreenShotLayoutVisible(isVisible: Boolean, animation: Boolean = true) {
        if (isVisible) {
            if (animation) {
                showScreenShotLayoutAnimation(true)
            } else {
                screenShotIv.isVisible = true
                screenShotIv.alpha = 1f
            }
        } else {
            if (animation) {
                showScreenShotLayoutAnimation(false)
            } else {
                screenShotIv.isVisible = false
            }
        }
    }

    fun setControllerLayoutVisible(isVisible: Boolean, animation: Boolean = true) {
        isControllerLayoutVisible = isVisible
        if (isVisible && isSpeedPanelVisible) {
            setSpeedLayoutVisible(isVisible = false, animation = false)
        }
        if (isVisible) {
            if (animation) {
                showTopLayoutAnimation(true)
                showBottomLayoutAnimation(true)
            } else {
                topLayout.isVisible = true
                topLayout.alpha = 1f
                topLayout.translationY = 0f
                bottomLayout.isVisible = true
                bottomLayout.alpha = 1f
                bottomLayout.translationY = 0f
            }
        } else {
            if (animation) {
                showTopLayoutAnimation(false)
                showBottomLayoutAnimation(false)
            } else {
                topLayout.isVisible = false
                bottomLayout.isVisible = false
            }
        }
        setScreenShotLayoutVisible(isVisible, animation)
    }

    private fun postHideControllerPanel() {
        removeCallbacks(hideControllerPanelRunnable)
        postDelayed(hideControllerPanelRunnable, hideControllerPanelInterval)
    }

    private fun cancelHideControllerPanel() {
        removeCallbacks(hideControllerPanelRunnable)
    }

    fun setSpeedLayoutVisible(isVisible: Boolean, animation: Boolean = true) {
        this.isSpeedPanelVisible = isVisible
        onBackPressedCallback.isEnabled = isVisible
        if (isVisible) {
            setControllerLayoutVisible(isVisible = false, animation = false)
            setScreenLockLayoutVisible(isVisible = false, animation = false)
            if (animation) {
                showSpeedLayoutAnimation(true)
            } else {
                speedLayout.isVisible = true
            }
        } else {
            if (animation) {
                showSpeedLayoutAnimation(false)
            } else {
                speedLayout.isVisible = false
            }
        }
    }

    private fun cancelTopLayoutAnimationIfNeed() {
        topLayoutAnimation?.apply {
            cancel()
            setListener(null)
            topLayoutAnimation = null
        }
    }

    private fun showTopLayoutAnimation(isVisible: Boolean) {
        cancelTopLayoutAnimationIfNeed()
        viewScope.launch {
            if (topLayout.isVisible) {
                topLayout.awaitPost()
            } else {
                topLayout.isInvisible = true
                topLayout.awaitPost()
            }
            val endAlpha = if (isVisible) 1f else 0f
            val endTranY = if (isVisible) 0f else -topLayout.height.toFloat()
            topLayout.animate().apply {
                translationY(endTranY)
                alpha(endAlpha)
                duration = animationDuration
                setListener(object : SimpleAnimatorListener() {
                    override fun onAnimationStart(animation: Animator) {
                        if (isVisible) {
                            topLayout.isVisible = true
                        }
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        if (!isVisible) {
                            topLayout.isVisible = false
                        }
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        if (!isVisible) {
                            topLayout.isVisible = false
                        }
                    }
                })
                start()
                topLayoutAnimation = this
            }
        }
    }

    private fun cancelScreenLockLayoutAnimationIfNeed() {
        screenLockLayoutAnimation?.apply {
            cancel()
            setListener(null)
            screenLockLayoutAnimation = null
        }
    }

    private fun showScreenLockLayoutAnimation(isVisible: Boolean) {
        cancelScreenLockLayoutAnimationIfNeed()
        viewScope.launch {
            screenLockIv.awaitPost()
            val endAlpha = if (isVisible) 1f else 0f
            screenLockIv.animate().apply {
                alpha(endAlpha)
                duration = animationDuration
                setListener(object : SimpleAnimatorListener() {
                    override fun onAnimationStart(animation: Animator) {
                        if (isVisible) {
                            screenLockIv.isVisible = true
                        }
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        if (!isVisible) {
                            screenLockIv.isVisible = false
                        }
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        if (!isVisible) {
                            screenLockIv.isVisible = false
                        }
                    }
                })
                start()
                screenLockLayoutAnimation = this
            }
        }
    }

    private fun cancelScreenShotLayoutAnimationIfNeed() {
        screenShotLayoutAnimation?.apply {
            cancel()
            setListener(null)
            screenShotLayoutAnimation = null
        }
    }

    private fun showScreenShotLayoutAnimation(isVisible: Boolean) {
        cancelScreenLockLayoutAnimationIfNeed()
        viewScope.launch {
            screenShotIv.awaitPost()
            val endAlpha = if (isVisible) 1f else 0f
            screenShotIv.animate().apply {
                alpha(endAlpha)
                duration = animationDuration
                setListener(object : SimpleAnimatorListener() {
                    override fun onAnimationStart(animation: Animator) {
                        if (isVisible) {
                            screenShotIv.isVisible = true
                        }
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        if (!isVisible) {
                            screenShotIv.isVisible = false
                        }
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        if (!isVisible) {
                            screenShotIv.isVisible = false
                        }
                    }
                })
                start()
                screenShotLayoutAnimation = this
            }
        }
    }

    private fun cancelBottomLayoutAnimationIfNeed() {
        bottomLayoutAnimation?.apply {
            cancel()
            setListener(null)
            bottomLayoutAnimation = null
        }
    }

    private fun showBottomLayoutAnimation(isVisible: Boolean) {
        cancelBottomLayoutAnimationIfNeed()
        viewScope.launch {
            if (bottomLayout.isVisible) {
                bottomLayout.awaitPost()
            } else {
                bottomLayout.isInvisible = true
                bottomLayout.awaitPost()
            }
            val endAlpha = if (isVisible) 1f else 0f
            val endTranY = if (isVisible) 0f else bottomLayout.height.toFloat()
            bottomLayout.animate().apply {
                alpha(endAlpha)
                translationY(endTranY)
                duration = animationDuration
                setListener(object : SimpleAnimatorListener() {
                    override fun onAnimationStart(animation: Animator) {
                        if (isVisible) {
                            bottomLayout.isVisible = true
                        }
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        if (!isVisible) {
                            bottomLayout.isVisible = false
                        }
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        if (!isVisible) {
                            bottomLayout.isVisible = false
                        }
                    }
                })
                start()
                bottomLayoutAnimation = this
            }
        }
    }

    private fun cancelSpeedLayoutAnimationIfNeed() {
        speedLayoutAnimation?.apply {
            cancel()
            setListener(null)
            speedLayoutAnimation = null
        }
    }

    private fun showSpeedLayoutAnimation(isVisible: Boolean) {
        cancelSpeedLayoutAnimationIfNeed()
        viewScope.launch {
            if (speedLayout.isVisible) {
                speedLayout.awaitPost()
            } else {
                speedLayout.isInvisible = true
                speedLayout.awaitPost()
            }
            val endAlpha = if (isVisible) 1f else 0f
            val endTranY = if (isVisible) 0f else speedLayout.height.toFloat()
            if (isVisible && speedLayout.translationY == 0f) {
                speedLayout.translationY = speedLayout.height.toFloat()
            }
            speedLayout.animate().apply {
                alpha(endAlpha)
                translationY(endTranY)
                duration = animationDuration
                setListener(object : SimpleAnimatorListener() {
                    override fun onAnimationStart(animation: Animator) {
                        if (isVisible) {
                            speedLayout.isVisible = true
                        }
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        if (!isVisible) {
                            speedLayout.isVisible = false
                        }
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        if (!isVisible) {
                            speedLayout.isVisible = false
                        }
                    }
                })
                start()
                speedLayoutAnimation = this
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                //手指按下时取消隐藏控制面板
                cancelHideControllerPanel()
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                //手指抬起时开始隐藏控制面板
                if (currentState != PlayState.Idle && currentState != PlayState.End) {
                    postHideControllerPanel()
                }
                //手指抬起时停止快速播放
                if (isFastPlay) {
                    stopFastPlay()
                }
                //重置调整亮度的初始值
                startAdjustBrightness = -1
                //手指抬起时隐藏调整亮度面板
                setAdjustBrightnessLayoutVisible(false)
                //重置调整音量的初始值
                startAdjustVolume = -1
                setAdjustVolumeLayoutVisible(false)
                //手指抬起时调整进度
                if (isDragSeekBar) {
                    isDragSeekBar = false
                    val position = seekBar.progress.toLong()
                    listeners.forEach {
                        it.onSeekChanged(this, position)
                    }
                }
                //重置调整进度初始值
                startAdjustPosition = -1L
                //手指抬起时隐藏调整进度面板
                setAdjustPositionLayoutVisible(false)
                //手指抬起时隐藏预览帧面板
                setThumbnailLayoutVisible(false)
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelTopLayoutAnimationIfNeed()
        cancelScreenLockLayoutAnimationIfNeed()
        cancelScreenShotLayoutAnimationIfNeed()
        cancelBottomLayoutAnimationIfNeed()
        cancelSpeedLayoutAnimationIfNeed()
        orientationEventListener.disable()
    }

    private fun startFastPlay() {
        VibrateUtils.vibrate(fastPlayVibrateDuration)
        isFastPlay = true
        fastPlayLayout.isVisible = true
        fastPlayTv.text = "${fastPlayVideoSpeed.speedName} 加速中"
        speedTv.text = fastPlayVideoSpeed.speedName
        listeners.forEach {
            it.onSpeedChanged(this, fastPlayVideoSpeed.speedValue)
        }
    }

    private fun stopFastPlay() {
        isFastPlay = false
        fastPlayLayout.isVisible = false
        speedTv.text = selectedVideoSpeed.speedName
        listeners.forEach {
            it.onSpeedChanged(this, selectedVideoSpeed.speedValue)
        }
    }

    interface OnControllerListener {

        fun onBackClick(controllerView: YiDianVideoControllerView)

        fun onScreenShotClick(controllerView: YiDianVideoControllerView)

        fun onSeekChanged(controllerView: YiDianVideoControllerView, currentPosition: Long)

        fun onPreviousClick(controllerView: YiDianVideoControllerView)

        fun onNextClick(controllerView: YiDianVideoControllerView)

        fun onPlayClick(controllerView: YiDianVideoControllerView)

        fun onPauseClick(controllerView: YiDianVideoControllerView)

        fun onRePlayClick(controllerView: YiDianVideoControllerView)

        fun onSpeedChanged(controllerView: YiDianVideoControllerView, speed: Float)

        fun trySystemBarVisibleChanged(isVisible: Boolean)

    }

    /**
     * 调整亮度
     */
    private fun adjustBrightness(move: MotionEvent, distanceY: Float) {
        val activity = ActivityUtils.getActivity(context) ?: return
        if (startAdjustBrightness == -1) {
            //记录刚开始触发拖拽时的亮度
            val windowBrightness = activity.window.attributes.screenBrightness
            startAdjustBrightness = if (windowBrightness != WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) {
                (windowBrightness * maxBrightness).toInt()
            } else {
                ActivityUtils.getSystemCurrentBrightness()
            }
        }
        //计算当前的屏幕亮度值
        var percent = ((downRawY - move.rawY) / screenHeight) * 2f
        percent = fixMinStepValue(percent, maxBrightness.toLong())
        val newBrightness = MathUtils.clamp(
            ((startAdjustBrightness + (maxBrightness * percent)).toInt()),
            0,
            maxBrightness
        )
        if (newBrightness == 0 || newBrightness == maxBrightness) {
            downRawY = move.rawY
            startAdjustBrightness = newBrightness
        }
        adjustBrightnessPb.max = maxBrightness
        adjustBrightnessPb.progress = newBrightness
        setAdjustBrightnessLayoutVisible(true)
        //设置屏幕亮度
        val windowBrightness = (newBrightness * 1f / maxBrightness)
        ActivityUtils.setWindowBrightness(activity.window, windowBrightness)
    }

    /**
     * 调整音量
     */
    private fun adjustVolume(move: MotionEvent, distanceY: Float) {
        if (startAdjustVolume == -1) {
            startAdjustVolume = ActivityUtils.getCurrentVolume()
        }
        //计算当前的音量值
        val maxVolume = ActivityUtils.getMaxVolume()
        var percent = ((downRawY - move.rawY) / screenHeight) * 2f
        percent = fixMinStepValue(percent, maxVolume.toLong())
        val newVolume = MathUtils.clamp(
            ((startAdjustVolume + (maxVolume * percent)).toInt()),
            0,
            maxVolume
        )
        Log.e("tag", "startAdjustVolume: $startAdjustVolume, percent: $percent, newVolume: $newVolume")
        if (newVolume == 0 || newVolume == maxVolume) {
            downRawY = move.rawY
            startAdjustVolume = newVolume
        }
        adjustVolumePb.max = maxVolume
        adjustVolumePb.progress = newVolume
        setAdjustVolumeLayoutVisible(true)
        //设置音量
        ActivityUtils.setVolume(newVolume)
    }

    /**
     * 调整进度
     */
    private fun adjustPosition(move: MotionEvent, distanceX: Float) {
        isDragSeekBar = true
        if (startAdjustPosition == -1L) {
            //记录刚开始触发拖拽时的位置
            startAdjustPosition = currentPosition
        }
        //计算当前滑动的总距离
        var percent = ((move.rawX - downRawX) / screenWidth) * 0.5f
        percent = fixMinStepValue(percent, currentDuration)
        //设置滑动到的位置: 开始滑动的位置 + 滑动了的位置
        val newPosition = MathUtils.clamp(
            ((startAdjustPosition + (currentDuration * percent)).toLong()),
            0L,
            currentDuration
        )
        if (newPosition == 0L || newPosition == currentDuration) {
            downRawX = move.rawX
            startAdjustPosition = newPosition
        }
        currentPosition = newPosition
        //控制快进和快退图标的显示
        fastForwardIv.isInvisible = distanceX > 0
        rewindIv.isInvisible = distanceX <= 0
        setAdjustPositionLayoutVisible(true)
    }

    /**
     * 修复最小步进值的问题
     */
    private fun fixMinStepValue(value: Float, max: Long): Float {
        val minStepValue = 1f / max
        return if (value != 0f && abs(value) < minStepValue) {
            if (value > 0) {
                minStepValue
            } else {
                -minStepValue
            }
        } else {
            value
        }
    }

    private fun setThumbnailLayoutVisible(isVisible: Boolean) {
        thumbnailLayout.isVisible = isVisible
    }

    /**
     * 确定手势交互操作
     */
    private fun ensureGestureOperation(down: MotionEvent, distanceX: Float, distanceY: Float): GestureOperation {
        return gestureOperation ?: kotlin.run {
            if (abs(distanceX) >= abs(distanceY)) {
                //横向滑动
                GestureOperation.AdjustPosition
            } else {
                //纵向滑动
                if (down.rawX >= screenWidth * 0.5f) {
                    GestureOperation.AdjustVolume
                } else {
                    GestureOperation.AdjustBrightness
                }
            }
        }.also {
            gestureOperation = it
        }
    }

    private enum class GestureOperation {
        AdjustPosition, AdjustBrightness, AdjustVolume
    }

    private fun requestOrientation(orientation: Int) {
        ActivityUtils.getActivity(context)?.requestedOrientation = orientation
    }

    private fun setControllerOrientation(orientation: Int) {
        Log.e("tag", "setControllerOrientation: $orientation")
        controllerOrientation = orientation
        fullscreenIv.isSelected = orientation == ORIENTATION_LANDSCAPE
        updateControllerLayoutPadding()
    }

    private fun onScreenOrientationChanged(screenOrientation: ScreenOrientation) {
        if (!DeviceUtils.isAutoRotateOn() || isScreenLocked) {
            return
        }
        Log.e("tag", "onScreenOrientationChanged: $screenOrientation")
        if (screenOrientation.isPortrait) {
            setControllerOrientation(ORIENTATION_PORTRAIT)
            requestOrientation(SCREEN_ORIENTATION_PORTRAIT)
        } else {
            setControllerOrientation(ORIENTATION_LANDSCAPE)
            if (screenOrientation.isTop) {
                requestOrientation(SCREEN_ORIENTATION_LANDSCAPE)
            } else {
                requestOrientation(SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
            }
        }
    }

    private fun onScreenLockedChanged(isLocked: Boolean) {
        //设置控制面板的显示和隐藏
        setControllerLayoutVisible(!isLocked)
        //通知系统栏显示和隐藏
        notifySystemBarVisibleChanged(!isLocked)
        if (!isLocked) {
            //回调一次屏幕方向改变
            onScreenOrientationChanged(currentScreenOrientation)
        }
    }

    private enum class ScreenOrientation(val isTop: Boolean, val isPortrait: Boolean) {
        TopPortrait(true, true),
        TopLandscape(true, false),
        BottomPortrait(false, true),
        BottomLandscape(false, false),
    }

}