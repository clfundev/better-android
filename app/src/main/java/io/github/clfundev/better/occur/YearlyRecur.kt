package io.github.clfundev.better.occur

import java.time.temporal.ChronoUnit

class YearlyRecur(start: Long, interval: Int) : AbstractRecur(start, interval) {

    override fun getOccursDuring(millis1Inclusive: Long, millis2Exclusive: Long): List<Long> {
        val occurs = mutableListOf<Long>()
        // 时间戳转LocalDateTime对象，方便计算
        val dtstart = start.toLocalDateTime()
        val dtmillis1 = millis1Inclusive.toLocalDateTime()
        val dtmillis2 = millis2Exclusive.toLocalDateTime()
        // 不能通过后移dtstart来计算，会产生日期前移(2.29前移到2.28)
        var offsetDtstart = dtstart
        var yearsToPlus = 0L
        // 后移start至start>=millis1开始
        if (offsetDtstart.isBefore(dtmillis1)) {
            val dtstart_ = offsetDtstart.withMonth(1)
            val dtmillis1_ = dtmillis1.withMonth(2)
            val yearsBetween = ChronoUnit.YEARS.between(dtstart_, dtmillis1_)
            // 如果start年份在millis1年份之前，使start
            // 移动到与millis1同年或在millis1年份之后
            if (yearsBetween > 0) {
                yearsToPlus += ((yearsBetween - 1) / interval + 1) * interval
                offsetDtstart = dtstart.plusYears(yearsToPlus)
            }
            // 如果start与millis1同年，仍可能start时间
            // 在millis1之前，所以这里做个检查
            while (offsetDtstart.isBefore(dtmillis1)) {
                // 再加一个周期
                yearsToPlus += interval.toLong()
                offsetDtstart = dtstart.plusYears(yearsToPlus)
            }
        }
        // 到start>=millis2结束
        while (offsetDtstart.isBefore(dtmillis2)) {
            val ms = offsetDtstart.toMilli()
            occurs.add(ms)
            yearsToPlus += interval.toLong()
            offsetDtstart = dtstart.plusYears(yearsToPlus)
        }
        return occurs
    }

    override fun isOccurDuring(millis1Inclusive: Long, millis2Exclusive: Long): Boolean {
        // 逻辑与getOccursDuring类似
        val dtstart = start.toLocalDateTime()
        val dtmillis1 = millis1Inclusive.toLocalDateTime()
        val dtmillis2 = millis2Exclusive.toLocalDateTime()
        var offsetDtstart = dtstart
        var yearsToPlus = 0L
        if (offsetDtstart.isBefore(dtmillis1)) {
            val dtstart_ = offsetDtstart.withMonth(1)
            val dtmillis1_ = dtmillis1.withMonth(2)
            val yearsBetween = ChronoUnit.YEARS.between(dtstart_, dtmillis1_)
            if (yearsBetween > 0) {
                yearsToPlus += ((yearsBetween - 1) / interval + 1) * interval
                offsetDtstart = dtstart.plusYears(yearsToPlus)
            }
            while (offsetDtstart.isBefore(dtmillis1)) {
                yearsToPlus += interval.toLong()
                offsetDtstart = dtstart.plusYears(yearsToPlus)
            }
        }
        return offsetDtstart.isBefore(dtmillis2)
    }

    override fun getNextOccur(millis: Long): Long {
        // 逻辑与getOccursDuring类似，但要注意
        // 返回的是第一个start>millis，不是大于等于
        val dtstart = start.toLocalDateTime()
        val dtmillis = millis.toLocalDateTime()
        var offsetDtstart = dtstart
        var yearsToPlus = 0L
        if (offsetDtstart.isBefore(dtmillis)) {
            val dtstart_ = offsetDtstart.withMonth(1)
            val dtmillis_ = dtmillis.withMonth(2)
            val yearsBetween = ChronoUnit.YEARS.between(dtstart_, dtmillis_)
            if (yearsBetween > 0) {
                yearsToPlus += ((yearsBetween - 1) / interval + 1) * interval
                offsetDtstart = dtstart.plusYears(yearsToPlus)
            }
        }
        // 检查，确保start>millis
        while (!offsetDtstart.isAfter(dtmillis)) {
            // 再加一个周期
            yearsToPlus += interval.toLong()
            offsetDtstart = dtstart.plusYears(yearsToPlus)
        }
        return offsetDtstart.toMilli()
    }

    override fun getPreviousOccur(millis: Long): Long {
        val dtstart = start.toLocalDateTime()
        val dtmillis = millis.toLocalDateTime()
        var offsetDtstart = dtstart
        var yearsToPlus = 0L
        // start在millis之前，查找millis之前最近的一次occur
        if (offsetDtstart.isBefore(dtmillis)) {
            val dtstart_ = offsetDtstart.withMonth(1)
            val dtmillis_ = dtmillis.withMonth(2)
            val yearsBetween = ChronoUnit.YEARS.between(dtstart_, dtmillis_)
            if (yearsBetween > 0) {
                // 将start后移到与millis同年或其年份之后
                yearsToPlus += ((yearsBetween - 1) / interval + 1) * interval
                offsetDtstart = dtstart.plusYears(yearsToPlus)
            }
            // start前移直至在millis之前
            while (!offsetDtstart.isBefore(dtmillis)) {
                yearsToPlus -= interval.toLong()
                // 保证start不会前移到第一次start前
                if (yearsToPlus < 0) return -1L
                offsetDtstart = dtstart.plusYears(yearsToPlus)
            }
            return offsetDtstart.toMilli()
        }
        // start在millis之后，所以millis之前没有occur
        return -1L
    }
}
