package io.github.clfundev.better.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import io.github.clfundev.better.R
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * 圆环型进度条
 */
class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = R.style.DefaultCircularProgressView
) : View(context, attrs, defStyleAttr, defStyleRes) {

    private var showProgress = true
    private var showText = false
    private var text = ""
    private var progress = 0f

    private var antiAlias = true
    private var startAngle = 270f
    private var sweepAngle = 360f
    private var textColor = Color.BLACK
    private var textSize = -1
    private var arcColor = Color.BLACK
    private var arcWidth = 1
    private var bgArcColor = Color.BLACK
    private var bgArcWidth = 1

    private val centerPointF = PointF() // 圆心坐标
    private var textY = 0f // 文字绘制时的y坐标
    private val arcRectF = RectF() // 圆弧矩形边界

    private val textPaint: TextPaint
    private val arcPaint: Paint
    private val bgArcPaint: Paint

    private val animator: ValueAnimator

    init {
        context.obtainStyledAttributes(
            attrs,
            R.styleable.CircularProgressView,
            defStyleAttr,
            defStyleRes
        ).use { typedArray ->
            showProgress = typedArray.getBoolean(
                R.styleable.CircularProgressView_showProgress, showProgress
            )
            showText = typedArray.getBoolean(
                R.styleable.CircularProgressView_showText, showText
            )
            antiAlias = typedArray.getBoolean(
                R.styleable.CircularProgressView_antiAlias, antiAlias
            )
            startAngle = typedArray.getFloat(
                R.styleable.CircularProgressView_startAngle, startAngle
            )
            sweepAngle = typedArray.getFloat(
                R.styleable.CircularProgressView_sweepAngle, sweepAngle
            )
            textColor = typedArray.getColor(
                R.styleable.CircularProgressView_textColor, textColor
            )
            textSize = typedArray.getDimensionPixelSize(
                R.styleable.CircularProgressView_textSize, textSize
            )
            arcColor = typedArray.getColor(
                R.styleable.CircularProgressView_fgArcColor, arcColor
            )
            arcWidth = typedArray.getDimensionPixelSize(
                R.styleable.CircularProgressView_fgArcWidth, arcWidth
            )
            bgArcColor = typedArray.getColor(
                R.styleable.CircularProgressView_bgArcColor, bgArcColor
            )
            bgArcWidth = typedArray.getDimensionPixelSize(
                R.styleable.CircularProgressView_bgArcWidth, bgArcWidth
            )
        }

        // 初始化动画器
        animator = ValueAnimator().apply {
            interpolator = LinearInterpolator() // 匀速
            addUpdateListener { animation ->
                progress = animation.getAnimatedValue() as Float
                invalidate()
            }
        }

        // 初始化Paint
        textPaint = TextPaint().apply {
            isAntiAlias = antiAlias
            setColor(textColor)
            textSize = textSize
            textAlign = Paint.Align.CENTER
        }
        arcPaint = Paint().apply {
            isAntiAlias = antiAlias
            setColor(arcColor)
            strokeWidth = arcWidth.toFloat()
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        bgArcPaint = Paint().apply {
            isAntiAlias = antiAlias
            setColor(bgArcColor)
            strokeWidth = bgArcWidth.toFloat()
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSpecSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSpecSize = MeasureSpec.getSize(heightMeasureSpec)
        val desiredSize = min(widthSpecSize, heightSpecSize)
        setMeasuredDimension(desiredSize, desiredSize)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 计算圆心
        centerPointF.x = floor(w / 2f)
        centerPointF.y = floor(h / 2f)
        // 求圆弧和背景圆弧的最大宽度
        val maxArcWidth = max(arcWidth, bgArcWidth)
        // 求更小值作为半径（因为圆弧应当贴合最窄的边界）
        val r = min(
            w - paddingLeft - paddingRight - maxArcWidth,
            h - paddingTop - paddingBottom - maxArcWidth
        ) / 2f
        // 计算圆弧边界矩形
        arcRectF.left = centerPointF.x - r
        arcRectF.top = centerPointF.y - r
        arcRectF.right = centerPointF.x + r
        arcRectF.bottom = centerPointF.y + r
        // 计算文字绘制时的y坐标
        textY = centerPointF.y - (textPaint.ascent() + textPaint.descent()) / 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        with(canvas) {
            if (showText) {
                // 由于Paint已设为居中绘制，无需把文字的x绘制坐标设为centerPoint.x - textWidth / 2
                drawText(text, centerPointF.x, textY, textPaint)
            }
            if (showProgress) {
                // 从进度圆弧结束的地方开始重新绘制，优化性能
                save()
                // 画背景圆弧
                drawArc(arcRectF, startAngle, sweepAngle, false, bgArcPaint)
                // 画圆弧
                drawArc(arcRectF, startAngle, sweepAngle * progress, false, arcPaint)
            }
        }
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    fun setShowProgress(showProgress: Boolean) {
        if (this.showProgress != showProgress) {
            this.showProgress = showProgress
            invalidate()
        }
    }

    fun setShowText(showText: Boolean) {
        if (this.showText != showText) {
            this.showText = showText
            invalidate()
        }
    }

    fun setText(text: String) {
        this.text = text
        invalidate()
    }

    /**
     * 设置进度条进度，范围在[0,1]之间（超出范围会被修正到范围内）
     * 该方法会取消正在运行的进度条动画
     */
    fun setProgress(progress: Float) {
        animator.cancel()
        this.progress = fixProgress(progress)
        invalidate()
    }

    /**
     * 启动进度条动画
     * 
     * @param start    初始进度（会被修正到[0,1]）
     * @param end      结束进度（会被修正到[0,1]）
     * @param animTime 动画时长
     */
    fun startAnimator(start: Float, end: Float, animTime: Long) {
        animator.cancel()
        animator.setFloatValues(fixProgress(start), fixProgress(end))
        animator.setDuration(animTime)
        animator.start()
    }

    /**
     * 修正并返回范围在[0,1]内的progress
     * 
     * @param progress 待修正的progress
     * @return 修正后范围在[0, 1]内的progress
     */
    private fun fixProgress(progress: Float): Float =
        if (progress < 0) 0f else if (progress > 1) 1f else progress

}