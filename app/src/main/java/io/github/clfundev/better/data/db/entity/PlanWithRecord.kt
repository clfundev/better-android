package io.github.clfundev.better.data.db.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * 计划及其完成记录
 */
data class PlanWithRecord(
    @Embedded val plan: Plan,
    @Relation(
        parentColumn = "id",
        entityColumn = "plan_id"
    )
    val planRecords: List<PlanRecord>
)
