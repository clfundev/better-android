package io.github.clfundev.better.ui.plan

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.clfundev.better.R
import io.github.clfundev.better.data.db.entity.Plan
import io.github.clfundev.better.data.db.entity.PlanRecord
import io.github.clfundev.better.data.db.entity.SpecPlan
import io.github.clfundev.better.databinding.FragmentPlanBinding
import io.github.clfundev.better.service.FocusService
import io.github.clfundev.better.service.FocusService.PlanObserver
import io.github.clfundev.better.ui.editplan.EditPlanActivity
import io.github.clfundev.better.ui.main.MainActivity
import io.github.clfundev.better.ui.main.MainViewModel
import io.github.clfundev.better.ui.plan.PlanInfoAdapter.OnItemInteractListener
import io.github.clfundev.better.utils.getAppContainer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PlanFragment : Fragment(), PlanObserver {
    private lateinit var planInfoAdapter: PlanInfoAdapter
    private var _binding: FragmentPlanBinding? = null
    private val binding: FragmentPlanBinding get() = _binding!!
    private var focusService: FocusService? = null
    private val planInfos = mutableListOf<PlanInfo>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = requireContext()
        val mainViewModel: MainViewModel by activityViewModels()
        val planViewModel: PlanViewModel by viewModels { PlanViewModel.factory }
        val planRecordRepository = requireContext().getAppContainer().planRecordRepository
        val dateTv = binding.tvPlanDate
        val planRv = binding.rvPlan
        val navTodayFab = binding.fabPlanNavtoday
        val addPlanFab = binding.fabPlanAddplan

        dateTv.setOnClickListener {
            // 打开日期选择对话框选择日期
            if (childFragmentManager.findFragmentByTag(DatePickerFragment.TAG) == null) {
                DatePickerFragment().show(childFragmentManager, DatePickerFragment.TAG)
            }
        }

        planRv.setLayoutManager(LinearLayoutManager(context))
        planInfoAdapter = PlanInfoAdapter(context, planInfos)
        planInfoAdapter.setOnItemInteractListener(object : OnItemInteractListener {
            override fun onMidLayoutClicked(position: Int) {
                // 显示计划详情对话框
                val planInfo = planInfos[position]
                if (childFragmentManager.findFragmentByTag(PlanDetailDialogFragment.TAG) == null) {
                    PlanDetailDialogFragment.newInstance(planInfo)
                        .show(childFragmentManager, PlanDetailDialogFragment.TAG)
                }
            }

            override fun onAchieveLayoutClicked(position: Int) {
                // 切换显示连续达成、共达成
                val planInfo = planInfos[position]
                planInfo.achieveType = if (planInfo.achieveType == PlanInfo.ACHIEVE_TYPE_CONTINUOUS)
                    PlanInfo.ACHIEVE_TYPE_TOTAL
                else
                    PlanInfo.ACHIEVE_TYPE_CONTINUOUS
                planInfoAdapter.notifyItemChanged(position)
            }

            override fun onLeftMenuOpened(position: Int) {
                val planInfo = planInfos[position]
                val plan = planInfo.plan
                val specPlanStart = planInfo.specPlanStart

                when (plan.goalType) {
                    Plan.GOAL_TYPE_COUNT -> {
                        lifecycleScope.launch {
                            // 增加计划完成数量
                            val newCompleteAmount = planInfo.completeAmount + 1
                            val planRec = PlanRecord(plan.id, specPlanStart, newCompleteAmount)
                            planRecordRepository.insertPlanRecord(planRec)
                        }
                    }

                    Plan.GOAL_TYPE_FOCUS -> {
                        // 启动专注服务并传入专注计划信息
                        (activity as? MainActivity)?.startFocus(SpecPlan(plan, specPlanStart))
                        // 导航到专注页(与NavigationUI对导航栏的实现一致)
                        val navController = NavHostFragment.findNavController(this@PlanFragment)
                        val navOptions: NavOptions = NavOptions.Builder()
                            .setLaunchSingleTop(true)
                            .setRestoreState(true)
                            .setPopUpTo(
                                navController.graph.findStartDestination().id,
                                inclusive = false,
                                saveState = true
                            )
                            .setEnterAnim(androidx.navigation.ui.R.anim.nav_default_enter_anim)
                            .setExitAnim(androidx.navigation.ui.R.anim.nav_default_exit_anim)
                            .setPopEnterAnim(androidx.navigation.ui.R.anim.nav_default_pop_enter_anim)
                            .setPopExitAnim(androidx.navigation.ui.R.anim.nav_default_pop_exit_anim)
                            .build()
                        navController.navigate(R.id.navigation_focus, null, navOptions)
                    }
                }
            }

            override fun onRightMenuOpened(position: Int) {
                // 打开计划编辑界面
                val intent = Intent(context, EditPlanActivity::class.java)
                intent.putExtra(EditPlanActivity.EXTRA_PLAN, planInfos[position].plan)
                context.startActivity(intent)
            }
        })
        planRv.setAdapter(planInfoAdapter)

        // 去除RecyclerView的Item增删改移动画
        planRv.setItemAnimator(null)

        navTodayFab.setOnClickListener {
            // 导航到今天
            planViewModel.selectDate(LocalDate.now())
        }

        addPlanFab.setOnClickListener {
            // 打开编辑计划页面
            startActivity(Intent(context, EditPlanActivity::class.java))
        }

        lifecycleScope.launch {
            planViewModel.planInfosFlow.collectLatest { infos ->
                // 使用DiffUtil优化计划列表更新性能
                val diffResult = DiffUtil.calculateDiff(
                    PlanInfoDiffCallback(planInfos, infos)
                )
                planInfos.clear()
                planInfos.addAll(infos)
                diffResult.dispatchUpdatesTo(planInfoAdapter)
            }
        }

        lifecycleScope.launch {
            planViewModel.dateFlow.collectLatest { date ->
                // 日期变动
                val formatter = DateTimeFormatter.ofPattern(getString(R.string.all_date_format))
                dateTv.text = date.format(formatter)
                // 选中今天时，隐藏“导航到今天”悬浮按钮
                navTodayFab.visibility =
                    if (date.isEqual(LocalDate.now())) View.INVISIBLE else View.VISIBLE
            }
        }

        lifecycleScope.launch {
            mainViewModel.focusServiceFlow.collectLatest { focusService ->
                // 专注服务的运行状态更新
                this@PlanFragment.focusService = focusService
                if (focusService != null) {
                    // 专注服务正在运行，说明有计划正在专注中
                    focusService.addPlanObserver(this@PlanFragment)
                } else {
                    // 专注服务未运行，没有计划正在专注
                    planInfoAdapter.setFocusingPlan(-1, -1)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        focusService?.removePlanObserver(this)
        _binding = null
    }

    override fun onPlanChanged(specPlan: SpecPlan?) {
        val focusingPlanId = specPlan?.plan?.id ?: -1L
        val focusingPlanStart = specPlan?.specPlanStart ?: -1L
        planInfoAdapter.setFocusingPlan(focusingPlanId, focusingPlanStart)
    }

    // 日期选择对话框
    class DatePickerFragment : DialogFragment(), OnDateSetListener {
        private val vm: PlanViewModel by viewModels(
            ownerProducer = { requireParentFragment() },
            factoryProducer = { PlanViewModel.factory }
        )

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val date = vm.dateFlow.value
            return DatePickerDialog(
                requireContext(), this, date.year, date.monthValue - 1, date.dayOfMonth
            )
        }

        override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
            vm.selectDate(LocalDate.of(year, month + 1, dayOfMonth))
        }

        companion object {
            const val TAG: String = "DatePickerFragment"
        }
    }
}