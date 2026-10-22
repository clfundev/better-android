package io.github.clfundev.better.widget

import android.content.Context
import android.util.AttributeSet
import io.github.clfundev.better.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class DateWheelPicker(context: Context, attrs: AttributeSet?) :
    WheelPicker<LocalDate>(context, attrs) {

    private var startDate: LocalDate // 起始日期(包含)
    private var endDate: LocalDate // 结束日期(不包含)

    init {
        // 设置日期的显示格式
        val pattern = context.getString(R.string.all_month_day_format)
        val dateFormatter = DateTimeFormatter.ofPattern(pattern)
        setItemFormatter { dateFormatter.format(it) }
        // 默认的日期选择范围为前后50年
        val currentYear = LocalDate.now().year
        startDate = LocalDate.of(currentYear - 50, 1, 1)
        endDate = LocalDate.of(currentYear + 50, 1, 1)
        // 设置最宽选项文本
        setMaxWidthText("WWW 00")
        // 默认选中今天
        selectItem(LocalDate.now(), false)
    }

    override fun getItemCount(): Int {
        val daysBetween = ChronoUnit.DAYS.between(startDate, endDate).toInt()
        return if (daysBetween > 0) daysBetween else 0
    }

    override fun getItem(index: Int): LocalDate {
        if (index < 0 || index >= getItemCount()) {
            throw IndexOutOfBoundsException("Index out of bounds: index = $index, size = ${getItemCount()}")
        }
        return startDate.plusDays(index.toLong())
    }

    override fun indexOf(item: LocalDate): Int {
        if (item == startDate || (item.isAfter(startDate) && item.isBefore(endDate))) {
            return ChronoUnit.DAYS.between(startDate, item).toInt()
        }
        return -1
    }

    fun getStartDate(): LocalDate {
        return startDate
    }

    fun setStartDate(startDate: LocalDate) {
        this.startDate = startDate
        computeFlingLimitY()
        requestLayout()
        invalidate()
    }

    fun getEndDate(): LocalDate {
        return endDate
    }

    fun setEndDate(endDate: LocalDate) {
        this.endDate = endDate
        computeFlingLimitY()
        requestLayout()
        invalidate()
    }
}