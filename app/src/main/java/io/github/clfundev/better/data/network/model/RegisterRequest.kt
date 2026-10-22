package io.github.clfundev.better.data.network.model

/**
 * 注册请求
 */
data class RegisterRequest(
    val username: String,
    val password: String,
    val nickname: String,
)