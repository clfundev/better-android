package io.github.clfundev.better.ui

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.DialogFragment
import io.github.clfundev.better.R
import io.github.clfundev.better.databinding.DialogDhmPickerBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DHMPickerDialogFragment : DialogFragment() {
    // 请求码，用于返回结果到对应监听器
    private lateinit var requestKey: String

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogDhmPickerBinding.inflate(requireActivity().layoutInflater)

        val indicatorTv = binding.tvDhmpickerIndicator
        val dateWp = binding.wpDhmpickerDate
        val hourWp = binding.wpDhmpickerHour
        val minuteWp = binding.wpDhmpickerMinute
        val confirmTv = binding.btnbarDhmpicker.tvBtnbarPositive
        val cancelTv = binding.btnbarDhmpicker.tvBtnbarNegative

        val context = requireContext()
        val pattern = context.getString(R.string.all_date_dayofweek_format)
        val dhmFormatter = DateTimeFormatter.ofPattern(pattern)

        val bundle = arguments
        if (bundle != null) {
            requestKey = bundle.getString(REQUEST_KEY) ?: ""
            val year = bundle.getInt(KEY_YEAR)
            val month = bundle.getInt(KEY_MONTH)
            val dayOfMonth = bundle.getInt(KEY_DAY_OF_MONTH)
            val hour = bundle.getInt(KEY_HOUR)
            val minute = bundle.getInt(KEY_MINUTE)
            val date = LocalDate.of(year, month, dayOfMonth)!!
            dateWp.selectItem(date, false)
            hourWp.selectItem(hour, false)
            minuteWp.selectItem(minute, false)
            indicatorTv.text = date.format(dhmFormatter)
        }
        if (savedInstanceState != null) {
            requestKey = savedInstanceState.getString(REQUEST_KEY) ?: ""
        }

        dateWp.setOnSelectListener { item, _ ->
            indicatorTv.text = item.format(dhmFormatter)
        }
        confirmTv.setText(R.string.all_confirm)
        confirmTv.setOnClickListener {
            // 将对话框选择结果通过setFragmentResult返回给Activity
            val result = Bundle().apply {
                val date = dateWp.getSelectedItem()
                putInt(KEY_YEAR, date.year)
                putInt(KEY_MONTH, date.monthValue)
                putInt(KEY_DAY_OF_MONTH, date.dayOfMonth)
                putInt(KEY_HOUR, hourWp.getSelectedItem())
                putInt(KEY_MINUTE, minuteWp.getSelectedItem())
            }
            parentFragmentManager.setFragmentResult(requestKey, result)
            dismiss()
        }
        cancelTv.setText(R.string.all_cancel)
        cancelTv.setOnClickListener { dismiss() }

        // 对话框进出动画、样式、布局设置
        val dialog = Dialog(context, R.style.SlideBottomAnimDialog)
        dialog.setContentView(binding.root)
        val window = dialog.window
        if (window != null) {
            val lp = window.attributes
            lp.gravity = Gravity.BOTTOM // 贴近底部
            lp.y = 80 // 与底部的距离
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
        const val TAG: String = "DHMPickerDialogFragment"

        // 返回结果、状态恢复用
        const val KEY_YEAR: String = "KEY_YEAR"
        const val KEY_MONTH: String = "KEY_MONTH"
        const val KEY_DAY_OF_MONTH: String = "KEY_DAY_OF_MONTH"
        const val KEY_HOUR: String = "KEY_HOUR"
        const val KEY_MINUTE: String = "KEY_MINUTE"

        // 状态恢复用
        private const val REQUEST_KEY = "REQUEST_KEY"

        fun newInstance(
            requestKey: String,
            year: Int,
            month: Int,
            dayOfMonth: Int,
            hour: Int,
            minute: Int
        ): DHMPickerDialogFragment {
            val bundle = Bundle().apply {
                putString(REQUEST_KEY, requestKey)
                putInt(KEY_YEAR, year)
                putInt(KEY_MONTH, month)
                putInt(KEY_DAY_OF_MONTH, dayOfMonth)
                putInt(KEY_HOUR, hour)
                putInt(KEY_MINUTE, minute)
            }
            return DHMPickerDialogFragment().apply { arguments = bundle }
        }
    }
}