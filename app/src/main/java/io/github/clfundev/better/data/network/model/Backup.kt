package io.github.clfundev.better.data.network.model

import java.time.LocalDateTime

/**
 * 备份
 */
data class Backup(
    val backupId: String,
    val userId: Int,
    val createTime: LocalDateTime,
    val size: Long
)