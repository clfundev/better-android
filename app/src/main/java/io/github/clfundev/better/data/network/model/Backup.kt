package io.github.clfundev.better.data.network.model

import java.time.Instant

/**
 * 备份
 */
data class Backup(
    val id: String,
    val userId: Long,
    val size: Long,
    val createdAt: Instant,
)