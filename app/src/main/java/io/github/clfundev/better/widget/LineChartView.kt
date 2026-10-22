package io.github.clfundev.better.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.toPointF
import io.github.clfundev.better.R
import io.github.clfundev.better.utils.getBaselineForCenteredOn
import io.github.clfundev.better.utils.getLineHeight
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class LineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = R.style.DefaultLineChartView
) : View(context, attrs, defStyleAttr, defStyleRes) {

    // 数据集
    private val dateToValue = mutableMapOf<LocalDate, Long>()

    // 辅助绘制
    private val drawRect = Rect() // 整体绘制区域
    private val tableRect = Rect() // 图表绘制区域
    private val points = mutableListOf<Point>() // 折线数据点集合
    private var bgNumX = 0
    private val bgNumYs = mutableListOf<Int>()
    private val bgNumTexts = mutableListOf<String>()
    private val bgLineYs = mutableListOf<Int>()
    private var bgDateY = 0

    // 折线
    private val linePaint: Paint
    private val lineWidth: Int
    private val lineColor: Int

    // y轴数值指示线
    private val bgLinePaint: Paint
    private val bgLineWidth: Int
    private val bgLineColor: Int
    private val bgLineCount: Int // 指示线条数
    private val bgLineNumGap: Int // 指示线与其左边指示数字间距
    private val bgLineDateGap: Int // 指示线与底部日期间距

    // y轴数值
    private val bgNumTextPaint: TextPaint
    private val bgNumTextSize: Int
    private val bgNumTextColor: Int

    // x轴日期
    private val dates = mutableListOf<LocalDate>()
    private val bgDateTextPaint: TextPaint
    private val bgDateTextSize: Int
    private val bgDateTextColor: Int

    // 详情数据点指示线
    private val detailLinePaint: Paint
    private val detailLineWidth: Int
    private val detailLineColor: Int

    // 详情数据点
    private val detailPointStrokePaint: Paint
    private val detailPointStrokeWidth: Int
    private val detailPointStrokeColor: Int
    private val detailPointFillPaint: Paint
    private val detailPointFillColor: Int
    private val detailPointRadius: Int // 详情数据点半径
    private var detailIndex = -1 // 详情数据点下标(-1表示未触摸、无详情数据点)

    // 详情浮窗
    private val detailRectF = RectF()
    private val detailRectPaint: Paint
    private val detailRectColor: Int
    private val detailRectShadowColor: Int
    private val detailRectShadowRadius: Int
    private val detailLineRectGap: Int // 详情浮窗和竖直指示线间距
    private val detailRectPadding: Int // 详情浮窗padding
    private val detailRectRoundCorner: Int // 详情浮窗圆角尺寸

    // 详情浮窗内文字
    private val detailTextPaint: TextPaint
    private val detailTextSize: Int
    private val detailTextColor: Int
    private val detailDateNumGap: Int // 详情日期与数值的行间距

    private var formatter: LineChartFormatter? = null // 文本格式
    private var stepCalculator: StepCalculator? = null // 图表数值指示线步长计算器

    init {
        context.obtainStyledAttributes(
            attrs, R.styleable.LineChartView, defStyleAttr, defStyleRes
        ).use { a ->
            lineWidth = a.getDimensionPixelSize(
                R.styleable.LineChartView_lineWidth, -1
            )
            lineColor = a.getColor(
                R.styleable.LineChartView_lineColor, Color.BLACK
            )
            bgLineWidth = a.getDimensionPixelSize(
                R.styleable.LineChartView_bgLineWidth, -1
            )
            bgLineColor = a.getColor(
                R.styleable.LineChartView_bgLineColor, Color.BLACK
            )
            bgNumTextSize = a.getDimensionPixelSize(
                R.styleable.LineChartView_bgNumTextSize, -1
            )
            bgNumTextColor = a.getColor(
                R.styleable.LineChartView_bgNumTextColor, Color.BLACK
            )
            bgDateTextSize = a.getDimensionPixelSize(
                R.styleable.LineChartView_bgDateTextSize, -1
            )
            bgDateTextColor = a.getColor(
                R.styleable.LineChartView_bgDateTextColor, Color.BLACK
            )
            detailLineWidth = a.getDimensionPixelSize(
                R.styleable.LineChartView_detailLineWidth, -1
            )
            detailLineColor = a.getColor(
                R.styleable.LineChartView_detailLineColor, Color.BLACK
            )
            detailPointStrokeWidth = a.getDimensionPixelSize(
                R.styleable.LineChartView_detailPointStrokeWidth, -1
            )
            detailPointStrokeColor = a.getColor(
                R.styleable.LineChartView_detailPointStrokeColor, Color.BLACK
            )
            detailPointFillColor = a.getColor(
                R.styleable.LineChartView_detailPointFillColor, Color.BLACK
            )
            detailTextSize = a.getDimensionPixelSize(
                R.styleable.LineChartView_detailTextSize, -1
            )
            detailTextColor = a.getColor(
                R.styleable.LineChartView_detailTextColor, Color.BLACK
            )
            detailRectColor = a.getColor(
                R.styleable.LineChartView_detailRectColor, Color.BLACK
            )
            detailRectShadowColor = a.getColor(
                R.styleable.LineChartView_detailRectShadowColor, Color.BLACK
            )
            detailRectShadowRadius = a.getDimensionPixelSize(
                R.styleable.LineChartView_detailRectShadowRadius, -1
            )
            bgLineCount = a.getInt(
                R.styleable.LineChartView_bgLineCount, 0
            )
            bgLineNumGap = a.getDimensionPixelSize(
                R.styleable.LineChartView_bgLineNumGap, -1
            )
            bgLineDateGap = a.getDimensionPixelSize(
                R.styleable.LineChartView_bgLineDateGap, -1
            )
            detailPointRadius = a.getDimensionPixelSize(
                R.styleable.LineChartView_detailPointRadius, -1
            )
            detailDateNumGap = a.getDimensionPixelSize(
                R.styleable.LineChartView_detailDateNumGap, -1
            )
            detailLineRectGap = a.getDimensionPixelSize(
                R.styleable.LineChartView_detailLineRectGap, -1
            )
            detailRectPadding = a.getDimensionPixelSize(
                R.styleable.LineChartView_detailRectPadding, -1
            )
            detailRectRoundCorner = a.getDimensionPixelSize(
                R.styleable.LineChartView_detailRectRoundCorner, -1
            )
        }

        linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = lineWidth.toFloat()
            setColor(lineColor)
            strokeCap = Paint.Cap.ROUND
        }
        bgLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = bgLineWidth.toFloat()
            setColor(bgLineColor)
        }
        bgNumTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = bgNumTextSize.toFloat()
            setColor(bgNumTextColor)
            textAlign = Paint.Align.RIGHT
        }
        bgDateTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = bgDateTextSize.toFloat()
            setColor(bgDateTextColor)
            textAlign = Paint.Align.CENTER
        }
        detailLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = detailLineWidth.toFloat()
            setColor(detailLineColor)
            strokeCap = Paint.Cap.ROUND
        }
        detailPointStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = detailPointStrokeWidth.toFloat()
            setColor(detailPointStrokeColor)
            style = Paint.Style.STROKE
        }
        detailPointFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            setColor(detailPointFillColor)
            style = Paint.Style.FILL
        }
        detailTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = detailTextSize.toFloat()
            setColor(detailTextColor)
        }
        detailRectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            setColor(detailRectColor)
            setShadowLayer(detailRectShadowRadius.toFloat(), 0f, 0f, detailRectShadowColor)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measuredWidth = resolveSize(paddingLeft + paddingRight, widthMeasureSpec)
        val measuredHeight = resolveSize(paddingTop + paddingBottom, heightMeasureSpec)
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 计算整个控件绘制的区域
        drawRect.set(paddingLeft, paddingTop, w - paddingRight, h - paddingBottom)
        // 绘制前计算
        prepareDraw()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!canDraw()) return

        // 从上到下绘制数值和指示线
        for (i in 0..<bgLineCount) {
            canvas.drawText(bgNumTexts[i], bgNumX.toFloat(), bgNumYs[i].toFloat(), bgNumTextPaint)
            canvas.drawLine(
                tableRect.left.toFloat(),
                bgLineYs[i].toFloat(),
                tableRect.right.toFloat(),
                bgLineYs[i].toFloat(),
                bgLinePaint
            )
        }

        // 绘制折线
        var p1 = points[0].toPointF()
        for (i in 1..<points.size) {
            val p2 = points[i].toPointF()
            canvas.drawLine(p1.x, p1.y, p2.x, p2.y, linePaint)
            p1 = p2
        }

        // 绘制头尾日期
        canvas.drawText(
            formatter?.formatBgDate(dates.first()) ?: dates.first().toString(),
            points.first().x.toFloat(),
            bgDateY.toFloat(),
            bgDateTextPaint
        )
        canvas.drawText(
            formatter?.formatBgDate(dates.last()) ?: dates.last().toString(),
            points.last().x.toFloat(),
            bgDateY.toFloat(),
            bgDateTextPaint
        )

        // 用户触摸时绘制数据点详情
        if (detailIndex != -1) {
            val p = points[detailIndex]
            canvas.drawLine(
                p.x.toFloat(),
                tableRect.top.toFloat(),
                p.x.toFloat(),
                tableRect.bottom.toFloat(),
                detailLinePaint
            )
            canvas.drawCircle(
                p.x.toFloat(),
                p.y.toFloat(),
                detailPointRadius.toFloat(),
                detailPointFillPaint
            )
            canvas.drawCircle(
                p.x.toFloat(),
                p.y.toFloat(),
                detailPointRadius.toFloat(),
                detailPointStrokePaint
            )
            // 绘制详情浮动窗口
            val detailDate = dates[detailIndex]
            val detailNum = dateToValue[detailDate] ?: 0
            val dateStr = formatter?.formatDetailDate(detailDate) ?: detailDate.toString()
            val numStr = formatter?.formatDetailNum(detailNum) ?: detailNum.toString()
            val fontMetrics = detailTextPaint.getFontMetrics()
            val dateW = detailTextPaint.measureText(dateStr).roundToInt() + 1
            val numW = detailTextPaint.measureText(numStr).roundToInt() + 1
            val numH = fontMetrics.getLineHeight().roundToInt()
            val rectH = numH * 2 + detailDateNumGap + detailRectPadding * 2
            val rectW = max(dateW, numW) + detailRectPadding * 2
            detailRectF.left = (p.x - detailLineRectGap - rectW).toFloat()
            detailRectF.top = p.y - rectH / 2f
            detailRectF.right = (p.x - detailLineRectGap).toFloat()
            detailRectF.bottom = detailRectF.top + rectH
            if (detailRectF.left < tableRect.left) {
                detailRectF.left = (p.x + detailLineRectGap).toFloat()
                detailRectF.right = (p.x + detailLineRectGap + rectW).toFloat()
            }
            if (detailRectF.top < tableRect.top) {
                detailRectF.top = tableRect.top.toFloat()
                detailRectF.bottom = (tableRect.top + rectH).toFloat()
            } else if (detailRectF.bottom > tableRect.bottom) {
                detailRectF.top = (tableRect.bottom - rectH).toFloat()
                detailRectF.bottom = tableRect.bottom.toFloat()
            }
            canvas.drawRoundRect(
                detailRectF,
                detailRectRoundCorner.toFloat(),
                detailRectRoundCorner.toFloat(),
                detailRectPaint
            )
            // 绘制详情窗口内文字
            val dateY =
                (detailRectF.centerY() - fontMetrics.bottom - detailDateNumGap / 2f).roundToInt()
            val numY =
                (detailRectF.centerY() - fontMetrics.top + detailDateNumGap / 2f).roundToInt()
            canvas.drawText(
                dateStr,
                detailRectF.left + detailRectPadding,
                dateY.toFloat(),
                detailTextPaint
            )
            canvas.drawText(
                numStr,
                detailRectF.left + detailRectPadding,
                numY.toFloat(),
                detailTextPaint
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touchIndex = when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE ->
                calcTouchIndex(event.x.roundToInt())

            else -> -1
        }
        if (detailIndex != touchIndex) {
            detailIndex = touchIndex
            invalidate()
        }
        return true
    }

    private fun prepareDraw() {
        if (!canDraw()) return
        val dateTextFontMetrics = bgDateTextPaint.getFontMetrics()
        val dateTextHeight = dateTextFontMetrics.getLineHeight().roundToInt()
        val dateTextWidth = bgDateTextPaint.measureText("88/88").roundToInt() + 1
        val numFontMetrics = bgNumTextPaint.getFontMetrics()
        val numTextHeight = numFontMetrics.getLineHeight().roundToInt()
        var numTextWidth = 0 // 在下面计算数值文字同时计算
        // 计算数值文本
        val maxNum = dateToValue.values.max()
        val step = stepCalculator?.calcStep(maxNum, bgLineCount) ?: 1
        bgNumTexts.clear()
        for (i in 0..<bgLineCount) {
            val num = step * (bgLineCount - 1 - i)
            val numText = formatter?.formatBgNum(num) ?: num.toString()
            bgNumTexts.add(numText)
            numTextWidth = max(bgNumTextPaint.measureText(numText).roundToInt() + 1, numTextWidth)
        }
        // 计算表格绘制范围
        tableRect.set(
            drawRect.left + numTextWidth + bgLineNumGap,
            drawRect.top + numTextHeight / 2,
            drawRect.right - dateTextWidth / 2,
            drawRect.bottom - max(numTextHeight / 2, dateTextHeight + bgLineDateGap)
        )
        val verticalGap = tableRect.height() / (bgLineCount - 1)
        val horizontalGap = tableRect.width() / (dateToValue.size - 1)
        tableRect.bottom = tableRect.top + verticalGap * (bgLineCount - 1)
        tableRect.right = tableRect.left + horizontalGap * (dateToValue.size - 1)
        // 计算数值x坐标
        bgNumX = tableRect.left - bgLineNumGap
        // 计算日期y坐标
        bgDateY = tableRect.bottom + bgLineDateGap - dateTextFontMetrics.top.roundToInt()
        // 从上至下计算各个数值和指示线的y坐标
        bgLineYs.clear()
        bgNumYs.clear()
        for (i in 0..<bgLineCount) {
            bgLineYs.add(tableRect.top + verticalGap * i)
            bgNumYs.add(numFontMetrics.getBaselineForCenteredOn(bgLineYs[i].toFloat()).toInt())
        }
        // 从左到右计算各个数据点
        points.clear()
        val maxLineNum = step * (bgLineCount - 1)
        for (i in 0..<dateToValue.size) {
            var pointNum = dateToValue[dates[i]] ?: 0
            pointNum = min(pointNum, maxLineNum)
            val hRatio = pointNum.toDouble() / maxLineNum
            points.add(
                Point(
                    tableRect.left + horizontalGap * i,
                    (tableRect.bottom - hRatio * tableRect.height()).toInt()
                )
            )
        }
    }

    private fun canDraw(): Boolean = dateToValue.size >= 2 && bgLineCount >= 2

    private fun calcTouchIndex(touchX: Int): Int {
        if (!canDraw()) return -1
        val horizontalGap = tableRect.width() / (dateToValue.size - 1)
        var xMin = points[0].x - horizontalGap / 2
        for (i in points.indices) {
            val xMax = xMin + horizontalGap
            if (touchX in xMin..<xMax) {
                return i
            }
            xMin = xMax
        }
        return -1
    }

    fun setLineChartFormatter(formatter: LineChartFormatter?) {
        this.formatter = formatter
        prepareDraw()
        invalidate()
    }

    fun setData(map: Map<LocalDate, Long>) {
        dateToValue.clear()
        dateToValue.putAll(map)
        dates.clear()
        dates.addAll(map.keys)
        dates.sort() // 日期升序排列
        prepareDraw()
        invalidate()
    }

    fun setStepCalculator(stepCalculator: StepCalculator?) {
        this.stepCalculator = stepCalculator
        prepareDraw()
        invalidate()
    }

    interface LineChartFormatter {
        fun formatBgNum(num: Long): String

        fun formatDetailNum(num: Long): String

        fun formatBgDate(date: LocalDate): String

        fun formatDetailDate(date: LocalDate): String
    }

    fun interface StepCalculator {
        fun calcStep(maxNum: Long, lineCount: Int): Long
    }
}