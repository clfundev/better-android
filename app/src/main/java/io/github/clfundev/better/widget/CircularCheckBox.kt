package io.github.clfundev.better.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.text.TextPaint
import android.util.AttributeSet
import android.view.SoundEffectConstants
import android.view.View
import io.github.clfundev.better.R
import io.github.clfundev.better.utils.dp2px
import kotlin.math.floor
import kotlin.math.min

class CircularCheckBox @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = R.style.DefaultCircularCheckBox
) : View(context, attrs, defStyleAttr, defStyleRes) {

    private var antiAlias = true
    private var text = ""
    private var textSize = -1
    private var arcWidth = 1
    private var arcColor = Color.BLACK
    private var checkedFillColor = Color.BLACK
    private var checkedTextColor = Color.BLACK
    private var uncheckedTextColor = Color.BLACK
    private var textAllCaps = true

    private val centerPointF = PointF() // 圆心坐标
    private var radius = 0f // 圆弧半径
    private var textY = 0f // 文字绘制时的y坐标
    private val textPaint = TextPaint()
    private val arcPaint = Paint()
    private val solidCirclePaint = Paint()

    var checked = false
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    init {
        context.obtainStyledAttributes(
            attrs,
            R.styleable.CircularCheckBox,
            defStyleAttr,
            defStyleRes
        ).use { typedArray ->
            antiAlias = typedArray.getBoolean(
                R.styleable.CircularCheckBox_antiAlias, antiAlias
            )
            text = typedArray.getString(
                R.styleable.CircularCheckBox_text
            ) ?: text
            textSize = typedArray.getDimensionPixelSize(
                R.styleable.CircularCheckBox_textSize, textSize
            )
            arcWidth = typedArray.getDimensionPixelSize(
                R.styleable.CircularCheckBox_arcWidth, arcWidth
            )
            arcColor = typedArray.getColor(
                R.styleable.CircularCheckBox_arcColor, arcColor
            )
            checkedFillColor = typedArray.getColor(
                R.styleable.CircularCheckBox_checkedFillColor, checkedFillColor
            )
            checkedTextColor = typedArray.getColor(
                R.styleable.CircularCheckBox_checkedTextColor, checkedTextColor
            )
            uncheckedTextColor = typedArray.getColor(
                R.styleable.CircularCheckBox_uncheckedTextColor, uncheckedTextColor
            )
            textAllCaps = typedArray.getBoolean(
                R.styleable.CircularCheckBox_textAllCaps, textAllCaps
            )
        }
        // 初始化各个Paint对象
        textPaint.isAntiAlias = antiAlias
        textPaint.textSize = textSize.toFloat()
        textPaint.textAlign = Paint.Align.CENTER
        arcPaint.isAntiAlias = antiAlias
        arcPaint.color = arcColor
        arcPaint.strokeWidth = arcWidth.toFloat()
        arcPaint.style = Paint.Style.STROKE
        solidCirclePaint.isAntiAlias = antiAlias
        solidCirclePaint.color = checkedFillColor
        solidCirclePaint.style = Paint.Style.FILL
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val defaultSizeInPx = dp2px(context, DEFAULT_SIZE_IN_DP)
        setMeasuredDimension(
            resolveSize(defaultSizeInPx, widthMeasureSpec),
            resolveSize(defaultSizeInPx, heightMeasureSpec)
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 计算圆心
        centerPointF.x = floor(w / 2f)
        centerPointF.y = floor(h / 2f)
        // 求更小值作为半径（因为圆应当贴合最窄的边界）
        radius = min(
            w - paddingLeft - paddingRight - arcWidth,
            h - paddingTop - paddingBottom - arcWidth
        ) / 2f
        // 计算文字绘制时的y坐标
        textY = centerPointF.y - (textPaint.ascent() + textPaint.descent()) / 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        with(canvas) {
            // 绘制背景圆或圆环
            drawCircle(
                centerPointF.x,
                centerPointF.y,
                radius,
                if (checked) solidCirclePaint else arcPaint
            )
            // 绘制文字(由于Paint已设为居中绘制，无需把文字的x绘制坐标设为centerPoint.x - textWidth / 2)
            val textToDraw = if (textAllCaps) text.uppercase() else text
            textPaint.setColor(if (checked) checkedTextColor else uncheckedTextColor)
            drawText(textToDraw, centerPointF.x, textY, textPaint)
        }
    }

    fun toggle() {
        checked = !checked
    }

    override fun performClick(): Boolean {
        // 参考android.widget.CompoundButton源码
        toggle()
        val handled = super.performClick()
        if (!handled) {
            // View only makes a sound effect if the onClickListener was
            // called, so we'll need to make one here instead.
            playSoundEffect(SoundEffectConstants.CLICK)
        }
        return handled
    }

    companion object {
        // 默认尺寸，当指定wrap_content的时候就会使用这个尺寸
        private const val DEFAULT_SIZE_IN_DP = 21f
    }
}