package io.github.clfundev.better.ui.focus

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import io.github.clfundev.better.R
import io.github.clfundev.better.data.db.entity.SpecPlan
import io.github.clfundev.better.databinding.FragmentFocusBinding
import io.github.clfundev.better.service.FocusService
import io.github.clfundev.better.service.FocusService.PlanObserver
import io.github.clfundev.better.service.FocusService.TimeObserver
import io.github.clfundev.better.ui.focusrecord.FocusRecordActivity
import io.github.clfundev.better.ui.immersivefocus.ImmersiveFocusActivity
import io.github.clfundev.better.ui.main.MainActivity
import io.github.clfundev.better.ui.main.MainViewModel
import io.github.clfundev.better.utils.getThemeColor
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import androidx.appcompat.R as AppcompatR
import com.google.android.material.R as MaterialR

@SuppressLint("SetTextI18n")
class FocusFragment : Fragment(), TimeObserver, PlanObserver {
    private var _binding: FragmentFocusBinding? = null
    private val binding get() = _binding!!
    private val focusViewModel: FocusViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()
    private var focusService: FocusService? = null
    private val numberFormat: NumberFormat = NumberFormat.getNumberInstance().apply {
        maximumIntegerDigits = 2
        minimumIntegerDigits = 2
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFocusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = requireContext()

        binding.toolbarFocus.inflateMenu(R.menu.focus_toolbar_menu)
        binding.toolbarFocus.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_focus_record) {
                val intent = Intent(context, FocusRecordActivity::class.java)
                context.startActivity(intent)
            }
            true
        }
        binding.tvFocusPlan.setOnClickListener {
            // 打开专注计划选择对话框
            val fragmentManager = getChildFragmentManager()
            if (fragmentManager.findFragmentByTag(FocusPlanDialogFragment.TAG) == null) {
                FocusPlanDialogFragment().show(fragmentManager, FocusPlanDialogFragment.TAG)
            }
        }
        binding.tvFocusTime.setOnClickListener {
            if (focusService != null) {
                // 专注服务正在运行
                context.startActivity(Intent(context, ImmersiveFocusActivity::class.java))
            }
        }
        binding.tvFocusControl.setOnClickListener {
            if (focusService != null) {
                // 专注服务正在运行
                context.stopService(Intent(context, FocusService::class.java))
            } else {
                // 专注服务未运行
                (activity as? MainActivity)?.startFocus(focusViewModel.selectedSpecPlanFlow.value)
            }
        }
        lifecycleScope.launch {
            mainViewModel.focusServiceFlow.collectLatest { focusService: FocusService? ->
                this@FocusFragment.focusService = focusService
                val colorPrimary = context.getThemeColor(AppcompatR.attr.colorPrimary)
                val colorOnPrimary = context.getThemeColor(MaterialR.attr.colorOnPrimary)
                if (focusService != null) {
                    // 专注服务正在运行，监听专注任务数据
                    focusService.addTimeObserver(this@FocusFragment)
                    focusService.addPlanObserver(this@FocusFragment)
                    binding.tvFocusControl.setText(R.string.focus_stop)
                    binding.tvFocusControl.setTextColor(colorPrimary)
                    binding.tvFocusControl.background = ContextCompat.getDrawable(
                        context, R.drawable.btn_outlined_primary_r100
                    )
                } else {
                    // 专注服务未运行
                    binding.tvFocusTime.text = "00:00"
                    binding.cpvFocus.setProgress(0f)
                    binding.tvFocusState.text = ""
                    binding.tvFocusControl.setText(R.string.focus_start)
                    binding.tvFocusControl.setTextColor(colorOnPrimary)
                    binding.tvFocusControl.background =
                        ContextCompat.getDrawable(context, R.drawable.btn_filled_primary_r100)
                }
            }
        }
        lifecycleScope.launch {
            focusViewModel.selectedSpecPlanFlow.collectLatest { specPlan ->
                if (specPlan != null) {
                    binding.tvFocusPlan.text = specPlan.plan.title
                } else {
                    binding.tvFocusPlan.text = getString(R.string.focus_plan)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        focusService?.removeTimeObserver(this)
        focusService?.removePlanObserver(this)
    }

    override fun onCountdown(remainTime: Long) {
        val remainTimeStr = (numberFormat.format(remainTime / 60000) + ":"
                + numberFormat.format(remainTime / 1000 % 60))
        binding.tvFocusTime.text = remainTimeStr
    }

    override fun onStateChanged(remainTime: Long, duration: Long, isFocus: Boolean) {
        binding.tvFocusTime.text =
            "${numberFormat.format(remainTime / 60000)}:${numberFormat.format(remainTime / 1000 % 60)}"
        binding.cpvFocus.startAnimator(remainTime.toFloat() / duration, 0f, remainTime)
        binding.tvFocusState.setText(if (isFocus) R.string.all_focusing else R.string.all_relaxing)
    }

    override fun onPlanChanged(specPlan: SpecPlan?) {
        focusViewModel.selectSpecPlan(specPlan)
    }
}