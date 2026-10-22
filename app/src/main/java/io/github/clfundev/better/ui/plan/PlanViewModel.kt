package io.github.clfundev.better.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.clfundev.better.application.BetterApplication
import io.github.clfundev.better.data.db.entity.PlanRecord
import io.github.clfundev.better.data.db.entity.PlanWithRecord
import io.github.clfundev.better.data.repository.PlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneId

class PlanViewModel(planRepository: PlanRepository) : ViewModel() {
    private val _dateFlow = MutableStateFlow<LocalDate>(LocalDate.now())

    val dateFlow = _dateFlow.asStateFlow()

    private val planWithRecordsFlow = planRepository.getAllPlansWithRecords()

    val planInfosFlow = combine(_dateFlow, planWithRecordsFlow) { date, planWithRecords ->
        generatePlanInfos(date, planWithRecords)
    }

    private fun generatePlanInfos(
        date: LocalDate,
        planWithRecords: List<PlanWithRecord>
    ): List<PlanInfo> {
        val planInfos = mutableListOf<PlanInfo>()
        val dateStart = date.atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val dateEnd = date.plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        for (planWithRecord in planWithRecords) {
            val plan = planWithRecord.plan
            val planOccur = plan.occur
            val occurs = planOccur.getOccursDuring(dateStart, dateEnd)

            // 若当前计划在当前日期不发生，则跳过该计划
            if (occurs.isEmpty()) continue

            // 按计划开始时间排序，为之后二分查找和计算连续达成计划的次数做准备
            val planRecords = planWithRecord.planRecords.sortedBy { it.planStart }
            // 创建按planStart比较PlanRecord对象大小的比较器
            val comparator = Comparator.comparingLong(PlanRecord::planStart)

            // 为当前计划在当日的每次发生创建一个PlanInfo
            for (occur in occurs) {
                // 查找occur对应的planRecord
                var planRecordIndex =
                    planRecords.binarySearch(PlanRecord(-1L, occur, -1L), comparator)

                val completeAmount: Long
                if (planRecordIndex >= 0) {
                    // 当前occur有记录，获取完成量记录
                    completeAmount = planRecords[planRecordIndex].completeAmount
                } else {
                    // 当前occur无记录，即完成量为0
                    completeAmount = 0L
                    // 将planRecordIndex定位到当前occur之前的
                    // 最近一次记录，用于之后计算总共、连续达成计划的次数
                    planRecordIndex = -planRecordIndex - 2
                }

                // 把当前occur及之前的所有计划记录遍历一边，计算总共、连续达成计划的次数
                var totalAchieved = 0
                var continuousAchieved = 0
                // 指示是否还是连续达成计划
                var continuous = true
                var previousOccur: Long = occur
                for (i in planRecordIndex downTo 0) {
                    val planRec = planRecords[i]
                    if (planRec.completeAmount >= plan.goalAmount) {
                        // 计划达成
                        totalAchieved++
                        if (continuous) {
                            if (planRec.planStart == previousOccur) {
                                continuousAchieved++
                                previousOccur = planOccur.getPreviousOccur(previousOccur)
                            } else {
                                // 记录在发生时间上不连续
                                continuous = false
                            }
                        }
                    } else {
                        // 计划未达成，终止连续
                        continuous = false
                    }
                }

                // 创建PlanInfo对象，指定默认显示连续达成
                val planInfo = PlanInfo(
                    plan,
                    occur,
                    completeAmount,
                    totalAchieved,
                    continuousAchieved,
                    PlanInfo.ACHIEVE_TYPE_CONTINUOUS
                )
                planInfos.add(planInfo)
            }
        }

        // 按计划发生时间从早到晚排列
        planInfos.sortBy { it.specPlanStart }

        return planInfos
    }

    fun selectDate(date: LocalDate) {
        this._dateFlow.value = date
    }

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val appContainer = (this[APPLICATION_KEY] as BetterApplication).appContainer
                PlanViewModel(
                    planRepository = appContainer.planRepository
                )
            }
        }
    }
}