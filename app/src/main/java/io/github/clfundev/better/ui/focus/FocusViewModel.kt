package io.github.clfundev.better.ui.focus

import androidx.lifecycle.ViewModel
import io.github.clfundev.better.data.db.entity.SpecPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// FocusFragment、FocusPlanDialogFragment共用
class FocusViewModel : ViewModel() {
    private val _selectedSpecPlanFlow = MutableStateFlow<SpecPlan?>(null)

    /**
     * 已选中计划，应与专注服务中正在专注的计划保持一致
     */
    val selectedSpecPlanFlow = _selectedSpecPlanFlow.asStateFlow()

    fun selectSpecPlan(specPlan: SpecPlan?) {
        _selectedSpecPlanFlow.value = specPlan
    }
}