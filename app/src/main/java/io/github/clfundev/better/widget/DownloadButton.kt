package io.github.clfundev.better.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import androidx.annotation.IntDef
import androidx.appcompat.widget.AppCompatTextView
import io.github.clfundev.better.R

/**
 * 带进度的下载按钮
 */
@SuppressLint("SetTextI18n")
class DownloadButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {

    @State
    private var state = IDLE
    private var progress = 0f // 进度条进度，范围[0,1]
    private val textOnIdle: String
    private val textOnFinished: String
    private val roundCorner: Int
    private val borderWidth: Int
    private val primaryColor: Int // 边框颜色、完成背景色
    private val progressColor: Int // 进度色
    private val unfinishedTextColor: Int // 未完成时的文字颜色
    private val finishedTextColor: Int // 完成时的文字颜色
    private val progressPaint: Paint
    private val borderPaint: Paint
    private val bgOnFinishedPaint: Paint
    private val drawRectF = RectF()
    private val borderPath = Path()

    init {
        context.obtainStyledAttributes(
            attrs, R.styleable.DownloadButton, defStyleAttr, 0
        ).use { a ->
            textOnIdle = a.getString(
                R.styleable.DownloadButton_textOnIdle
            ) ?: ""
            textOnFinished = a.getString(
                R.styleable.DownloadButton_textOnFinished
            ) ?: ""
            roundCorner = a.getDimensionPixelSize(
                R.styleable.DownloadButton_roundCorner, 0
            )
            borderWidth = a.getDimensionPixelSize(
                R.styleable.DownloadButton_borderWidth, 0
            )
            primaryColor = a.getColor(
                R.styleable.DownloadButton_primaryColor, Color.BLACK
            )
            progressColor = a.getColor(
                R.styleable.DownloadButton_progressColor, Color.BLACK
            )
            unfinishedTextColor = a.getColor(
                R.styleable.DownloadButton_unfinishedTextColor, Color.BLACK
            )
            finishedTextColor = a.getColor(
                R.styleable.DownloadButton_finishedTextColor, Color.BLACK
            )
        }
        progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            setColor(progressColor)
        }
        borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = borderWidth.toFloat()
            setColor(primaryColor)
        }
        bgOnFinishedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            setColor(primaryColor)
        }
        updateTextByState()
        updateTextColorByState()
    }

    fun getProgress(): Float {
        return progress
    }

    fun setProgress(progress: Float) {
        this.progress = progress
        setState(ONGOING)
        setText("${(progress * 100f).toInt()} %")
        invalidate()
    }

    @State
    fun getState(): Int {
        return state
    }

    fun setState(@State state: Int) {
        if (this.state == state) return
        this.state = state
        updateTextByState()
        updateTextColorByState()
        invalidate()
    }

    private fun updateTextByState() {
        when (state) {
            IDLE -> setText(textOnIdle)
            ONGOING -> setText("${(progress * 100f).toInt()} %")
            FINISHED -> setText(textOnFinished)
        }
    }

    private fun updateTextColorByState() {
        setTextColor(if (state == FINISHED) finishedTextColor else unfinishedTextColor)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val halfStrokeWidth = borderWidth / 2 + 1
        drawRectF.set(
            halfStrokeWidth.toFloat(),
            halfStrokeWidth.toFloat(),
            (w - halfStrokeWidth).toFloat(),
            (h - halfStrokeWidth).toFloat()
        )
        borderPath.reset()
        borderPath.addRoundRect(
            drawRectF,
            roundCorner.toFloat(),
            roundCorner.toFloat(),
            Path.Direction.CW
        )
    }

    override fun onDraw(canvas: Canvas) {
        // 绘制边框
        canvas.drawPath(borderPath, if (state == FINISHED) bgOnFinishedPaint else borderPaint)
        canvas.clipPath(borderPath) // 限制之后的绘制在边框内
        // 绘制进度
        if (state == ONGOING) {
            canvas.drawRect(
                drawRectF.left,
                drawRectF.top,
                drawRectF.left + drawRectF.width() * progress,
                drawRectF.bottom,
                progressPaint
            )
        }
        // 文字的绘制交给父类
        super.onDraw(canvas)
    }

    companion object {
        const val IDLE = 0
        const val ONGOING = 1
        const val FINISHED = 2
    }

    @IntDef(IDLE, ONGOING, FINISHED)
    @Retention(AnnotationRetention.SOURCE)
    annotation class State
}
