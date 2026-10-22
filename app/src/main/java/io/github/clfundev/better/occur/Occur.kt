package io.github.clfundev.better.occur

interface Occur {
    /**
     * 获取millis1Inclusive和millis2Exclusive之间的所有发生时间戳
     * 
     * @param millis1Inclusive 区间开始(包括)
     * @param millis2Exclusive 区间结束(不包括)
     * @return millis1Inclusive和millis2Exclusive之间的所有发生时间
     */
    fun getOccursDuring(millis1Inclusive: Long, millis2Exclusive: Long): List<Long>

    /**
     * 判断millis1Inclusive和millis2Exclusive之间是否发生
     * 
     * @param millis1Inclusive 区间开始(包括)
     * @param millis2Exclusive 区间结束(不包括)
     * @return millis1Inclusive和millis2Exclusive之间是否发生
     */
    fun isOccurDuring(millis1Inclusive: Long, millis2Exclusive: Long): Boolean

    /**
     * 获取指定时间之后(满足start>millis)
     * 最近一次发生时间戳，没有则返回-1
     * 
     * @param millis 指定时间
     * @return 指定时间之后最近一次发生时间戳，没有则返回-1
     */
    fun getNextOccur(millis: Long): Long

    /**
     * 获取指定时间之前(满足start<millis)
     * 最近一次发生时间戳，没有则返回-1
     * 
     * @param millis 指定时间
     * @return 指定时间之前最近一次发生时间戳，没有则返回-1
     */
    fun getPreviousOccur(millis: Long): Long
}