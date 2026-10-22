package io.github.clfundev.better.ui.editplan

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import io.github.clfundev.better.R
import io.github.clfundev.better.data.db.entity.Plan
import io.github.clfundev.better.data.repository.PlanRecordRepository
import io.github.clfundev.better.data.repository.PlanRepository
import io.github.clfundev.better.databinding.ActivityEditPlanBinding
import io.github.clfundev.better.occur.toLocalDateTime
import io.github.clfundev.better.occur.toMilli
import io.github.clfundev.better.ui.BaseActivity
import io.github.clfundev.better.ui.ConfirmDialogFragment
import io.github.clfundev.better.ui.DHMPickerDialogFragment
import io.github.clfundev.better.ui.HMPickerDialogFragment
import io.github.clfundev.better.ui.IntPickerDialogFragment
import io.github.clfundev.better.utils.getAppContainer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class EditPlanActivity : BaseActivity() {
    private lateinit var binding: ActivityEditPlanBinding
    private lateinit var planRepository: PlanRepository
    private lateinit var planRecordRepository: PlanRecordRepository
    private val viewModel: EditPlanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditPlanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val appContainer = getAppContainer()
        planRepository = appContainer.planRepository
        planRecordRepository = appContainer.planRecordRepository

        // 有计划传入就是编辑计划，无计划传入就是新建计划
        if (savedInstanceState == null && intent != null) {
            viewModel.editingPlan = intent.getParcelableExtra(EXTRA_PLAN)
            viewModel.editingPlan?.let {
                // TODO: 由ViewModel管理所有UI状态
                binding.etEditplanTitle.setText(it.title)
                binding.etEditplanDescription.setText(it.description)
                viewModel.goalTypeFlow.value = it.goalType
                viewModel.goalAmountFlow.value = it.goalAmount
                viewModel.startDateTimeFlow.value = it.start.toLocalDateTime()
                viewModel.durationFlow.value = it.duration
                viewModel.rruleFlow.value = it.rrule
                binding.switchEditplanAlarm.isChecked = it.hasAlarm
            }
        }

        binding.tvEditplanHeader.text = if (isEditing())
            getString(R.string.editplan_edit_plan_header)
        else
            getString(R.string.editplan_new_plan_header)

        binding.tvEditplanDelete.visibility = if (isEditing()) View.VISIBLE else View.GONE

        // 设置点击监听
        setupOnClickListeners()
        // 设置对话框返回监听
        setupDialogResultListeners()
        // 将ViewModel中的数据绑定到View
        bindData()
    }

    private fun isEditing() = viewModel.editingPlan != null

    private fun checkInput(): Boolean {
        val title = binding.etEditplanTitle.text.toString()
        val goalType = viewModel.goalTypeFlow.value
        val goalAmount = viewModel.goalAmountFlow.value
        // 标题不能为空
        if (title.isEmpty()) {
            Toast.makeText(this, R.string.editplan_require_title, Toast.LENGTH_SHORT).show()
            return false
        }
        // 数量目标不能小于1个
        if (goalType == Plan.GOAL_TYPE_COUNT && goalAmount < 1) {
            Toast.makeText(this, R.string.editplan_invalid_count_goalamount, Toast.LENGTH_SHORT)
                .show()
            return false
        }
        // 时长目标不能小于5分钟
        if (goalType == Plan.GOAL_TYPE_FOCUS && goalAmount < 300000) {
            Toast.makeText(this, R.string.editplan_invalid_time_goalamount, Toast.LENGTH_SHORT)
                .show()
            return false
        }
        return true
    }

    private fun setupOnClickListeners() {
        binding.ivEditplanCancel.setOnClickListener { finish() }

        binding.ivEditplanConfirm.setOnClickListener {
            if (!checkInput()) return@setOnClickListener
            if (isEditing()) {
                // 更新计划
                val plan = Plan(
                    viewModel.editingPlan!!.id,
                    binding.etEditplanTitle.text.toString(),
                    binding.etEditplanDescription.text.toString(),
                    viewModel.goalTypeFlow.value,
                    viewModel.goalAmountFlow.value,
                    viewModel.startDateTimeFlow.value.toMilli(),
                    viewModel.durationFlow.value,
                    viewModel.rruleFlow.value,
                    binding.switchEditplanAlarm.isChecked
                )
                if (!plan.filterEquals(viewModel.editingPlan)) {
                    // 修改计划将会清空计划完成记录，显示对话框请求用户确认
                    if (supportFragmentManager.findFragmentByTag(ConfirmDialogFragment.TAG) == null) {
                        viewModel.planToUpdate = plan
                        ConfirmDialogFragment.newInstance(
                            CONFIRM_MODIFY_PLAN,
                            getString(R.string.all_warning),
                            getString(R.string.editplan_modify_plan_warning),
                            getString(R.string.all_yes),
                            getString(R.string.all_no)
                        ).show(supportFragmentManager, ConfirmDialogFragment.TAG)
                    }
                } else {
                    // 修改计划不影响计划完成记录，不用请求用户确认
                    lifecycleScope.launch {
                        planRepository.updatePlan(this@EditPlanActivity, plan)
                        finish()
                    }
                }
            } else {
                // 插入计划
                val plan = Plan(
                    binding.etEditplanTitle.text.toString(),
                    binding.etEditplanDescription.text.toString(),
                    viewModel.goalTypeFlow.value,
                    viewModel.goalAmountFlow.value,
                    viewModel.startDateTimeFlow.value.toMilli(),
                    viewModel.durationFlow.value,
                    viewModel.rruleFlow.value,
                    binding.switchEditplanAlarm.isChecked
                )
                lifecycleScope.launch {
                    planRepository.insertPlan(this@EditPlanActivity, plan)
                    finish()
                }
            }
        }

        binding.llEditplanGoaltype.setOnClickListener {
            if (supportFragmentManager.findFragmentByTag(GoalTypeDialogFragment.TAG) == null) {
                GoalTypeDialogFragment().show(supportFragmentManager, GoalTypeDialogFragment.TAG)
            }
        }

        binding.llEditplanGoalamount.setOnClickListener {
            // 不同目标类型，启动不同对话框
            val goalType = viewModel.goalTypeFlow.value
            val goalAmount = viewModel.goalAmountFlow.value
            when (goalType) {
                Plan.GOAL_TYPE_COUNT -> {
                    val tag = IntPickerDialogFragment.TAG
                    if (supportFragmentManager.findFragmentByTag(tag) == null) {
                        IntPickerDialogFragment.newInstance(
                            PICK_COUNT_GOAL_AMOUNT,
                            Math.toIntExact(goalAmount)
                        ).show(supportFragmentManager, tag)
                    }
                }

                Plan.GOAL_TYPE_FOCUS -> {
                    val tag = HMPickerDialogFragment.TAG
                    if (supportFragmentManager.findFragmentByTag(tag) == null) {
                        HMPickerDialogFragment.newInstance(
                            PICK_TIME_GOAL_AMOUNT,
                            (goalAmount / 3600000).toInt(),
                            (goalAmount / 60000 % 60).toInt()
                        ).show(supportFragmentManager, tag)
                    }
                }
            }
        }

        binding.llEditplanStart.setOnClickListener {
            if (supportFragmentManager.findFragmentByTag(DHMPickerDialogFragment.TAG) == null) {
                val startDateTime = viewModel.startDateTimeFlow.value
                DHMPickerDialogFragment.newInstance(
                    PICK_START,
                    startDateTime.year,
                    startDateTime.monthValue,
                    startDateTime.dayOfMonth,
                    startDateTime.hour,
                    startDateTime.minute
                ).show(supportFragmentManager, DHMPickerDialogFragment.TAG)
            }
        }

        binding.llEditplanDuration.setOnClickListener {
            if (supportFragmentManager.findFragmentByTag(HMPickerDialogFragment.TAG) == null) {
                val duration = viewModel.durationFlow.value
                HMPickerDialogFragment.newInstance(
                    PICK_DURATION,
                    (duration / 3600000).toInt(),
                    (duration / 60000 % 60).toInt()
                ).show(supportFragmentManager, HMPickerDialogFragment.TAG)
            }
        }

        binding.llEditplanRepeat.setOnClickListener {
            if (supportFragmentManager.findFragmentByTag(RruleDialogFragment.TAG) == null) {
                RruleDialogFragment().show(supportFragmentManager, RruleDialogFragment.TAG)
            }
        }

        binding.tvEditplanDelete.setOnClickListener {
            if (supportFragmentManager.findFragmentByTag(ConfirmDialogFragment.TAG) == null) {
                ConfirmDialogFragment.newInstance(
                    CONFIRM_DELETE,
                    getString(R.string.all_warning),
                    getString(R.string.editplan_delete_warning),
                    getString(R.string.all_yes),
                    getString(R.string.all_no)
                ).show(supportFragmentManager, ConfirmDialogFragment.TAG)
            }
        }
    }

    private fun setupDialogResultListeners() {
        supportFragmentManager.setFragmentResultListener(PICK_START, this) { _, result ->
            val year = result.getInt(DHMPickerDialogFragment.KEY_YEAR)
            val month = result.getInt(DHMPickerDialogFragment.KEY_MONTH)
            val dayOfMonth = result.getInt(DHMPickerDialogFragment.KEY_DAY_OF_MONTH)
            val hour = result.getInt(DHMPickerDialogFragment.KEY_HOUR)
            val minute = result.getInt(DHMPickerDialogFragment.KEY_MINUTE)
            val newStartDateTime = LocalDateTime.of(year, month, dayOfMonth, hour, minute)
            viewModel.startDateTimeFlow.value = newStartDateTime
        }

        supportFragmentManager.setFragmentResultListener(PICK_DURATION, this) { _, result ->
            val hour = result.getInt(HMPickerDialogFragment.KEY_HOUR)
            val minute = result.getInt(HMPickerDialogFragment.KEY_MINUTE)
            val newDuration = (hour * 60L + minute) * 60000L
            viewModel.durationFlow.value = newDuration
        }

        supportFragmentManager.setFragmentResultListener(
            PICK_COUNT_GOAL_AMOUNT,
            this
        ) { _, result ->
            val newGoalAmount = result.getInt(IntPickerDialogFragment.KEY_INT).toLong()
            viewModel.goalAmountFlow.value = newGoalAmount
        }

        supportFragmentManager.setFragmentResultListener(PICK_TIME_GOAL_AMOUNT, this) { _, result ->
            val hour = result.getInt(HMPickerDialogFragment.KEY_HOUR)
            val minute = result.getInt(HMPickerDialogFragment.KEY_MINUTE)
            val newGoalAmount = (hour * 60L + minute) * 60000L
            viewModel.goalAmountFlow.value = newGoalAmount
        }

        supportFragmentManager.setFragmentResultListener(CONFIRM_MODIFY_PLAN, this) { _, result ->
            val which = result.getInt(ConfirmDialogFragment.KEY_WHICH)
            if (which == ConfirmDialogFragment.RESULT_POSITIVE) {
                // 修改计划并清空计划完成记录
                viewModel.planToUpdate?.let {
                    lifecycleScope.launch {
                        planRepository.updatePlan(this@EditPlanActivity, it)
                        planRecordRepository.deletePlanRecordsByPlanId(it.id)
                        finish()
                    }
                }
            }
        }

        supportFragmentManager.setFragmentResultListener(CONFIRM_DELETE, this) { _, result ->
            val which = result.getInt(ConfirmDialogFragment.KEY_WHICH)
            if (which == ConfirmDialogFragment.RESULT_POSITIVE) {
                viewModel.editingPlan?.let {
                    lifecycleScope.launch {
                        planRepository.deletePlan(this@EditPlanActivity, it)
                        finish()
                    }
                }
            }
        }
    }

    private fun bindData() {
        lifecycleScope.launch {
            viewModel.startDateTimeFlow.collectLatest { startDateTime ->
                val pattern = getString(R.string.all_date_hour_minute_format)
                val formatter = DateTimeFormatter.ofPattern(pattern)
                binding.tvEditplanStart.text = startDateTime.format(formatter)
            }
        }
        lifecycleScope.launch {
            viewModel.goalTypeFlow.collectLatest { goalType ->
                val goalTypeItems = getResources().getStringArray(R.array.goal_type_items)
                binding.tvEditplanGoaltype.text = goalTypeItems[goalType]
            }
        }
        lifecycleScope.launch {
            viewModel.goalAmountFlow.collectLatest { goalAmount ->
                when (viewModel.goalTypeFlow.value) {
                    Plan.GOAL_TYPE_COUNT -> {
                        binding.tvEditplanGoalamount.text = goalAmount.toString()
                    }

                    Plan.GOAL_TYPE_FOCUS -> {
                        // 毫秒数转换为时长形式显示
                        val hour = goalAmount / 3600000
                        val min = goalAmount / 60000 % 60
                        val text = buildString {
                            if (hour > 0) {
                                append(hour)
                                append(getString(R.string.all_abbr_hour))
                                append(" ")
                            }
                            append(min)
                            append(getString(R.string.all_abbr_minute))
                        }
                        binding.tvEditplanGoalamount.text = text
                    }
                }
            }
        }
        lifecycleScope.launch {
            viewModel.durationFlow.collectLatest { duration ->
                // 毫秒数转换为时长形式显示
                val hour = duration / 3600000
                val min = duration / 60000 % 60
                val text = buildString {
                    if (hour > 0) {
                        append(hour)
                        append(getString(R.string.all_abbr_hour))
                        append(" ")
                    }
                    append(min)
                    append(getString(R.string.all_abbr_minute))
                }
                binding.tvEditplanDuration.text = text
            }
        }
        lifecycleScope.launch {
            viewModel.rruleFlow.collectLatest { recurRule ->
                val text = recurRule?.toReadableStringOrNull(this@EditPlanActivity)
                    ?: getString(R.string.editplan_neverrepeat)
                binding.tvEditplanRepeat.text = text
            }
        }
    }

    companion object {
        // 接收、保存要编辑的计划
        const val EXTRA_PLAN = "EXTRA_PLAN"

        // 用于监听对话框返回结果
        private const val PICK_START = "PICK_START"
        private const val PICK_DURATION = "PICK_DURATION"
        private const val PICK_COUNT_GOAL_AMOUNT = "PICK_COUNT_GOAL_AMOUNT"
        private const val PICK_TIME_GOAL_AMOUNT = "PICK_TIME_GOAL_AMOUNT"
        private const val CONFIRM_MODIFY_PLAN = "CONFIRM_MODIFY_PLAN"
        private const val CONFIRM_DELETE = "CONFIRM_DELETE"
    }
}