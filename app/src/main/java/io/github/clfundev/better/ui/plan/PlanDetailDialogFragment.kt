package io.github.clfundev.better.ui.plan

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import io.github.clfundev.better.R
import io.github.clfundev.better.data.db.entity.Plan
import io.github.clfundev.better.databinding.DialogPlanDetailBinding
import io.github.clfundev.better.occur.toLocalDateTime
import io.github.clfundev.better.utils.getSimpleColor
import io.github.clfundev.better.utils.getThemeColor
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class PlanDetailDialogFragment : DialogFragment() {
    private var _planInfo: PlanInfo? = null
    private val planInfo: PlanInfo get() = _planInfo!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogPlanDetailBinding.inflate(requireActivity().layoutInflater)
        val context = requireContext()

        _planInfo = savedInstanceState?.getParcelable(KEY_PLAN_INFO)
            ?: arguments?.getParcelable(KEY_PLAN_INFO)

        binding.tvPlandetailTitle.text = planInfo.plan.title

        val colorPrimary = context.getThemeColor(androidx.appcompat.R.attr.colorPrimary)
        val grayA40 = context.getSimpleColor(R.color.gray_a40)
        binding.ivPlandetailAlarm.setColorFilter(if (planInfo.plan.hasAlarm) colorPrimary else grayA40)

        val start = planInfo.specPlanStart.toLocalDateTime()
        val end = (planInfo.specPlanStart + planInfo.plan.duration).toLocalDateTime()
        binding.tvPlandetailTime.text = if (start.toLocalDate().isEqual(end.toLocalDate())) {
            // 不是跨天事件
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
            val dateFormatter =
                DateTimeFormatter.ofPattern(getString(R.string.all_date_dayofweek_format))
            val time = if (ChronoUnit.MINUTES.between(start, end) > 0) {
                timeFormatter.format(start) + " - " + timeFormatter.format(end)
            } else {
                timeFormatter.format(start)
            }
            val date = dateFormatter.format(start)
            val repeat = planInfo.plan.rrule?.toReadableStringOrNull(context)
            if (repeat != null) "$time\n$date\n$repeat" else "$time\n$date"
        } else {
            // 是跨天事件
            val dateTimeFormatter =
                DateTimeFormatter.ofPattern(getString(R.string.all_date_dayofweek_hour_minute_format))
            val startStr = dateTimeFormatter.format(start)
            val endStr = dateTimeFormatter.format(end)
            val dateTime = getString(R.string.plandetail_occur_cross_day, startStr, endStr)
            val repeat = planInfo.plan.rrule?.toReadableStringOrNull(context)
            if (repeat != null) "$dateTime\n$repeat" else dateTime
        }

        binding.tvPlandetailGoaltip.text = if (planInfo.plan.goalType == Plan.GOAL_TYPE_COUNT) {
            getString(R.string.plandetail_goal_count)
        } else {
            getString(R.string.plandetail_goal_focus)
        }

        binding.tvPlandetailGoal.text = if (planInfo.plan.goalType == Plan.GOAL_TYPE_COUNT) {
            "${planInfo.completeAmount} / ${planInfo.plan.goalAmount}"
        } else {
            "${formatDuration(planInfo.completeAmount)} / ${formatDuration(planInfo.plan.goalAmount)}"
        }

        binding.tvPlandetailTotal.text = planInfo.totalAchieved.toString()

        binding.tvPlandetailContinuous.text = planInfo.continuousAchieved.toString()

        binding.tvPlandetailDescription.text =
            planInfo.plan.description.ifEmpty { getString(R.string.plandetail_none) }

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

    private fun formatDuration(duration: Long): String = buildString {
        val hours = duration / 3600000
        val minutes = duration / 60000 % 60
        if (hours > 0) {
            append(hours)
            append(getString(R.string.all_abbr_hour))
        }
        append(minutes)
        append(getString(R.string.all_abbr_minute))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(KEY_PLAN_INFO, planInfo)
        super.onSaveInstanceState(outState)
    }

    companion object {
        const val TAG = "PlanDetailDialogFragment"
        private const val KEY_PLAN_INFO = "KEY_PLAN_INFO"

        @JvmStatic
        fun newInstance(planInfo: PlanInfo): PlanDetailDialogFragment {
            val fragment = PlanDetailDialogFragment()
            val bundle = Bundle()
            bundle.putParcelable(KEY_PLAN_INFO, planInfo)
            fragment.setArguments(bundle)
            return fragment
        }
    }
}
