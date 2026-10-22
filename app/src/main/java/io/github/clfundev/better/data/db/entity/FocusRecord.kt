package io.github.clfundev.better.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 专注记录实体类
 */
@Entity(tableName = "tb_focus_record")
data class FocusRecord(
    @PrimaryKey
    @ColumnInfo(name = "begin", typeAffinity = ColumnInfo.INTEGER)
    val begin: Long,
    @ColumnInfo(name = "end", typeAffinity = ColumnInfo.INTEGER)
    val end: Long
)
