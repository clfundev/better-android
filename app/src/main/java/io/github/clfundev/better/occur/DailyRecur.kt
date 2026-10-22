package io.github.clfundev.better.occur

class DailyRecur(start: Long, interval: Int) : AbstractRecur(start, interval) {
    override fun getOccursDuring(millis1Inclusive: Long, millis2Exclusive: Long): List<Long> {
        val occurs = mutableListOf<Long>()
        val intervalMillis = interval * MILLIS_PER_DAY
        var offsetStart = start
        if (offsetStart < millis1Inclusive) {
            offsetStart += ((millis1Inclusive - 1 - offsetStart) / intervalMillis + 1) * intervalMillis
        }
        while (offsetStart < millis2Exclusive) {
            occurs.add(offsetStart)
            offsetStart += intervalMillis
        }
        return occurs
    }

    override fun isOccurDuring(millis1Inclusive: Long, millis2Exclusive: Long): Boolean {
        if (start >= millis2Exclusive) return false
        if (start >= millis1Inclusive) return true
        val intervalMillis = interval * MILLIS_PER_DAY
        val offsetStart =
            start + ((millis1Inclusive - 1 - start) / intervalMillis + 1) * intervalMillis
        return offsetStart < millis2Exclusive
    }

    override fun getNextOccur(millis: Long): Long {
        val intervalMillis = interval * MILLIS_PER_DAY
        var offsetStart = start
        // 注意这里是小于等于
        if (start <= millis) {
            // 注意这里millis不能-1
            offsetStart = start + ((millis - start) / intervalMillis + 1) * intervalMillis
        }
        return offsetStart
    }

    override fun getPreviousOccur(millis: Long): Long {
        val intervalMillis = interval * MILLIS_PER_DAY
        if (start < millis) {
            // 找到millis之前最近一次start
            return start + (millis - 1 - start) / intervalMillis * intervalMillis
        }
        // start在millis之后，故millis之前没有occur
        return -1L
    }

    companion object {
        private const val MILLIS_PER_DAY = 86400000L
    }
}