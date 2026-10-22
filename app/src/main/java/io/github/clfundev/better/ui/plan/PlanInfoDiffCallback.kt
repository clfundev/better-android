package io.github.clfundev.better.ui.plan

import androidx.recyclerview.widget.DiffUtil

class PlanInfoDiffCallback(
    private val oldList: List<PlanInfo>,
    private val newList: List<PlanInfo>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldPlanInfo = oldList[oldItemPosition]
        val newPlanInfo = newList[newItemPosition]
        return oldPlanInfo.plan.id == newPlanInfo.plan.id
                && oldPlanInfo.specPlanStart == newPlanInfo.specPlanStart
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldPlanInfo = oldList[oldItemPosition]
        val newPlanInfo = newList[newItemPosition]
        return oldPlanInfo == newPlanInfo
    }
}