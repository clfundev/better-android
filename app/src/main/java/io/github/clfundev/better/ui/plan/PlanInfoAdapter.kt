package io.github.clfundev.better.ui.plan

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.RecyclerView
import io.github.clfundev.better.R
import io.github.clfundev.better.data.db.entity.Plan
import io.github.clfundev.better.databinding.ItemNoPlanBinding
import io.github.clfundev.better.databinding.ItemPlanBinding
import io.github.clfundev.better.occur.toLocalDateTime
import io.github.clfundev.better.utils.getSimpleColor
import io.github.clfundev.better.utils.getThemeColor
import io.github.clfundev.better.widget.SlideLinearLayout
import io.github.clfundev.better.widget.SlideLinearLayout.OnMenuStateChangedListener
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class PlanInfoAdapter(
    private val context: Context,
    private val planInfos: List<PlanInfo>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var focusingPlanId = -1L
    private var focusingPlanStart = -1L
    private var onItemInteractListener: OnItemInteractListener? = null

    private class PlanViewHolder(binding: ItemPlanBinding) : RecyclerView.ViewHolder(binding.root) {
        val slideLinearLayout: SlideLinearLayout = binding.sllRvplan
        val leftLayout: RelativeLayout = binding.rlRvplanLeft
        val midLayout: ConstraintLayout = binding.midRvplan.root
        val leftIv: ImageView = binding.ivRvplanLeft
        val timeTv: TextView = binding.midRvplan.tvRvplanmidTime
        val titleTv: TextView = binding.midRvplan.tvRvplanmidTitle
        val completeTv: TextView = binding.midRvplan.tvRvplanmidComplete
        val achieveTv: TextView = binding.midRvplan.tvRvplanmidAchieve
        val achieveTypeTv: TextView = binding.midRvplan.tvRvplanmidAchievetype
        val achieveLayout: LinearLayout = binding.midRvplan.llRvplanmidAchieve
        val animator: ValueAnimator
        val cardColor: Int

        init {
            val context = binding.root.context
            val argbEvaluator = ArgbEvaluator()
            cardColor = context.getThemeColor(R.attr.appCardColor)
            // 获取紫色和卡片色的中间值
            val midPurple = argbEvaluator.evaluate(
                0.5f, cardColor, context.getSimpleColor(R.color.primary_light)
            ) as Int
            // 创建背景色渐变动画
            animator = ObjectAnimator.ofInt(
                midLayout, "backgroundColor", cardColor, midPurple
            ).apply {
                duration = 1500L
                interpolator = AccelerateDecelerateInterpolator()
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                setEvaluator(argbEvaluator)
            }
        }
    }

    private class NoPlanViewHolder(binding: ItemNoPlanBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == TYPE_PLAN) {
            val binding =
                ItemPlanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return PlanViewHolder(binding)
        } else {
            val binding =
                ItemNoPlanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return NoPlanViewHolder(binding)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is PlanViewHolder) {
            val planInfo = planInfos[position]

            // 计划发生时间
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            val beginDt = planInfo.specPlanStart.toLocalDateTime()
            val endDt = (planInfo.specPlanStart + planInfo.plan.duration).toLocalDateTime()
            holder.timeTv.text = if (ChronoUnit.MINUTES.between(beginDt, endDt) > 0) {
                "${formatter.format(beginDt)}\n|\n${formatter.format(endDt)}"
            } else {
                formatter.format(beginDt)
            }

            // 计划标题
            holder.titleTv.text = planInfo.plan.title

            // 计划完成量
            val completeAmount = planInfo.completeAmount
            val goalAmount = planInfo.plan.goalAmount
            holder.completeTv.text = if (planInfo.plan.goalType == Plan.GOAL_TYPE_COUNT) {
                "$completeAmount / $goalAmount"
            } else {
                "${formatDuration(completeAmount)} / ${formatDuration(goalAmount)}"
            }

            // 根据不同的目标类型设置左侧菜单的样式
            when (planInfo.plan.goalType) {
                Plan.GOAL_TYPE_COUNT -> {
                    holder.leftLayout.background =
                        context.getSimpleColor(R.color.green).toDrawable()
                    holder.leftIv.setImageResource(R.drawable.ic_check_24px)
                }

                Plan.GOAL_TYPE_FOCUS -> {
                    holder.leftLayout.background =
                        context.getSimpleColor(R.color.primary_light).toDrawable()
                    holder.leftIv.setImageResource(R.drawable.ic_baseline_trip_origin_24)
                }
            }

            // 计划目标达成情况
            val achieveStr: String
            val achieveTypeStr: String
            if (planInfo.achieveType == PlanInfo.ACHIEVE_TYPE_CONTINUOUS) {
                achieveStr = planInfo.continuousAchieved.toString()
                achieveTypeStr = context.getString(R.string.plan_continuous)
            } else {
                achieveStr = planInfo.totalAchieved.toString()
                achieveTypeStr = context.getString(R.string.plan_total)
            }
            holder.achieveTv.text = achieveStr
            holder.achieveTypeTv.text = achieveTypeStr

            // 正在专注或正在计时的计划开启背景闪烁动画
            if (planInfo.plan.id == focusingPlanId && planInfo.specPlanStart == focusingPlanStart) {
                // 判断动画是否正在运行，是为了防止用户点击达成情况，切换共达成、连续达成时，
                // 正在专注的item重新onBindViewHolder，导致动画重启突兀、不美观
                if (!holder.animator.isRunning) {
                    holder.animator.start()
                }
            } else {
                holder.animator.cancel()
                holder.midLayout.setBackgroundColor(holder.cardColor)
            }

            // 点击目标达成情况时切换总共达成、连续达成
            holder.midLayout.setOnClickListener {
                onItemInteractListener?.onMidLayoutClicked(holder.getBindingAdapterPosition())
            }

            holder.achieveLayout.setOnClickListener {
                onItemInteractListener?.onAchieveLayoutClicked(holder.getBindingAdapterPosition())
            }

            // 设置左右侧菜单打开监听
            holder.slideLinearLayout.setOnMenuStateChangedListener(object :
                OnMenuStateChangedListener {
                override fun onMenuStateChanged(menuState: Int) {
                    if (menuState == SlideLinearLayout.LEFT_MENU_OPENED) {
                        onItemInteractListener?.onLeftMenuOpened(holder.getBindingAdapterPosition())
                    } else if (menuState == SlideLinearLayout.RIGHT_MENU_OPENED) {
                        onItemInteractListener?.onRightMenuOpened(holder.getBindingAdapterPosition())
                    }
                }
            })
        }
    }

    override fun getItemCount(): Int {
        return if (planInfos.isNotEmpty()) planInfos.size else 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (planInfos.isNotEmpty()) TYPE_PLAN else TYPE_NO_PLAN
    }

    fun setOnItemInteractListener(onItemInteractListener: OnItemInteractListener?) {
        this.onItemInteractListener = onItemInteractListener
    }

    fun setFocusingPlan(focusingPlanId: Long, focusingPlanStart: Long) {
        if (this.focusingPlanId == focusingPlanId && this.focusingPlanStart == focusingPlanStart) return
        val oldId = this.focusingPlanId
        val oldStart = this.focusingPlanStart
        this.focusingPlanId = focusingPlanId
        this.focusingPlanStart = focusingPlanStart
        // 遍历计划数据集，找到新旧专注item，通知它们更新界面
        for (i in planInfos.indices) {
            val planInfo = planInfos[i]
            val planId = planInfo.plan.id
            val planStart = planInfo.specPlanStart
            if ((planId == focusingPlanId && planStart == focusingPlanStart)
                || (planId == oldId && planStart == oldStart)
            ) {
                notifyItemChanged(i)
            }
        }
    }

    private fun formatDuration(duration: Long): String = buildString {
        val hours = duration / 3600000
        val minutes = duration / 60000 % 60
        if (hours > 0) {
            append(hours)
            append(context.getString(R.string.all_abbr_hour))
        }
        append(minutes)
        append(context.getString(R.string.all_abbr_minute))
    }

    interface OnItemInteractListener {
        fun onMidLayoutClicked(position: Int)

        fun onAchieveLayoutClicked(position: Int)

        fun onLeftMenuOpened(position: Int)

        fun onRightMenuOpened(position: Int)
    }

    companion object {
        // ViewHolder类型
        private const val TYPE_PLAN = 0
        private const val TYPE_NO_PLAN = 1
    }
}
