package io.github.clfundev.better.widget

import android.content.Context
import android.util.AttributeSet
import io.github.clfundev.better.R
import java.text.NumberFormat
import kotlin.math.abs
import kotlin.math.max

class IntegerWheelPicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = R.style.DefaultWheelPicker
) : WheelPicker<Int>(context, attrs, defStyleAttr, defStyleRes) {

    private var maxValue = 0
    private var minValue = 0
    private var digits = 0
    private var digitsEnabled = false
    private val numberFormat: NumberFormat

    init {
        context.obtainStyledAttributes(
            attrs, R.styleable.IntegerWheelPicker, defStyleAttr, defStyleRes
        ).use { a ->
            maxValue = a.getInt(R.styleable.IntegerWheelPicker_maxValue, 0)
            minValue = a.getInt(R.styleable.IntegerWheelPicker_minValue, 0)
            digits = a.getInt(R.styleable.IntegerWheelPicker_digits, 1)
            digitsEnabled = a.getBoolean(R.styleable.IntegerWheelPicker_digitsEnabled, false)
        }
        check(minValue <= maxValue) { "minValue must be <= maxValue" }
        numberFormat = NumberFormat.getNumberInstance().apply {
            minimumIntegerDigits = digits
            maximumIntegerDigits = digits
            isGroupingUsed = false // 禁用逗号分组
        }
        if (digitsEnabled) {
            setItemFormatter { numberFormat.format(it) }
        } else {
            setItemFormatter(null)
        }
        updateMaxWidthText()
    }

    override fun getItemCount(): Int {
        val count = maxValue - minValue + 1
        return if (count > 0) count else 0
    }

    override fun getItem(index: Int): Int {
        if (index < 0 || index >= getItemCount()) {
            throw IndexOutOfBoundsException("Index out of bounds: index = $index, size = ${getItemCount()}")
        }
        return minValue + index
    }

    override fun indexOf(item: Int): Int {
        if (item in minValue..maxValue) {
            return item - minValue
        }
        return -1
    }

    fun setDigits(digits: Int) {
        if (digits != this.digits && digits > 0) {
            this.digits = digits
            numberFormat.setMinimumIntegerDigits(digits)
            numberFormat.setMaximumIntegerDigits(digits)
            if (digitsEnabled) {
                updateMaxWidthText()
            }
        }
    }

    fun setDigitsEnabled(digitsEnabled: Boolean) {
        if (this.digitsEnabled != digitsEnabled) {
            this.digitsEnabled = digitsEnabled
            updateMaxWidthText()
            if (digitsEnabled) {
                setItemFormatter { numberFormat.format(it) }
            } else {
                setItemFormatter(null)
            }
        }
    }

    fun setMaxValue(maxValue: Int) {
        if (this.maxValue != maxValue) {
            this.maxValue = maxValue
            if (minValue > maxValue) minValue = maxValue
            computeFlingLimitY()
            requestLayout()
            invalidate()
        }
    }

    fun setMinValue(minValue: Int) {
        if (this.minValue != minValue) {
            this.minValue = minValue
            if (minValue > maxValue) maxValue = minValue
            computeFlingLimitY()
            requestLayout()
            invalidate()
        }
    }

    private fun getIntDigits(num: Int): Int {
        var n = num
        if (n == 0) return 1
        n = abs(n)
        var digits = 0
        while (n != 0) {
            n /= 10
            digits++
        }
        return digits
    }

    private fun updateMaxWidthText() {
        // TODO: 既然最宽的文本一定是最大值或最小值，测出两者的文本宽度然后比较即可
        val digitCount = if (digitsEnabled) {
            // 依据指定位数计算宽度（负数的位数加1是考虑到负号的占位）
            if (minValue < 0) digits + 1 else digits
        } else {
            // 最宽的文本一定是最大值或最小值，取两者中位数更大者
            max(
                getIntDigits(maxValue),
                if (minValue < 0) getIntDigits(minValue) + 1 else getIntDigits(minValue)
            )
        }
        setMaxWidthText("0".repeat(digitCount))
    }
}
