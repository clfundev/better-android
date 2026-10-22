package io.github.clfundev.better.ui.settings

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import io.github.clfundev.better.R
import io.github.clfundev.better.databinding.DialogClearDataBinding
import io.github.clfundev.better.ui.ConfirmDialogFragment
import io.github.clfundev.better.utils.getAppContainer
import kotlinx.coroutines.launch

class ClearDataDialogFragment : DialogFragment() {
    private var _binding: DialogClearDataBinding? = null
    private val binding: DialogClearDataBinding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogClearDataBinding.inflate(requireActivity().layoutInflater)

        val context = requireContext()
        val fragmentManager = getChildFragmentManager()

        val planCheckBox = binding.checkboxCleardataPlan
        val focusRecordCheckBox = binding.checkboxCleardataFocusrecord
        val confirmTv = binding.btnbarCleardata.tvBtnbarPositive
        val cancelTv = binding.btnbarCleardata.tvBtnbarNegative

        confirmTv.setText(R.string.all_confirm)
        confirmTv.setOnClickListener {
            val focusRecordsChecked = focusRecordCheckBox.isChecked
            val plansChecked = planCheckBox.isChecked
            if (!focusRecordsChecked && !plansChecked) {
                // 没有一项选中
                dismiss()
                return@setOnClickListener
            }
            // 创建对话框询问用户是否确认删除
            if (fragmentManager.findFragmentByTag(ConfirmDialogFragment.TAG) == null) {
                val content = buildString {
                    if (plansChecked) {
                        append("•")
                        append(getString(R.string.cleardata_plans_option))
                    }
                    if (plansChecked && focusRecordsChecked) {
                        append("\n")
                    }
                    if (focusRecordsChecked) {
                        append("•")
                        append(getString(R.string.cleardata_focus_records_option))
                    }
                }
                ConfirmDialogFragment.newInstance(
                    CONFIRM_CLEAR_DATA,
                    getString(R.string.settings_clear_data_warning),
                    content,
                    getString(R.string.all_confirm),
                    getString(R.string.all_cancel)
                ).show(fragmentManager, ConfirmDialogFragment.TAG)
            }
        }
        cancelTv.setText(R.string.all_cancel)
        cancelTv.setOnClickListener { dismiss() }

        fragmentManager.setFragmentResultListener(CONFIRM_CLEAR_DATA, this) { _, result ->
            if (result.getInt(ConfirmDialogFragment.KEY_WHICH) == ConfirmDialogFragment.RESULT_POSITIVE) {
                // 用户确认删除
                if (planCheckBox.isChecked) {
                    // 由于有外键约束，删除计划时会同时删除对应记录
                    lifecycleScope.launch {
                        context.getAppContainer().planRepository.deleteAllPlans(context)
                    }
                }
                if (focusRecordCheckBox.isChecked) {
                    lifecycleScope.launch {
                        context.getAppContainer().focusRecordRepository.deleteAllFocusRecords()
                    }
                }
                dismiss()
            }
        }

        val dialog = Dialog(context, R.style.FadeAnimDialog)
        dialog.setContentView(binding.root)
        val window = dialog.window
        if (window != null) {
            val lp = window.attributes
            lp.width = resources.displayMetrics.widthPixels / 8 * 7 // 宽度
            window.setAttributes(lp)
        }
        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG: String = "ClearDataDialogFragment"

        // 用于请求ConfirmDialog返回确认结果
        private const val CONFIRM_CLEAR_DATA = "CONFIRM_CLEAR_DATA"
    }
}