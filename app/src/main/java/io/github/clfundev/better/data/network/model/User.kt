package io.github.clfundev.better.data.network.model

/**
 * 用户
 */
data class User(
    val userId: Int,
    val username: String,
    val password: String,
    val nickname: String,
    val profile: String,
    val avatarId: String
)