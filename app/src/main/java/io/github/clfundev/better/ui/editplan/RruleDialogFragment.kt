package io.github.clfundev.better.ui.editplan

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import io.github.clfundev.better.R
import io.github.clfundev.better.databinding.DialogRruleBinding
import io.github.clfundev.better.occur.RecurRule
import io.github.clfundev.better.widget.CircularCheckBox
import java.time.LocalDate

class RruleDialogFragment : DialogFragment(), View.OnClickListener {
    private val byDays = mutableSetOf<Int>()
    private val viewModel: EditPlanViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogRruleBinding.inflate(requireActivity().layoutInflater)

        val repeatSwitch = binding.switchRruleRepeat
        val intervalWp = binding.wpRruleInterval
        val freqWp = binding.wpRruleFreq

        val monCcb = binding.ccbRruleMon
        val tueCcb = binding.ccbRruleTue
        val wedCcb = binding.ccbRruleWed
        val thuCcb = binding.ccbRruleThu
        val friCcb = binding.ccbRruleFri
        val satCcb = binding.ccbRruleSat
        val sunCcb = binding.ccbRruleSun

        val setRepeatLl = binding.llRruleSetrepeat
        val byDaysGl = binding.glRruleBydays
        val confirmTv = binding.btnbarRrule.tvBtnbarPositive
        val cancelTv = binding.btnbarRrule.tvBtnbarNegative
        if (savedInstanceState != null) {
            byDays.apply {
                clear()
                savedInstanceState.getIntArray(KEY_BYDAYS)?.let { addAll(it.toList()) }
            }
        } else {
            // 初始化界面数据
            val recurRule = viewModel.rruleFlow.value
            if (recurRule != null) {
                repeatSwitch.setChecked(true)
                intervalWp.selectItem(recurRule.interval, false)
                freqWp.selectItem(recurRule.freq, false)
                setRepeatLl.visibility = View.VISIBLE
                byDaysGl.visibility =
                    if (recurRule.freq == RecurRule.WEEKLY) View.VISIBLE else View.GONE
                freqWp.setQuantity(recurRule.interval) // 保证复数等语法规则一开始就正确生效
            } else {
                repeatSwitch.setChecked(false)
                intervalWp.selectItem(1, false)
                freqWp.selectItem(RecurRule.DAILY, false)
                setRepeatLl.visibility = View.GONE
                byDaysGl.visibility = View.GONE
                freqWp.setQuantity(1) // 保证复数等语法规则一开始就正确生效
            }
            byDays.apply {
                clear()
                recurRule?.byDays?.let { addAll(it) }
                if (isEmpty()) add(LocalDate.now().getDayOfWeek().value)
            }
        }

        repeatSwitch.setOnCheckedChangeListener { _, isChecked ->
            setRepeatLl.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        intervalWp.setOnSelectListener { interval, _ ->
            // 数字改变，单复数形式也可能要变
            freqWp.setQuantity(interval)
        }

        freqWp.setOnSelectListener { freq, _ ->
            byDaysGl.visibility = if (freq == RecurRule.WEEKLY) View.VISIBLE else View.GONE
        }

        for (day in byDays) {
            when (day) {
                1 -> monCcb.checked = true
                2 -> tueCcb.checked = true
                3 -> wedCcb.checked = true
                4 -> thuCcb.checked = true
                5 -> friCcb.checked = true
                6 -> satCcb.checked = true
                7 -> sunCcb.checked = true
            }
        }
        monCcb.setOnClickListener(this)
        tueCcb.setOnClickListener(this)
        wedCcb.setOnClickListener(this)
        thuCcb.setOnClickListener(this)
        friCcb.setOnClickListener(this)
        satCcb.setOnClickListener(this)
        sunCcb.setOnClickListener(this)

        confirmTv.setText(R.string.all_confirm)
        confirmTv.setOnClickListener {
            val repeat = repeatSwitch.isChecked
            val freq = freqWp.getSelectedItem()
            val interval = intervalWp.getSelectedItem()
            viewModel.rruleFlow.value = if (repeat)
                RecurRule(freq, interval, if (freq == RecurRule.WEEKLY) byDays else null)
            else
                null
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
        outState.putIntArray(KEY_BYDAYS, byDays.toIntArray())
        super.onSaveInstanceState(outState)
    }

    // 监听各个星期复选框
    override fun onClick(v: View) {
        val checkBox = v as? CircularCheckBox
        val day = when (v.id) {
            R.id.ccb_rrule_mon -> 1
            R.id.ccb_rrule_tue -> 2
            R.id.ccb_rrule_wed -> 3
            R.id.ccb_rrule_thu -> 4
            R.id.ccb_rrule_fri -> 5
            R.id.ccb_rrule_sat -> 6
            R.id.ccb_rrule_sun -> 7
            else -> -1
        }
        if (checkBox != null && day != -1) {
            if (checkBox.checked) {
                byDays.add(day)
            } else if (byDays.size > 1) {
                byDays.remove(day)
            } else {
                // 至少保留一天
                checkBox.checked = true
            }
        }
    }

    companion object {
        const val TAG = "RruleDialogFragment"

        private const val KEY_BYDAYS = "KEY_BYDAYS"
    }
}