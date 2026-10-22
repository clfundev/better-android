package io.github.clfundev.better.data.network.model

/**
 * 分页请求数据类
 *
 * @param T 记录的数据类型
 */
data class Page<T>(
    /**
     * 当前页的记录
     */
    val records: List<T>,

    /**
     * 总记录数
     */
    val total: Int,

    /**
     * 每页记录数
     */
    val size: Int,

    /**
     * 当前页码
     */
    val current: Int,

    /**
     * 总页数
     */
    val pages: Int
)
