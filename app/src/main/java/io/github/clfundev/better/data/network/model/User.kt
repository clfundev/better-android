package io.github.clfundev.better.data.network.model

import java.time.Instant

/**
 * 用户
 */
data class User(
    val id: Long,
    val username: String,
    val nickname: String,
    val profile: String?,
    val avatar: String?,
    val createdAt: Instant,
)