package io.github.clfundev.better.ui.plan

import android.os.Parcelable
import io.github.clfundev.better.data.db.entity.Plan
import kotlinx.parcelize.Parcelize

// 计划页面的列表项
@Parcelize
data class PlanInfo(
    var plan: Plan,
    var specPlanStart: Long,
    var completeAmount: Long,
    var totalAchieved: Int,
    var continuousAchieved: Int,
    var achieveType: Int
) : Parcelable {
    companion object {
        const val ACHIEVE_TYPE_CONTINUOUS: Int = 0
        const val ACHIEVE_TYPE_TOTAL: Int = 1
    }
}