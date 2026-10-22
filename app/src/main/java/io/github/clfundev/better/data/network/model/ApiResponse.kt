package io.github.clfundev.better.data.network.model

/**
 * 统一响应类
 */
data class ApiResponse<T>(
    var code: Int? = null,
    var message: String? = null,
    var data: T? = null
) {
    fun isSuccessful(): Boolean = code == 200
}