package io.github.clfundev.better.occur

import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

class WeeklyRecur(start: Long, interval: Int, val byDays: List<Int>) :
    AbstractRecur(start, interval) {

    override fun getOccursDuring(millis1Inclusive: Long, millis2Exclusive: Long): List<Long> {
        val occurs = mutableListOf<Long>()
        val intervalMillis = interval * WEEK_MILLIS
        var recurIndex = 0
        var starts = getOrderedStarts(recurIndex)
        // 第一周可能没有计划发生
        if (starts.isEmpty()) {
            // 获取第二周的starts
            starts = getOrderedStarts(++recurIndex)
        }
        // 从一周中最后一个start>=begin开始
        val lastStart = starts.last()
        if (lastStart < millis1Inclusive) {
            recurIndex += ((millis1Inclusive - 1 - lastStart) / intervalMillis + 1).toInt()
            starts = getOrderedStarts(recurIndex)
        }
        // 到一周中第一个start>=end结束
        while (starts.first() < millis2Exclusive) {
            for (s in starts) {
                if (s < millis1Inclusive) continue
                if (s >= millis2Exclusive) break
                occurs.add(s)
            }
            starts = getOrderedStarts(++recurIndex)
        }
        return occurs
    }

    override fun isOccurDuring(millis1Inclusive: Long, millis2Exclusive: Long): Boolean {
        val intervalMillis = interval * WEEK_MILLIS
        var recurIndex = 0
        var starts = getOrderedStarts(recurIndex)
        // 第一周可能没有计划发生
        if (starts.isEmpty()) {
            // 获取第二周的starts
            starts = getOrderedStarts(++recurIndex)
        }
        // 从一周中最后一个start>=begin开始
        val lastStart = starts.last()
        if (lastStart < millis1Inclusive) {
            recurIndex += ((millis1Inclusive - 1 - lastStart) / intervalMillis + 1).toInt()
            starts = getOrderedStarts(recurIndex)
        }
        // 到一周中第一个start>=end结束
        return starts.first() < millis2Exclusive
    }

    override fun getNextOccur(millis: Long): Long {
        val intervalMillis = interval * WEEK_MILLIS
        var recurIndex = 0
        var starts = getOrderedStarts(recurIndex)
        // 第一周可能没有计划发生
        if (starts.isEmpty()) {
            // 获取第二周的starts
            starts = getOrderedStarts(++recurIndex)
        }
        // 从一周中最后一个start>=begin开始
        val lastStart = starts.last()
        // 注意这里是小于等于
        if (lastStart <= millis) {
            // 注意这里millis不能-1
            recurIndex += ((millis - lastStart) / intervalMillis + 1).toInt()
            starts = getOrderedStarts(recurIndex)
        }
        for (s in starts) {
            if (s > millis) return s
        }
        return -1L
    }

    override fun getPreviousOccur(millis: Long): Long {
        val intervalMillis = interval * WEEK_MILLIS
        var recurIndex = 0
        var starts = getOrderedStarts(recurIndex)
        // 第一周可能没有计划发生
        if (starts.isEmpty()) {
            // 获取第二周的starts
            starts = getOrderedStarts(++recurIndex)
        }
        val lastStart = starts.last()
        if (lastStart < millis) {
            // 找到在millis之前的最近一次lastStart
            recurIndex += ((millis - 1 - lastStart) / intervalMillis).toInt()
            starts = getOrderedStarts(recurIndex)
        }
        // 遍历当前lastStart对应那一周所有start，找到millis之前最近的那次start
        for (s in starts.reversed()) {
            if (s < millis) return s
        }
        return -1L
    }

    /**
     * 获取第recurIndex+1次计划发生周内计划所有发生时间，
     * 发生时间从先到后排序。其中第一周的计划所有发生时间
     * 只包括与byDays对应且在start之后的发生时间，而不是
     * 全部byDays都有对应
     * 
     * @param recurIndex 计划重复周次
     * @return 指定周次计划所有发生时间(ms)
     */
    private fun getOrderedStarts(recurIndex: Int): List<Long> {
        val intervalMillis = interval * WEEK_MILLIS
        val starts = mutableListOf<Long>()
        val dtstart = (start + intervalMillis * recurIndex).toLocalDateTime()
        if (recurIndex > 0) {
            for (day in byDays) {
                val adjustedDtstart = if (day < dtstart.dayOfWeek.value) {
                    dtstart.with(TemporalAdjusters.previous(DayOfWeek.of(day)))
                } else {
                    dtstart.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(day)))
                }
                starts.add(adjustedDtstart.toMilli())
            }
        } else if (recurIndex == 0) {
            // 第一周不包括start之前的发生时间
            for (day in byDays) {
                if (day >= dtstart.dayOfWeek.value) {
                    val adjustedDtstart =
                        dtstart.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(day)))
                    starts.add(adjustedDtstart.toMilli())
                }
            }
        }
        // 对starts按时间先后排序
        starts.sort()
        return starts
    }

    companion object {
        private const val WEEK_MILLIS = 604800000L
    }
}
