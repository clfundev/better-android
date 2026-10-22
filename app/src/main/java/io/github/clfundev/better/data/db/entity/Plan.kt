package io.github.clfundev.better.data.db.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.clfundev.better.occur.Occur
import io.github.clfundev.better.occur.OccurFactory
import io.github.clfundev.better.occur.RecurRule
import kotlinx.parcelize.Parcelize

/**
 * 计划实体类
 */
@Entity(tableName = "tb_plan")
@Parcelize
data class Plan(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id", typeAffinity = ColumnInfo.INTEGER)
    val id: Long,
    @ColumnInfo(name = "title", typeAffinity = ColumnInfo.TEXT)
    val title: String,
    @ColumnInfo(name = "description", typeAffinity = ColumnInfo.TEXT)
    val description: String,
    @ColumnInfo(name = "goal_type", typeAffinity = ColumnInfo.INTEGER)
    val goalType: Int,
    @ColumnInfo(name = "goal_amount", typeAffinity = ColumnInfo.INTEGER)
    val goalAmount: Long,
    @ColumnInfo(name = "start", typeAffinity = ColumnInfo.INTEGER)
    val start: Long,
    @ColumnInfo(name = "duration", typeAffinity = ColumnInfo.INTEGER)
    val duration: Long,
    @ColumnInfo(name = "rrule", typeAffinity = ColumnInfo.TEXT)
    val rrule: RecurRule?,
    @ColumnInfo(name = "has_alarm", typeAffinity = ColumnInfo.INTEGER)
    val hasAlarm: Boolean
) : Parcelable {
    constructor(
        title: String,
        description: String,
        goalType: Int,
        goalAmount: Long,
        start: Long,
        duration: Long,
        rrule: RecurRule?,
        hasAlarm: Boolean
    ) : this(0, title, description, goalType, goalAmount, start, duration, rrule, hasAlarm)

    val occur: Occur get() = OccurFactory.createOccur(start, rrule)

    /**
     * 比较当前对象与 [anotherPlan] 是否在不影响计划完成记录的条件下相等
     *
     * 若两个计划的目标一致（目标类型和目标个数一致）、
     * 发生时间一致（计划首次开始时间和计划重复规则一致），
     * 则视为相等
     */
    fun filterEquals(anotherPlan: Plan?): Boolean {
        if (this == anotherPlan) {
            return true
        }
        if (anotherPlan != null) {
            if (this.goalType != anotherPlan.goalType) return false
            if (this.goalAmount != anotherPlan.goalAmount) return false
            if (this.start != anotherPlan.start) return false
            if (this.rrule != anotherPlan.rrule) return false
            return true
        }
        return false
    }

    companion object {
        const val GOAL_TYPE_COUNT = 0
        const val GOAL_TYPE_FOCUS = 1
    }
}