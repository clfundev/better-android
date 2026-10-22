package io.github.clfundev.better.ui.editplan

import android.R.style.ThemeOverlay_Material_Dialog
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import io.github.clfundev.better.R
import io.github.clfundev.better.data.db.entity.Plan

class GoalTypeDialogFragment : DialogFragment() {
    private val viewModel: EditPlanViewModel by activityViewModels()
    private var goalType = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val oldGoalType = viewModel.goalTypeFlow.value
        goalType = savedInstanceState?.getInt(KEY_GOAL_TYPE) ?: oldGoalType
        val goalTypeItems = resources.getStringArray(R.array.goal_type_items)
        // 对话框样式要使用overlay theme才能保留app主题中的colorPrimary等属性(见AlertDialog.Builder注释)
        return AlertDialog.Builder(requireContext(), ThemeOverlay_Material_Dialog)
            .setTitle(R.string.editplan_goaltype)
            .setSingleChoiceItems(goalTypeItems, goalType) { _, which -> goalType = which }
            .setPositiveButton(R.string.all_confirm) { _, _ ->
                if (goalType != oldGoalType) {
                    viewModel.goalTypeFlow.value = goalType
                    viewModel.goalAmountFlow.value = if (goalType == Plan.GOAL_TYPE_COUNT) {
                        1L
                    } else {
                        300000L
                    }
                }
                dismiss()
            }.create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_GOAL_TYPE, goalType)
        super.onSaveInstanceState(outState)
    }

    companion object {
        const val TAG = "GoalTypeDialogFragment"
        private const val KEY_GOAL_TYPE = "KEY_GOAL_TYPE"
    }
}