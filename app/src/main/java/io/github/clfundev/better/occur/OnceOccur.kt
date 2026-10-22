package io.github.clfundev.better.occur

class OnceOccur(start: Long) : AbstractOccur(start) {
    override fun getOccursDuring(millis1Inclusive: Long, millis2Exclusive: Long): List<Long> {
        val occurs = mutableListOf<Long>()
        if (isOccurDuring(millis1Inclusive, millis2Exclusive)) {
            occurs.add(start)
        }
        return occurs
    }

    override fun isOccurDuring(millis1Inclusive: Long, millis2Exclusive: Long): Boolean =
        start in millis1Inclusive..<millis2Exclusive

    override fun getNextOccur(millis: Long): Long = if (start > millis) start else -1L

    override fun getPreviousOccur(millis: Long): Long = if (start < millis) start else -1L
}
