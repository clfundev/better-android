package io.github.clfundev.better.ui.focus

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.recyclerview.widget.RecyclerView
import io.github.clfundev.better.R
import io.github.clfundev.better.data.db.entity.SpecPlan
import io.github.clfundev.better.databinding.ItemFocusPlanBinding
import io.github.clfundev.better.databinding.ItemNoFocusPlanBinding
import io.github.clfundev.better.occur.toLocalDateTime
import io.github.clfundev.better.utils.getThemeColor
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class FocusPlanAdapter(
    private val context: Context,
    private val focusPlans: List<SpecPlan>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var focusingPlanId = -1L
    private var focusingPlanStart = -1L
    private var onItemClickListener: OnItemClickListener? = null

    private class FocusPlanViewHolder(
        binding: ItemFocusPlanBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        val subLayout = binding.clRvfocusplanSub
        val titleTv = binding.tvRvfocusplanTitle
        val timeTv = binding.tvRvfocusplanTime
        val goalTv = binding.tvRvfocusplanGoal
        val animator: ValueAnimator
        val elevatedCardColor: Int

        init {
            val context = binding.root.context
            val argbEvaluator = ArgbEvaluator()
            elevatedCardColor = context.getThemeColor(R.attr.appElevatedCardColor)
            val colorPrimary = context.getThemeColor(androidx.appcompat.R.attr.colorPrimary)
            val midColor = argbEvaluator.evaluate(0.5f, elevatedCardColor, colorPrimary) as Int
            // 创建背景色渐变动画
            animator = ObjectAnimator.ofInt(
                subLayout, "backgroundColor", elevatedCardColor, midColor
            ).apply {
                setDuration(1500)
                interpolator = AccelerateDecelerateInterpolator()
                setEvaluator(argbEvaluator)
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
            }
        }
    }

    private class NoFocusPlanViewHolder(binding: ItemNoFocusPlanBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == TYPE_FOCUS_PLAN) {
            val binding =
                ItemFocusPlanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return FocusPlanViewHolder(binding)
        } else {
            val binding =
                ItemNoFocusPlanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return NoFocusPlanViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is FocusPlanViewHolder) {
            val specPlan = focusPlans[position]
            // 计划标题
            holder.titleTv.text = specPlan.plan.title
            // 计划发生时间
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            val startDateTime = specPlan.specPlanStart.toLocalDateTime()
            val endDateTime = (specPlan.specPlanStart + specPlan.plan.duration).toLocalDateTime()
            holder.timeTv.text = if (ChronoUnit.MINUTES.between(startDateTime, endDateTime) > 0)
                "${formatter.format(startDateTime)} - ${formatter.format(endDateTime)}"
            else
                formatter.format(startDateTime)
            // 计划目标专注时长
            val durationStr = buildString {
                val goalAmount = specPlan.plan.goalAmount
                val goalHours = goalAmount / 3600000
                val goalMins = goalAmount / 60000 % 60
                if (goalHours > 0) {
                    append(goalHours)
                    append(context.getString(R.string.all_abbr_hour))
                }
                append(goalMins)
                append(context.getString(R.string.all_abbr_minute))
            }
            holder.goalTv.text = context.getString(R.string.focusplan_goal, durationStr)
            // 计划点击回调
            holder.subLayout.setOnClickListener {
                onItemClickListener?.onItemClick(holder.getBindingAdapterPosition())
            }
            // 正在专注或正在计时的计划开启背景闪烁动画
            if (specPlan.plan.id == focusingPlanId
                && specPlan.specPlanStart == focusingPlanStart
            ) {
                // 判断动画是否正在运行，是为了防止用户点击达成情况，切换共达成、连续达成时，
                // 正在专注的item重新onBindViewHolder，导致动画重启突兀、不美观
                if (!holder.animator.isRunning) {
                    holder.animator.start()
                }
            } else {
                holder.animator.cancel()
                holder.subLayout.setBackgroundColor(holder.elevatedCardColor)
            }
        }
    }

    override fun getItemCount(): Int {
        return if (focusPlans.isNotEmpty()) focusPlans.size else 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (focusPlans.isNotEmpty()) TYPE_FOCUS_PLAN else TYPE_NO_FOCUS_PLAN
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener?) {
        this.onItemClickListener = onItemClickListener
    }

    fun setFocusingPlan(focusingPlanId: Long, focusingPlanStart: Long) {
        if (this.focusingPlanId == focusingPlanId && this.focusingPlanStart == focusingPlanStart) return
        // 遍历计划数据集，找到新旧专注item，通知它们更新界面
        for (i in focusPlans.indices) {
            val specPlan = focusPlans[i]
            val planId = specPlan.plan.id
            val planStart = specPlan.specPlanStart
            if ((planId == focusingPlanId && planStart == focusingPlanStart)
                || (planId == this.focusingPlanId && planStart == this.focusingPlanStart)
            ) {
                notifyItemChanged(i)
            }
        }
        this.focusingPlanId = focusingPlanId
        this.focusingPlanStart = focusingPlanStart
    }

    fun interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    companion object {
        // ViewHolder类型
        private const val TYPE_FOCUS_PLAN = 0
        private const val TYPE_NO_FOCUS_PLAN = 1
    }
}
