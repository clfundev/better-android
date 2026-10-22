package io.github.clfundev.better.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.clfundev.better.application.BetterApplication
import io.github.clfundev.better.data.db.entity.Plan
import io.github.clfundev.better.data.db.entity.SpecPlan
import io.github.clfundev.better.data.repository.PlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneId

class FocusPlanViewModel(planRepository: PlanRepository) : ViewModel() {
    private val _dateFlow = MutableStateFlow(LocalDate.now())

    private val focusPlansFlow = planRepository.getPlansByGoalType(Plan.GOAL_TYPE_FOCUS)

    val dateFlow = _dateFlow.asStateFlow()

    val specPlansFlow = combine(_dateFlow, focusPlansFlow) { date, plans ->
        getOccurredSpecPlans(date, plans)
    }

    fun setDate(date: LocalDate) {
        _dateFlow.value = date
    }

    // 获取指定日期发生的计划
    private fun getOccurredSpecPlans(date: LocalDate, plans: List<Plan>): List<SpecPlan> {
        val specPlans = mutableListOf<SpecPlan>()
        val dateStart =
            date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val dateEnd =
            date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        for (plan in plans) {
            val occurs = plan.occur.getOccursDuring(dateStart, dateEnd)
            for (occur in occurs) {
                specPlans.add(SpecPlan(plan, occur))
            }
        }
        // 按计划发生时间从早到晚排列
        specPlans.sortBy { it.specPlanStart }
        return specPlans
    }

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val appContainer = (this[APPLICATION_KEY] as BetterApplication).appContainer
                FocusPlanViewModel(
                    planRepository = appContainer.planRepository
                )
            }
        }
    }
}
