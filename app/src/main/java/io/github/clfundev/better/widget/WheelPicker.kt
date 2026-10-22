package io.github.clfundev.better.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.os.Parcelable
import android.text.TextPaint
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.Scroller
import io.github.clfundev.better.R
import io.github.clfundev.better.utils.getLineHeight
import kotlin.math.abs
import kotlin.math.max

/**
 * 滚轮选择器
 * 
 * @param T 选项的类型
 */
@Suppress("unused")
abstract class WheelPicker<T> @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = R.style.DefaultWheelPicker
) : View(context, attrs, defStyleAttr, defStyleRes) {

    // 当前选中项下标
    private var selectedIndex: Int = 0

    private var itemFormatter: ItemFormatter<T>? = null

    private var onSelectListener: OnSelectListener<T>? = null

    private var itemTextSize = 0

    private var itemTextColor = 0

    private val itemTextPaint: TextPaint

    private var selectedItemTextSize = 0

    private var selectedItemTextColor = 0

    private var selectedItemTextStrokeWidth = 0f

    private var selectedItemLetterSpacing = 0f

    private val selectedItemTextPaint: TextPaint

    private var unit: String? = null

    private var unitTextSize = 0

    private var unitTextColor = 0

    private val unitTextPaint: TextPaint

    // 最宽选项文本，用于辅助测量文字最大宽度
    private var maxWidthText: String? = null

    // 用于测量文字尺寸
    private val measureTextPaint: Paint

    // 文字最大宽度，用来保证文字都能显示完全
    private var textMaxWidth = 0

    // 文字最大高度，用来保证文字都能显示完全
    private var textMaxHeight = 0

    // 选项高度方向留余的空间
    private var itemHeightSpace = 0

    // 选项宽度方向留余的空间
    private var itemWidthSpace = 0

    // 单个选项的高度
    private var itemHeight = 0

    // 在选中项上方或下方展示的选项个数
    private var halfDisplayCount = 0

    private var isTextSizeGradual = false

    private var isTextColorGradual = false

    private var isTextAlphaGradual = false

    private var showCurtain = false

    private var curtainColor = 0

    private val curtainPaint: Paint

    private var showSelectedItemDivider = false

    private var selectedItemDividerColor = 0

    private val selectedItemDividerPaint: Paint

    // 绘制区域
    private val drawRect = Rect()

    // 选中项区域
    private val selectedItemRect = Rect()

    // 选项文字x坐标
    private var textX = 0

    // 第一个选项文字y坐标
    private var firstTextY = 0

    // 中央文字y坐标
    private var centerTextY = 0

    // 是否为移动手势
    private var isMoveGesture = false

    // 移动手势判定阈值
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    // 列表是否循环
    private var looped = false

    // 是否在滚动结束后才选中
    private var selectAfterScroll = false

    // 是否在选中时振动
    private var vibrateOnSelect = false

    // 上一个触摸事件的y坐标
    private var lastTouchY = 0

    // 起始触摸事件的y坐标
    private var touchDownY = 0

    // y方向偏移量，控制界面滚动
    private var offsetY = 0

    // Fling滚动范围的最大值
    private var maxFlingY = 0

    // Fling滚动范围的最小值
    private var minFlingY = 0

    private var velocityTracker: VelocityTracker? = null

    private val scroller = Scroller(context)

    private val handler = Handler(Looper.getMainLooper())

    // 负责将scroller的值应用到界面
    private val scrollRunnable: Runnable = object : Runnable {
        override fun run() {
            if (scroller.computeScrollOffset()) {
                // 滚动未结束
                offsetY = scroller.currY
                if (!selectAfterScroll) {
                    selectByOffsetY()
                }
                invalidate()
                handler.postDelayed(this, 15)
            } else if (selectAfterScroll && scroller.currY == scroller.finalY) {
                // 滚动已结束，并且已滚动到目标位置
                // （此时表明用户已松手，因为只有用户松手后，才会使用scroller辅助滚动，
                // 而直至scroller滚动结束用户都没有干涉，才能满足currY==finalY）
                selectByOffsetY()
            }
        }
    }

    init {
        context.obtainStyledAttributes(
            attrs, R.styleable.WheelPicker, defStyleAttr, defStyleRes
        ).use { typedArray ->
            itemTextSize = typedArray.getDimensionPixelSize(
                R.styleable.WheelPicker_itemTextSize, -1
            )
            itemTextColor = typedArray.getColor(
                R.styleable.WheelPicker_itemTextColor, Color.BLACK
            )
            selectedItemTextSize = typedArray.getDimensionPixelSize(
                R.styleable.WheelPicker_selectedItemTextSize, -1
            )
            selectedItemTextColor = typedArray.getColor(
                R.styleable.WheelPicker_selectedItemTextColor, Color.BLACK
            )
            selectedItemTextStrokeWidth = typedArray.getFloat(
                R.styleable.WheelPicker_selectedItemTextStrokeWidth, 0f
            )
            selectedItemLetterSpacing = typedArray.getFloat(
                R.styleable.WheelPicker_selectedItemLetterSpacing, 0f
            )
            unitTextSize = typedArray.getDimensionPixelSize(
                R.styleable.WheelPicker_unitTextSize, -1
            )
            unitTextColor = typedArray.getColor(
                R.styleable.WheelPicker_unitTextColor, Color.BLACK
            )
            unit = typedArray.getString(
                R.styleable.WheelPicker_unit
            )
            maxWidthText = typedArray.getString(
                R.styleable.WheelPicker_maxWidthText
            )
            itemWidthSpace = typedArray.getDimensionPixelSize(
                R.styleable.WheelPicker_itemWidthSpace, 0
            )
            itemHeightSpace = typedArray.getDimensionPixelSize(
                R.styleable.WheelPicker_itemHeightSpace, 0
            )
            halfDisplayCount = typedArray.getInteger(
                R.styleable.WheelPicker_halfDisplayCount, 2
            )
            isTextSizeGradual = typedArray.getBoolean(
                R.styleable.WheelPicker_textSizeGradual, true
            )
            isTextColorGradual = typedArray.getBoolean(
                R.styleable.WheelPicker_textColorGradual, true
            )
            isTextAlphaGradual = typedArray.getBoolean(
                R.styleable.WheelPicker_textAlphaGradual, true
            )
            showCurtain = typedArray.getBoolean(
                R.styleable.WheelPicker_showCurtain, false
            )
            curtainColor = typedArray.getColor(
                R.styleable.WheelPicker_curtainColor, Color.GRAY
            )
            showSelectedItemDivider = typedArray.getBoolean(
                R.styleable.WheelPicker_showSelectedItemDivider, true
            )
            selectedItemDividerColor = typedArray.getColor(
                R.styleable.WheelPicker_selectedItemDividerColor, Color.BLACK
            )
            looped = typedArray.getBoolean(
                R.styleable.WheelPicker_looped, false
            )
            selectAfterScroll = typedArray.getBoolean(
                R.styleable.WheelPicker_selectAfterScroll, false
            )
            vibrateOnSelect = typedArray.getBoolean(
                R.styleable.WheelPicker_vibrateOnSelect, true
            )
        }

        itemTextPaint = TextPaint(
            Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or Paint.LINEAR_TEXT_FLAG
        ).apply {
            style = Paint.Style.FILL_AND_STROKE
            textAlign = Paint.Align.CENTER
            setColor(itemTextColor)
            textSize = itemTextSize.toFloat()
        }
        selectedItemTextPaint = TextPaint(
            Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or Paint.LINEAR_TEXT_FLAG
        ).apply {
            style = Paint.Style.FILL_AND_STROKE
            textAlign = Paint.Align.CENTER
            setColor(selectedItemTextColor)
            textSize = selectedItemTextSize.toFloat()
            strokeWidth = selectedItemTextStrokeWidth
            letterSpacing = selectedItemLetterSpacing
        }
        unitTextPaint = TextPaint(
            Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or Paint.LINEAR_TEXT_FLAG
        ).apply {
            style = Paint.Style.FILL_AND_STROKE
            textAlign = Paint.Align.LEFT
            setColor(unitTextColor)
            textSize = unitTextSize.toFloat()
        }
        curtainPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
            style = Paint.Style.FILL
            setColor(curtainColor)
        }
        selectedItemDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
            style = Paint.Style.STROKE
            setColor(selectedItemDividerColor)
        }
        measureTextPaint = Paint().apply {
            style = Paint.Style.FILL_AND_STROKE
            textAlign = Paint.Align.CENTER
        }
    }

    /**
     * 获取选项的个数
     */
    abstract fun getItemCount(): Int

    /**
     * 获取 [index] 对应的选项
     *
     * 若下标越界 `index < 0 || index >= getItemCount()`，抛出 [IndexOutOfBoundsException]
     */
    abstract fun getItem(index: Int): T

    /**
     * 获取选项 [item] 对应的下标，选项不存在则返回 `-1`
     */
    abstract fun indexOf(item: T): Int

    /**
     * 设置是否开启循环模式
     *
     * @param looped `true` 列表滚动范围"无限制"; `false` 列表滚动范围受限于数据集大小
     */
    fun setLooped(looped: Boolean) {
        if (this.looped != looped) {
            this.looped = looped
            computeFlingLimitY()
            requestLayout()
            invalidate()
        }
    }

    /**
     * 设置是否在滚动结束后才选中对应选项
     *
     * @param selectAfterScroll `true` 滚动结束后选中; `false` 选项一进入中央区域就选中
     */
    fun setSelectAfterScroll(selectAfterScroll: Boolean) {
        this.selectAfterScroll = selectAfterScroll
    }

    /**
     * 设置是否在选中时振动
     *
     * @param vibrateOnSelect 是否在选中时振动
     */
    fun setVibrateOnSelect(vibrateOnSelect: Boolean) {
        this.vibrateOnSelect = vibrateOnSelect
    }

    /**
     * 设置选中监听器
     *
     * @param onSelectListener 选中监听器
     */
    fun setOnSelectListener(onSelectListener: OnSelectListener<T>?) {
        this.onSelectListener = onSelectListener
    }

    /**
     * 获取当前选中项
     */
    fun getSelectedItem(): T = getItem(selectedIndex)

    /**
     * 选中指定选项，选项不存在则不执行任何操作
     *
     * @param item 指定选项
     * @param scroll 是否采用滚动
     */
    fun selectItem(item: T, scroll: Boolean) {
        val index = indexOf(item)
        if (index == -1) return
        selectIndex(index, scroll)
    }

    /**
     * 设置选项格式
     *
     * 若未指定，选择器将用 `toString()` 作为选项文本
     */
    fun setItemFormatter(itemFormatter: ItemFormatter<T>?) {
        this.itemFormatter = itemFormatter
        invalidate()
    }

    /**
     * 设置未选中项的文字大小
     */
    fun setItemTextSize(itemTextSize: Int) {
        if (this.itemTextSize != itemTextSize) {
            this.itemTextSize = itemTextSize
            itemTextPaint.textSize = itemTextSize.toFloat()
            computeTextMaxSize()
            requestLayout()
            invalidate()
        }
    }

    /**
     * 设置未选中项的文字颜色
     */
    fun setItemTextColor(itemTextColor: Int) {
        if (this.itemTextColor != itemTextColor) {
            this.itemTextColor = itemTextColor
            itemTextPaint.setColor(itemTextColor)
            invalidate()
        }
    }

    /**
     * 设置选中项的文字大小
     */
    fun setSelectedItemTextSize(selectedItemTextSize: Int) {
        if (this.selectedItemTextSize != selectedItemTextSize) {
            this.selectedItemTextSize = selectedItemTextSize
            selectedItemTextPaint.textSize = selectedItemTextSize.toFloat()
            computeTextMaxSize()
            requestLayout()
            invalidate()
        }
    }

    /**
     * 设置选中项的文字颜色
     */
    fun setSelectedItemTextColor(selectedItemTextColor: Int) {
        if (this.selectedItemTextColor != selectedItemTextColor) {
            this.selectedItemTextColor = selectedItemTextColor
            selectedItemTextPaint.setColor(selectedItemTextColor)
            invalidate()
        }
    }

    /**
     * 设置单位文字大小
     */
    fun setUnitTextSize(unitTextSize: Int) {
        if (this.unitTextSize != unitTextSize) {
            this.unitTextSize = unitTextSize
            unitTextPaint.textSize = unitTextSize.toFloat()
            invalidate()
        }
    }

    /**
     * 设置单位文字颜色
     */
    fun setUnitTextColor(unitTextColor: Int) {
        if (this.unitTextColor != unitTextColor) {
            this.unitTextColor = unitTextColor
            unitTextPaint.setColor(unitTextColor)
            invalidate()
        }
    }

    /**
     * 设置单位
     */
    fun setUnit(unit: String?) {
        if (this.unit != unit) {
            this.unit = unit
            invalidate()
        }
    }

    /**
     * 设置用于测量文字最大宽度的文本
     */
    fun setMaxWidthText(maxWidthText: String?) {
        if (this.maxWidthText != maxWidthText) {
            this.maxWidthText = maxWidthText
            computeTextMaxSize()
            requestLayout()
            invalidate()
        }
    }

    /**
     * 设置选项高度方向留余的空间
     */
    fun setItemHeightSpace(itemHeightSpace: Int) {
        if (this.itemHeightSpace != itemHeightSpace) {
            this.itemHeightSpace = itemHeightSpace
            requestLayout()
        }
    }

    /**
     * 设置选项宽度方向留余的空间
     */
    fun setItemWidthSpace(itemWidthSpace: Int) {
        if (this.itemWidthSpace != itemWidthSpace) {
            this.itemWidthSpace = itemWidthSpace
            requestLayout()
        }
    }

    /**
     * 设置选中项上下方的可见选项数目
     */
    fun setHalfDisplayCount(halfDisplayCount: Int) {
        if (this.halfDisplayCount != halfDisplayCount) {
            this.halfDisplayCount = halfDisplayCount
            requestLayout()
            invalidate()
        }
    }

    /**
     * 设置文字大小是否渐变
     */
    fun setTextSizeGradual(textSizeGradual: Boolean) {
        if (isTextSizeGradual != textSizeGradual) {
            isTextSizeGradual = textSizeGradual
            invalidate()
        }
    }

    /**
     * 设置文字颜色是否渐变
     */
    fun setTextColorGradual(textColorGradual: Boolean) {
        if (isTextColorGradual != textColorGradual) {
            isTextColorGradual = textColorGradual
            invalidate()
        }
    }

    /**
     * 设置文字透明度是否渐变
     */
    fun setTextAlphaGradual(textAlphaGradual: Boolean) {
        if (isTextAlphaGradual != textAlphaGradual) {
            isTextAlphaGradual = textAlphaGradual
            invalidate()
        }
    }

    /**
     * 设置是否显示幕布
     */
    fun setShowCurtain(showCurtain: Boolean) {
        if (this.showCurtain != showCurtain) {
            this.showCurtain = showCurtain
            invalidate()
        }
    }

    /**
     * 设置幕布颜色
     */
    fun setCurtainColor(curtainColor: Int) {
        if (this.curtainColor != curtainColor) {
            this.curtainColor = curtainColor
            curtainPaint.setColor(curtainColor)
            invalidate()
        }
    }

    /**
     * 设置是否显示选中项分隔线
     */
    fun setShowSelectedItemDivider(showSelectedItemDivider: Boolean) {
        if (this.showSelectedItemDivider != showSelectedItemDivider) {
            this.showSelectedItemDivider = showSelectedItemDivider
            invalidate()
        }
    }

    /**
     * 设置选中项分隔线颜色
     */
    fun setSelectedItemDividerColor(selectedItemDividerColor: Int) {
        if (this.selectedItemDividerColor != selectedItemDividerColor) {
            this.selectedItemDividerColor = selectedItemDividerColor
            selectedItemDividerPaint.setColor(selectedItemDividerColor)
            invalidate()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 不能用float来接收触摸事件的Y坐标，会出问题
        val y = event.y.toInt()
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 停止滚动（此处不能用scroller.abortAnimation()来停止滚动，这会导致
                // selectAfterScroll为true时，用户一旦按下手指停止滚动，finalY==currY
                // 就会成立从而马上选中当前item，而不是用户松手后滚动结束再选中）
                scroller.forceFinished(true)
                // 创建或重置VelocityTracker，开始跟踪手势移动速度
                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain()
                } else {
                    velocityTracker!!.clear()
                }
                velocityTracker!!.addMovement(event)
                lastTouchY = y
                touchDownY = y
                isMoveGesture = false
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isMoveGesture && abs(touchDownY - y) <= touchSlop) {
                    // 手指移动距离未达到阈值，不是移动手势
                    return true
                }
                // 是移动手势
                isMoveGesture = true
                velocityTracker!!.addMovement(event)
                offsetY += y - lastTouchY // 加上手指的位移
                lastTouchY = y
                if (!selectAfterScroll) {
                    selectByOffsetY()
                }
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                if (!isMoveGesture) {
                    // 点击手势，滚动到用户点击的选项
                    var finalY = offsetY - (y - selectedItemRect.centerY())
                    finalY += computeDistanceToEndY(finalY)
                    scroller.startScroll(0, offsetY, 0, finalY - offsetY)
                } else {
                    // 移动手势，计算手势的Y方向速度
                    velocityTracker!!.computeCurrentVelocity(1000, MAX_FLING_VELOCITY.toFloat())
                    val vy = velocityTracker!!.yVelocity.toInt()
                    if (abs(vy) > MIN_FLING_VELOCITY
                        && !(offsetY < minFlingY && vy < 0)
                        && !(offsetY > maxFlingY && vy > 0)
                    ) {
                        // 用户滑动较快，开始fling
                        scroller.fling(0, offsetY, 0, vy, 0, 0, minFlingY, maxFlingY)
                        // 使fling结束在item对齐的位置
                        scroller.finalY += computeDistanceToEndY(scroller.finalY)
                    } else {
                        // 用户滑动较慢，滚动到最近的item对齐位置
                        scroller.startScroll(0, offsetY, 0, computeDistanceToEndY(offsetY))
                    }
                }
                if (!looped) {
                    // 限制滚动不要超出边界
                    if (scroller.finalY > maxFlingY) {
                        scroller.finalY = maxFlingY
                    } else if (scroller.finalY < minFlingY) {
                        scroller.finalY = minFlingY
                    }
                }
                // 开始让界面跟随Scroller的数据滚动
                handler.post(scrollRunnable)
                // 回收VelocityTracker
                velocityTracker!!.recycle()
                velocityTracker = null
            }
        }
        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measuredWidth = resolveSize(
            textMaxWidth + itemWidthSpace + paddingLeft + paddingRight,
            widthMeasureSpec
        )
        val measuredHeight = resolveSize(
            (textMaxHeight + itemHeightSpace) * getDisplayCount() + paddingTop + paddingBottom,
            heightMeasureSpec
        )
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 计算整个控件绘制的区域
        drawRect.set(paddingLeft, paddingTop, w - paddingRight, h - paddingBottom)
        // 计算item的高度
        itemHeight = drawRect.height() / getDisplayCount()
        // 计算文字绘制的X坐标，使文字在item里水平居中
        textX = drawRect.centerX()
        // 计算文字绘制的Y坐标，使文字在item里垂直居中
        firstTextY = drawRect.top + ((itemHeight - (selectedItemTextPaint.ascent()
                + selectedItemTextPaint.descent())) / 2).toInt()
        centerTextY = firstTextY + itemHeight * halfDisplayCount
        // 计算selectedItem的边框位置
        selectedItemRect.set(
            drawRect.left,
            drawRect.top + itemHeight * halfDisplayCount,
            drawRect.right,
            drawRect.top + itemHeight * halfDisplayCount + itemHeight
        )
        // 计算Fling极限
        computeFlingLimitY()
        // 计算offsetY
        offsetY = -itemHeight * selectedIndex
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (showCurtain) {
            canvas.drawRect(selectedItemRect, curtainPaint)
        }
        if (showSelectedItemDivider) {
            val l = selectedItemRect.left.toFloat()
            val t = selectedItemRect.top.toFloat()
            val r = selectedItemRect.right.toFloat()
            val b = selectedItemRect.bottom.toFloat()
            canvas.drawLine(l, t, r, t, selectedItemDividerPaint)
            canvas.drawLine(l, b, r, b, selectedItemDividerPaint)
        }
        val selectedIndex = computeIndex(offsetY)
        // 首尾各多绘制一个用于缓冲
        for (i in selectedIndex - halfDisplayCount - 1..selectedIndex + halfDisplayCount + 1) {
            val itemIndex = if (looped) fixIndex(i) else i
            if (itemIndex < 0 || itemIndex >= getItemCount()) continue
            val item = getItem(itemIndex)
            val itemY = firstTextY + (i + halfDisplayCount) * itemHeight + offsetY
            val yDistance = abs(centerTextY - itemY)
            // 文字颜色渐变(设置颜色要在设置透明度之前，因为颜色包含alpha分量，会覆盖透明度设置)
            if (isTextColorGradual && yDistance < itemHeight) {
                val ratio = 1 - (yDistance / itemHeight.toFloat())
                val color = getGradientColor(itemTextColor, selectedItemTextColor, ratio)
                selectedItemTextPaint.setColor(color)
                itemTextPaint.setColor(color)
            } else {
                selectedItemTextPaint.setColor(selectedItemTextColor)
                itemTextPaint.setColor(itemTextColor)
            }
            // 文字透明度渐变
            if (isTextAlphaGradual) {
                var ratio = if (itemY > centerTextY) {
                    (drawRect.bottom - itemY).toFloat() / (drawRect.bottom - centerTextY)
                } else {
                    (itemY - drawRect.top).toFloat() / (centerTextY - drawRect.top)
                }
                ratio = if (ratio < 0) 0f else ratio
                selectedItemTextPaint.setAlpha((ratio * 255).toInt())
                itemTextPaint.setAlpha((ratio * 255).toInt())
            }
            // 文字大小渐变
            if (isTextSizeGradual && yDistance < itemHeight) {
                val addedSize =
                    ((itemHeight - yDistance) / itemHeight.toFloat() * (selectedItemTextSize - itemTextSize))
                selectedItemTextPaint.textSize = itemTextSize + addedSize
                itemTextPaint.textSize = itemTextSize + addedSize
            } else {
                selectedItemTextPaint.textSize = selectedItemTextSize.toFloat()
                itemTextPaint.textSize = itemTextSize.toFloat()
            }
            // 绘制文字
            val text = itemFormatter?.format(item) ?: item.toString()
            val textPaint = if (i == selectedIndex) selectedItemTextPaint else itemTextPaint
            canvas.drawText(text, textX.toFloat(), itemY.toFloat(), textPaint)
        }
        // 绘制单位
        unit?.let {
            canvas.drawText(it, textX + textMaxWidth / 2f, centerTextY.toFloat(), unitTextPaint)
        }
    }

    override fun onDetachedFromWindow() {
        // 停止滚动释放资源
        handler.removeCallbacks(scrollRunnable)
        super.onDetachedFromWindow()
    }

    override fun onSaveInstanceState(): Parcelable? {
        val savedState = SavedState(super.onSaveInstanceState())
        savedState.selectedIndex = selectedIndex
        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)
            selectIndex(state.selectedIndex, false)
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    /**
     * 计算Fling极限
     *
     * - 如果为循环模式，则为Int极限值
     * - 如果为非循环模式，则基于选项个数和单个选项高度计算
     *
     * 实现类在选项个数变化时，应主动调用此方法更新Fling极限
     */
    protected fun computeFlingLimitY() {
        minFlingY = if (looped) Int.MIN_VALUE else -itemHeight * (getItemCount() - 1)
        maxFlingY = if (looped) Int.MAX_VALUE else 0
    }

    /**
     * 计算文字最大尺寸
     */
    private fun computeTextMaxSize() {
        // 选用更大字体来测量文字
        measureTextPaint.textSize = max(selectedItemTextSize, itemTextSize).toFloat()
        if (!maxWidthText.isNullOrEmpty()) {
            textMaxWidth = measureTextPaint.measureText(maxWidthText).toInt()
        } else if (getItemCount() > 0) {
            // 计算第一个选项的宽度
            val firstText = getItem(0).let { itemFormatter?.format(it) ?: it.toString() }
            textMaxWidth = measureTextPaint.measureText(firstText).toInt()
        }
        textMaxHeight = measureTextPaint.getFontMetrics().getLineHeight().toInt()
    }

    /**
     * 计算指定offsetY与最近的选项对齐位置的距离
     */
    private fun computeDistanceToEndY(offsetY: Int): Int {
        val remainder = offsetY % itemHeight
        return if (remainder <= -itemHeight / 2) {
            -itemHeight - remainder
        } else if (remainder > itemHeight / 2) {
            itemHeight - remainder
        } else {
            -remainder
        }
    }

    /**
     * 根据offsetY计算下标 (循环模式下计算结果还应该用 [fixIndex] 修正)
     */
    private fun computeIndex(offsetY: Int): Int {
        val base = -offsetY + itemHeight / 2
        return if (base < 0) (base / itemHeight - 1) else (base / itemHeight)
    }

    /**
     * 循环模式下将下标修正到 `[0, getItemCount() - 1]` 范围内
     */
    private fun fixIndex(index: Int): Int {
        val r = index % getItemCount()
        return if (r < 0) r + getItemCount() else r
    }

    /**
     * 获取展示的选项个数
     */
    private fun getDisplayCount(): Int = halfDisplayCount * 2 + 1

    /**
     * 计算并返回线性渐变色
     */
    private fun getGradientColor(startColor: Int, endColor: Int, ratio: Float): Int {
        val sR = Color.red(startColor)
        val sG = Color.green(startColor)
        val sB = Color.blue(startColor)
        val eR = Color.red(endColor)
        val eG = Color.green(endColor)
        val eB = Color.blue(endColor)
        val r = (sR + ((eR - sR) * ratio + 0.5)).toInt()
        val g = (sG + ((eG - sG) * ratio + 0.5)).toInt()
        val b = (sB + ((eB - sB) * ratio + 0.5)).toInt()
        return Color.rgb(r, g, b)
    }

    /**
     * 计算 [offsetY] 对应的选项并将其选中
     */
    private fun selectByOffsetY() {
        val i = if (looped) fixIndex(computeIndex(offsetY)) else computeIndex(offsetY)
        if (i < 0 || i >= getItemCount()) return
        if (selectedIndex != i) {
            selectedIndex = i
            onSelectListener?.onSelect(getItem(i), i)
            if (vibrateOnSelect) {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
        }
    }

    /**
     * 选中 [index] 对应选项
     * 
     * @param index 下标
     * @param scroll 是否采用滚动
     */
    private fun selectIndex(index: Int, scroll: Boolean) {
        val i = if (looped) fixIndex(index) else index
        if (i < 0 || i >= getItemCount()) return
        if (selectedIndex != i) {
            if (!scroller.isFinished) {
                scroller.forceFinished(true)
            }
            // 如果itemHeight=0代表还没有绘制完成，无需滚动
            if (scroll && itemHeight != 0) {
                scroller.startScroll(0, offsetY, 0, (selectedIndex - i) * itemHeight)
                // 保证滚动到对齐位置，因为不一定是从对齐位置开始滚动的
                scroller.finalY = -i * itemHeight
                handler.post(scrollRunnable)
            } else {
                // 不进行滚动
                // 此处不能用selectByOffsetY()，因为绘制可能未完成，itemHeight为0
                selectedIndex = i
                offsetY = -itemHeight * selectedIndex
                invalidate()
                onSelectListener?.onSelect(getItem(i), i)
            }
        }
    }

    fun interface OnSelectListener<T> {
        fun onSelect(item: T, index: Int)
    }

    fun interface ItemFormatter<T> {
        fun format(item: T): String
    }

    private class SavedState : BaseSavedState {
        var selectedIndex: Int = 0

        constructor(superState: Parcelable?) : super(superState)

        private constructor(source: Parcel) : super(source) {
            selectedIndex = source.readInt()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(selectedIndex)
        }

        companion object {
            @JvmField
            val CREATOR = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel): SavedState {
                    return SavedState(source)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    companion object {
        private const val TAG = "WheelPicker"

        // Fling最大初始速度
        private const val MAX_FLING_VELOCITY = 10000

        // Fling最小初始速度
        private const val MIN_FLING_VELOCITY = 50
    }
}