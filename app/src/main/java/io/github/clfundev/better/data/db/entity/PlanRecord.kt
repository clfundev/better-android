package io.github.clfundev.better.data.db.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import kotlinx.parcelize.Parcelize

/**
 * 计划完成记录实体类
 */
@Entity(
    tableName = "tb_plan_record",
    primaryKeys = ["plan_id", "plan_start"],
    foreignKeys = [ForeignKey(
        entity = Plan::class,
        parentColumns = ["id"],
        childColumns = ["plan_id"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    )]
)
@Parcelize
data class PlanRecord(
    @ColumnInfo(name = "plan_id", typeAffinity = ColumnInfo.INTEGER)
    val planId: Long,
    @ColumnInfo(name = "plan_start", typeAffinity = ColumnInfo.INTEGER)
    val planStart: Long,
    @ColumnInfo(name = "complete_amount", typeAffinity = ColumnInfo.INTEGER)
    val completeAmount: Long
) : Parcelable