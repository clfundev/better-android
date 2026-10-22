package io.github.clfundev.better.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.widget.LinearLayout
import android.widget.Scroller
import androidx.annotation.IntDef
import io.github.clfundev.better.R
import kotlin.math.abs

class SlideLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val scroller = Scroller(context)
    private var velocityTracker: VelocityTracker? = null
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var lastX = 0f // 滑动过程中记录上次触碰点X
    private var downX = 0f
    private var downY = 0f // 首次触碰范围
    private var isSlide = false // 是否滑动子View
    private var gestureIgnored = false

    private var mWidth = 0 // 记录layout的宽度，每个子view的宽度都应与该宽度一致
    private var roundCorner = 0
    private val clipRectF = RectF()

    @MenuState
    private var menuState = LEFT_MENU_CLOSED
    private var onMenuStateChangedListener: OnMenuStateChangedListener? = null

    @SuppressLint("UseKtx")
    private val initRunnable: Runnable = Runnable {
        val childCount = getChildCount()
        check(childCount == 3) { "SlideLinearLayout must have 3 child views" }
        mWidth = width
        // 所有child都设成与layout宽度一致
        for (i in 0..<childCount) {
            val child = getChildAt(i)
            val layoutParams = child.layoutParams
            layoutParams.width = mWidth
            child.setLayoutParams(layoutParams)
        }
    }

    init {
        context.obtainStyledAttributes(
            attrs, R.styleable.SlideLinearLayout, defStyleAttr, defStyleRes
        ).use { typedArray ->
            roundCorner = typedArray.getDimensionPixelSize(
                R.styleable.SlideLinearLayout_roundCorner, roundCorner
            )
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        val path = Path()
        // 裁剪区域随着滚动而偏移
        clipRectF.offsetTo(scrollX.toFloat(), scrollY.toFloat())
        path.addRoundRect(
            clipRectF,
            roundCorner.toFloat(),
            roundCorner.toFloat(),
            Path.Direction.CW
        )
        canvas.clipPath(path)
        super.dispatchDraw(canvas)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        clipRectF.set(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            (w - paddingRight).toFloat(),
            (h - paddingBottom).toFloat()
        )
        clipRectF.offsetTo(scrollX.toFloat(), scrollY.toFloat())
        // 初始滚动到中间child的位置
        scrollX = w
        removeCallbacks(initRunnable)
        post(initRunnable)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val x = ev.x
        val y = ev.y
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                // DOWN事件全由onInterceptTouchEvent处理，onTouchEvent中不处理DOWN以防重复处理
                // 判断当前是否正在打开、关闭菜单，如果是则不处理手势
                gestureIgnored = false
                when (menuState) {
                    LEFT_MENU_OPENING, RIGHT_MENU_OPENING, LEFT_MENU_CLOSING, RIGHT_MENU_CLOSING -> {
                        gestureIgnored = true
                        return super.onInterceptTouchEvent(ev)
                    }
                }
                // 创建或重置VelocityTracker
                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain()
                } else {
                    velocityTracker!!.clear()
                }
                velocityTracker!!.addMovement(ev)
                // 如果滚动还没停止，则立即终止滚动
                if (!scroller.isFinished) {
                    scroller.forceFinished(true)
                }
                // 记录DOWN事件的坐标
                downX = x
                downY = y
                // 重置侧滑判断变量
                isSlide = false
            }

            MotionEvent.ACTION_MOVE -> {
                if (gestureIgnored) return super.onInterceptTouchEvent(ev)
                velocityTracker!!.addMovement(ev)
                if (!isSlide) {
                    velocityTracker!!.computeCurrentVelocity(1000)
                    val vx = velocityTracker!!.xVelocity
                    val vy = velocityTracker!!.yVelocity
                    val dx = x - downX
                    val dy = y - downY
                    // 满足以下条件之一则认为是侧滑:
                    // 1.x方向速度大于y方向速度，且达到速度阈值
                    // 2.x方向滑动距离大于y方向滑动距离，且达到距离阈值
                    if ((abs(vx) > abs(vy) && abs(vx) > SNAP_VELOCITY)
                        || (abs(dx) > abs(dy) && abs(dx) > touchSlop)
                    ) {
                        // 判定为侧滑，开始拦截手势，不允许父view拦截手势，开始记录触摸x坐标
                        isSlide = true
                        lastX = x
                        requestDisallowInterceptTouchEvent(true)
                        return true
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                // 如果有子view处理手势且不对其进行拦截，UP事件将在此处理，
                // 所以这里要释放VelocityTracker
                if (gestureIgnored) return super.onInterceptTouchEvent(ev)
                velocityTracker!!.recycle()
                velocityTracker = null
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val x = ev.x
        val y = ev.y
        if (gestureIgnored) return true
        when (ev.action) {
            MotionEvent.ACTION_MOVE -> {
                velocityTracker!!.addMovement(ev)
                if (!isSlide) {
                    // 判断是否为侧滑，逻辑同上
                    velocityTracker!!.computeCurrentVelocity(1000)
                    val vx = velocityTracker!!.xVelocity
                    val vy = velocityTracker!!.yVelocity
                    val dx = x - downX
                    val dy = y - downY
                    if ((abs(vx) > abs(vy) && abs(vx) > SNAP_VELOCITY)
                        || (abs(dx) > abs(dy) && abs(dx) > touchSlop)
                    ) {
                        isSlide = true
                        lastX = x
                        requestDisallowInterceptTouchEvent(true)
                    }
                } else {
                    // 随手指滑动
                    val dx = (lastX - x).toInt()
                    if ((scrollX + dx) in 0..2 * mWidth) {
                        scrollBy(dx, 0)
                    }
                    lastX = x
                }
            }

            MotionEvent.ACTION_UP -> {
                if (isSlide) {
                    velocityTracker!!.computeCurrentVelocity(1000)
                    if (velocityTracker!!.xVelocity <= -SNAP_VELOCITY) {
                        // 用户快速左滑
                        if (scrollX >= mWidth) {
                            smoothOpenRightMenu()
                        } else {
                            smoothCloseMenu()
                        }
                    } else if (velocityTracker!!.xVelocity >= SNAP_VELOCITY) {
                        // 用户快速右滑
                        if (scrollX <= mWidth) {
                            smoothOpenLeftMenu()
                        } else {
                            smoothCloseMenu()
                        }
                    } else if (scrollX >= mWidth + mWidth / 2) {
                        // 右侧菜单露出过半
                        smoothOpenRightMenu()
                    } else if (scrollX <= mWidth / 2) {
                        // 左侧菜单露出过半
                        smoothOpenLeftMenu()
                    } else {
                        // 左、右侧菜单均未露出过半
                        smoothCloseMenu()
                    }
                } else {
                    // 不是侧滑，是点击手势
                    smoothCloseMenu()
                }
                requestDisallowInterceptTouchEvent(false)
                // 释放VelocityTracker
                velocityTracker!!.recycle()
                velocityTracker = null
            }
        }
        return true
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            // 正在滚动
            scrollTo(scroller.currX, scroller.currY)
            invalidate()
        } else if (scroller.currY == scroller.finalY) {
            // 滚动已结束
            when (menuState) {
                LEFT_MENU_OPENING -> {
                    setMenuState(LEFT_MENU_OPENED)
                    // 菜单打开后，略微停留，然后关闭
                    postDelayed({ smoothCloseMenu() }, 100)
                }

                RIGHT_MENU_OPENING -> {
                    setMenuState(RIGHT_MENU_OPENED)
                    // 菜单打开后，略微停留，然后关闭
                    postDelayed({ smoothCloseMenu() }, 100)
                }

                LEFT_MENU_CLOSING -> {
                    setMenuState(LEFT_MENU_CLOSED)
                }

                RIGHT_MENU_CLOSING -> {
                    setMenuState(RIGHT_MENU_CLOSED)
                }
            }
        }
    }

    fun setRoundCorner(roundCorner: Int) {
        if (this.roundCorner != roundCorner) {
            this.roundCorner = roundCorner
            invalidate()
        }
    }

    fun setOnMenuStateChangedListener(onMenuStateChangedListener: OnMenuStateChangedListener?) {
        this.onMenuStateChangedListener = onMenuStateChangedListener
    }

    fun smoothCloseMenu() {
        if (scrollX != mWidth) {
            if (!scroller.isFinished) {
                scroller.forceFinished(true)
            }
            val dx = mWidth - scrollX
            val duration = (abs(dx).toDouble() / mWidth * ANIM_DURATION).toInt()
            scroller.startScroll(scrollX, 0, dx, 0, duration)
            when (menuState) {
                LEFT_MENU_OPENING, LEFT_MENU_OPENED -> setMenuState(LEFT_MENU_CLOSING)
                RIGHT_MENU_OPENING, RIGHT_MENU_OPENED -> setMenuState(RIGHT_MENU_CLOSING)
            }
            invalidate()
        }
    }

    fun smoothOpenLeftMenu() {
        if (scrollX != 0) {
            if (!scroller.isFinished) {
                scroller.forceFinished(true)
            }
            val dx = -scrollX
            val duration = (abs(dx).toDouble() / mWidth * ANIM_DURATION).toInt()
            scroller.startScroll(scrollX, 0, dx, 0, duration)
            setMenuState(LEFT_MENU_OPENING)
            invalidate()
        }
    }

    fun smoothOpenRightMenu() {
        if (scrollX != 2 * mWidth) {
            if (!scroller.isFinished) {
                scroller.forceFinished(true)
            }
            val dx = 2 * mWidth - scrollX
            val duration = (abs(dx).toDouble() / mWidth * ANIM_DURATION).toInt()
            scroller.startScroll(scrollX, 0, dx, 0, duration)
            setMenuState(RIGHT_MENU_OPENING)
            invalidate()
        }
    }

    private fun setMenuState(@MenuState menuState: Int) {
        if (this.menuState == menuState) return
        this.menuState = menuState
        onMenuStateChangedListener?.onMenuStateChanged(menuState)
    }

    interface OnMenuStateChangedListener {
        fun onMenuStateChanged(@MenuState menuState: Int)
    }

    companion object {
        // 解释为侧滑要求的最小滑动速度
        private const val SNAP_VELOCITY = 1000

        // 菜单打开、关闭动画总时长
        private const val ANIM_DURATION = 400

        // 菜单状态常量
        const val LEFT_MENU_CLOSING = 0
        const val LEFT_MENU_CLOSED = 1
        const val RIGHT_MENU_CLOSING = 2
        const val RIGHT_MENU_CLOSED = 3
        const val LEFT_MENU_OPENING = 4
        const val LEFT_MENU_OPENED = 5
        const val RIGHT_MENU_OPENING = 6
        const val RIGHT_MENU_OPENED = 7
    }

    @IntDef(
        LEFT_MENU_CLOSING, LEFT_MENU_CLOSED,
        RIGHT_MENU_CLOSING, RIGHT_MENU_CLOSED,
        LEFT_MENU_OPENING, LEFT_MENU_OPENED,
        RIGHT_MENU_OPENING, RIGHT_MENU_OPENED
    )
    @Retention(AnnotationRetention.SOURCE)
    private annotation class MenuState
}