package io.github.clfundev.better.ui.editplan

import androidx.lifecycle.ViewModel
import io.github.clfundev.better.data.db.entity.Plan
import io.github.clfundev.better.occur.RecurRule
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDateTime

class EditPlanViewModel : ViewModel() {
    // 正在编辑的计划
    var editingPlan: Plan? = null

    // 待更新计划缓存
    var planToUpdate: Plan? = null

    // 计划目标类型，默认为数量类型
    val goalTypeFlow = MutableStateFlow(Plan.GOAL_TYPE_COUNT)

    // 计划目标数量，默认为1
    val goalAmountFlow = MutableStateFlow(1L)

    // 计划开始时间，默认为当前时间（将秒和纳秒置为0，以保证只有年月日时分有意义）
    val startDateTimeFlow = MutableStateFlow(LocalDateTime.now().withSecond(0).withNano(0))

    // 计划时长，默认为0
    val durationFlow = MutableStateFlow(0L)

    // 计划重复规则，默认不重复
    val rruleFlow = MutableStateFlow<RecurRule?>(null)
}
