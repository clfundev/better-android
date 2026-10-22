package io.github.clfundev.better.ui.focus

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.clfundev.better.R
import io.github.clfundev.better.data.db.entity.SpecPlan
import io.github.clfundev.better.databinding.DialogFocusPlanBinding
import io.github.clfundev.better.occur.toLocalDateTime
import io.github.clfundev.better.service.FocusService
import io.github.clfundev.better.service.FocusService.PlanObserver
import io.github.clfundev.better.ui.main.MainActivity
import io.github.clfundev.better.ui.main.MainViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class FocusPlanDialogFragment : DialogFragment(), PlanObserver {
    private var _binding: DialogFocusPlanBinding? = null
    private val binding get() = _binding!!
    private var focusService: FocusService? = null
    private lateinit var focusPlanAdapter: FocusPlanAdapter
    private val specPlans = mutableListOf<SpecPlan>()
    private val mainViewModel: MainViewModel by activityViewModels()
    private val focusViewModel: FocusViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private val focusPlanViewModel: FocusPlanViewModel by viewModels { FocusPlanViewModel.factory }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogFocusPlanBinding.inflate(requireActivity().layoutInflater)

        // 根据已选中计划初始化日期
        if (savedInstanceState == null) {
            val specPlan = focusViewModel.selectedSpecPlanFlow.value
            val initialDate =
                specPlan?.specPlanStart?.toLocalDateTime()?.toLocalDate() ?: LocalDate.now()
            focusPlanViewModel.setDate(initialDate)
        }

        binding.tvFocusplanDate.setOnClickListener {
            if (childFragmentManager.findFragmentByTag(DatePickerFragment.TAG) == null) {
                DatePickerFragment().show(childFragmentManager, DatePickerFragment.TAG)
            }
        }

        focusPlanAdapter = FocusPlanAdapter(requireContext(), specPlans)
        focusPlanAdapter.setOnItemClickListener { position: Int ->
            val specPlan = specPlans[position]
            if (focusService != null) {
                // 服务正在运行，更新专注服务的专注计划
                (activity as? MainActivity)?.startFocus(specPlan)
            } else {
                focusViewModel.selectSpecPlan(specPlan)
            }
            dismiss()
        }
        binding.rvFocusplan.setLayoutManager(LinearLayoutManager(requireContext()))
        binding.rvFocusplan.setAdapter(focusPlanAdapter)

        lifecycleScope.launch {
            focusPlanViewModel.dateFlow.collectLatest {
                val formatter = DateTimeFormatter.ofPattern(getString(R.string.all_date_format))
                binding.tvFocusplanDate.text = it.format(formatter)
            }
        }
        lifecycleScope.launch {
            focusPlanViewModel.specPlansFlow.collectLatest {
                specPlans.clear()
                specPlans.addAll(it)
                focusPlanAdapter.notifyDataSetChanged()
            }
        }
        lifecycleScope.launch {
            mainViewModel.focusServiceFlow.collectLatest { focusService: FocusService? ->
                this@FocusPlanDialogFragment.focusService = focusService
                if (focusService != null) {
                    focusService.addPlanObserver(this@FocusPlanDialogFragment)
                } else {
                    focusPlanAdapter.setFocusingPlan(-1, -1)
                }
            }
        }

        val dialog = Dialog(requireContext(), R.style.FadeAnimDialog)
        dialog.setContentView(binding.root)
        val window = dialog.window
        if (window != null) {
            val lp = window.attributes
            lp.width = resources.displayMetrics.widthPixels / 8 * 7 // 宽度
            window.setAttributes(lp)
        }
        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        focusService?.removePlanObserver(this)
    }

    override fun onPlanChanged(specPlan: SpecPlan?) {
        focusPlanAdapter.setFocusingPlan(specPlan?.plan?.id ?: -1L, specPlan?.specPlanStart ?: -1L)
    }

    // 注意这个类只能是public static，否则报错
    class DatePickerFragment : DialogFragment(), OnDateSetListener {
        private val vm: FocusPlanViewModel by viewModels(
            ownerProducer = { requireParentFragment() },
            factoryProducer = { FocusPlanViewModel.factory }
        )

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val date = vm.dateFlow.value
            return DatePickerDialog(
                requireContext(), this, date.year, date.monthValue - 1, date.dayOfMonth
            )
        }

        override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
            vm.setDate(LocalDate.of(year, month + 1, dayOfMonth))
        }

        companion object {
            const val TAG: String = "DatePickerFragment"
        }
    }

    companion object {
        const val TAG: String = "FocusPlanDialogFragment"
    }
}
