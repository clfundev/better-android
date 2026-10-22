package io.github.clfundev.better.ui

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.DialogFragment
import io.github.clfundev.better.R
import io.github.clfundev.better.databinding.DialogIntPickerBinding

class IntPickerDialogFragment : DialogFragment() {
    private lateinit var requestKey: String

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogIntPickerBinding.inflate(requireActivity().layoutInflater)

        val integerWp = binding.wpIntpicker
        val confirmTv = binding.btnbarIntpicker.tvBtnbarPositive
        val cancelTv = binding.btnbarIntpicker.tvBtnbarNegative

        val bundle = arguments
        if (bundle != null) {
            requestKey = bundle.getString(REQUEST_KEY) ?: ""
            val initialValue = bundle.getInt(KEY_INT)
            integerWp.selectItem(initialValue, false)
        }
        if (savedInstanceState != null) {
            requestKey = savedInstanceState.getString(REQUEST_KEY) ?: ""
        }

        confirmTv.setText(R.string.all_confirm)
        confirmTv.setOnClickListener {
            // 返回结果
            val result = Bundle().apply {
                putInt(KEY_INT, integerWp.getSelectedItem())
            }
            parentFragmentManager.setFragmentResult(requestKey, result)
            dismiss()
        }
        cancelTv.setText(R.string.all_cancel)
        cancelTv.setOnClickListener { dismiss() }

        val dialog = Dialog(requireContext(), R.style.SlideBottomAnimDialog)
        dialog.setContentView(binding.root)
        val window = dialog.window
        if (window != null) {
            val lp = window.attributes
            lp.gravity = Gravity.BOTTOM // 紧贴底部
            lp.y = 80 // 与底部距离
            lp.width = resources.displayMetrics.widthPixels / 8 * 7 // 宽度
            window.setAttributes(lp)
        }
        return dialog
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(REQUEST_KEY, requestKey)
        super.onSaveInstanceState(outState)
    }

    companion object {
        const val TAG: String = "IntPickerDialogFragment"
        const val KEY_INT: String = "KEY_INT"

        // 初始化、状态恢复、返回结果用
        private const val REQUEST_KEY = "REQUEST_KEY"

        fun newInstance(requestKey: String, integer: Int): IntPickerDialogFragment {
            val bundle = Bundle().apply {
                putString(REQUEST_KEY, requestKey)
                putInt(KEY_INT, integer)
            }
            return IntPickerDialogFragment().apply { arguments = bundle }
        }
    }
}