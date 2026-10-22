package io.github.clfundev.better.occur

import java.time.temporal.ChronoUnit

class MonthlyRecur(start: Long, interval: Int) : AbstractRecur(start, interval) {
    override fun getOccursDuring(millis1Inclusive: Long, millis2Exclusive: Long): List<Long> {
        val occurs = mutableListOf<Long>()
        val dtstart = start.toLocalDateTime()
        val dtmillis1 = millis1Inclusive.toLocalDateTime()
        val dtmillis2 = millis2Exclusive.toLocalDateTime()
        var offsetDtstart = dtstart
        var monthsToPlus = 0L
        if (offsetDtstart.isBefore(dtmillis1)) {
            val dtstart_ = offsetDtstart.withDayOfMonth(1)
            val dtmillis1_ = dtmillis1.withDayOfMonth(2)
            val monthsBetween = ChronoUnit.MONTHS.between(dtstart_, dtmillis1_)
            // 如果start月份在millis1月份之前，使start
            // 移动到与millis1同月或在millis1月份之后
            if (monthsBetween > 0) {
                monthsToPlus += ((monthsBetween - 1) / interval + 1) * interval
                offsetDtstart = dtstart.plusMonths(monthsToPlus)
            }
            // 如果start与millis1同月，仍可能start时间
            // 在millis1之前，所以这里做个检查
            while (offsetDtstart.isBefore(dtmillis1)) {
                // 再加一个周期
                monthsToPlus += interval
                offsetDtstart = dtstart.plusMonths(monthsToPlus)
            }
        }
        // 到start>=millis2结束
        while (offsetDtstart.isBefore(dtmillis2)) {
            val ms = offsetDtstart.toMilli()
            occurs.add(ms)
            monthsToPlus += interval
            offsetDtstart = dtstart.plusMonths(monthsToPlus)
        }
        return occurs
    }

    override fun isOccurDuring(millis1Inclusive: Long, millis2Exclusive: Long): Boolean {
        // 逻辑与getOccursDuring类似
        val dtstart = start.toLocalDateTime()
        val dtmillis1 = millis1Inclusive.toLocalDateTime()
        val dtmillis2 = millis2Exclusive.toLocalDateTime()
        var offsetDtstart = dtstart
        var monthsToPlus = 0L
        if (offsetDtstart.isBefore(dtmillis1)) {
            val dtstart_ = offsetDtstart.withDayOfMonth(1)
            val dtmillis1_ = dtmillis1.withDayOfMonth(2)
            val monthsBetween = ChronoUnit.MONTHS.between(dtstart_, dtmillis1_)
            if (monthsBetween > 0) {
                monthsToPlus += ((monthsBetween - 1) / interval + 1) * interval
                offsetDtstart = dtstart.plusMonths(monthsToPlus)
            }
            while (offsetDtstart.isBefore(dtmillis1)) {
                monthsToPlus += interval
                offsetDtstart = dtstart.plusMonths(monthsToPlus)
            }
        }
        return offsetDtstart.isBefore(dtmillis2)
    }

    override fun getNextOccur(millis: Long): Long {
        // 逻辑与getOccursDuring类似，但要注意返回的是第一个start>millis，不是大于等于
        val dtstart = start.toLocalDateTime()
        val dtmillis = millis.toLocalDateTime()
        var offsetDtstart = dtstart
        var monthsToPlus = 0L
        if (offsetDtstart.isBefore(dtmillis)) {
            val dtstart_ = offsetDtstart.withDayOfMonth(1)
            val dtmillis_ = dtmillis.withDayOfMonth(2)
            val monthsBetween = ChronoUnit.MONTHS.between(dtstart_, dtmillis_)
            if (monthsBetween > 0) {
                monthsToPlus += ((monthsBetween - 1) / interval + 1) * interval
                offsetDtstart = dtstart.plusMonths(monthsToPlus)
            }
        }
        // 检查，确保start>millis
        while (!offsetDtstart.isAfter(dtmillis)) {
            monthsToPlus += interval
            offsetDtstart = dtstart.plusMonths(monthsToPlus)
        }
        return offsetDtstart.toMilli()
    }

    override fun getPreviousOccur(millis: Long): Long {
        val dtstart = start.toLocalDateTime()
        val dtmillis = millis.toLocalDateTime()
        var offsetDtstart = dtstart
        var monthsToPlus = 0L
        // start在millis之前，查找millis之前最近的一次occur
        if (offsetDtstart.isBefore(dtmillis)) {
            val dtstart_ = offsetDtstart.withDayOfMonth(1)
            val dtmillis_ = dtmillis.withDayOfMonth(2)
            val monthsBetween = ChronoUnit.MONTHS.between(dtstart_, dtmillis_)
            if (monthsBetween > 0) {
                // 将start后移到与millis同月或其月份之后
                monthsToPlus += ((monthsBetween - 1) / interval + 1) * interval
                offsetDtstart = dtstart.plusMonths(monthsToPlus)
            }
            // start前移直至在millis之前
            while (!offsetDtstart.isBefore(dtmillis)) {
                monthsToPlus -= interval
                // 保证start不会前移到第一次start前
                if (monthsToPlus < 0) return -1
                offsetDtstart = dtstart.plusMonths(monthsToPlus)
            }
            return offsetDtstart.toMilli()
        }
        // start在millis之后，所以millis之前没有occur
        return -1L
    }
}