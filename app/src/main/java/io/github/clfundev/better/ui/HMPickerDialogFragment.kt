package io.github.clfundev.better.ui

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.DialogFragment
import io.github.clfundev.better.R
import io.github.clfundev.better.databinding.DialogHmPickerBinding

class HMPickerDialogFragment : DialogFragment() {
    private lateinit var requestKey: String

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogHmPickerBinding.inflate(requireActivity().layoutInflater)

        val hourWp = binding.wpHmpickerHour
        val minuteWp = binding.wpHmpickerMinute
        val confirmTv = binding.btnbarHmpicker.tvBtnbarPositive
        val cancelTv = binding.btnbarHmpicker.tvBtnbarNegative

        val bundle = arguments
        if (bundle != null) {
            requestKey = bundle.getString(REQUEST_KEY) ?: ""
            hourWp.selectItem(bundle.getInt(KEY_HOUR), false)
            minuteWp.selectItem(bundle.getInt(KEY_MINUTE), false)
        }
        if (savedInstanceState != null) {
            requestKey = savedInstanceState.getString(REQUEST_KEY) ?: ""
        }

        confirmTv.setText(R.string.all_confirm)
        confirmTv.setOnClickListener {
            // 将对话框选择的时间返回给Activity
            val result = Bundle()
            result.putInt(KEY_HOUR, hourWp.getSelectedItem())
            result.putInt(KEY_MINUTE, minuteWp.getSelectedItem())
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
        const val TAG: String = "HMPickerDialogFragment"
        const val KEY_HOUR: String = "KEY_HOUR"
        const val KEY_MINUTE: String = "KEY_MINUTE"
        private const val REQUEST_KEY = "REQUEST_KEY"

        fun newInstance(requestKey: String, hour: Int, minute: Int): HMPickerDialogFragment {
            val fragment = HMPickerDialogFragment()
            val bundle = Bundle()
            bundle.putString(REQUEST_KEY, requestKey)
            bundle.putInt(KEY_HOUR, hour)
            bundle.putInt(KEY_MINUTE, minute)
            fragment.setArguments(bundle)
            return fragment
        }
    }
}