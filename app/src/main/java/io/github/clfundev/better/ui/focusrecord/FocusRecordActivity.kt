package io.github.clfundev.better.ui.focusrecord

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import io.github.clfundev.better.R
import io.github.clfundev.better.data.db.entity.FocusRecord
import io.github.clfundev.better.databinding.ActivityFocusRecordBinding
import io.github.clfundev.better.occur.toLocalDateTime
import io.github.clfundev.better.ui.BaseActivity
import io.github.clfundev.better.widget.LineChartView.LineChartFormatter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class FocusRecordActivity : BaseActivity() {
    private val viewModel: FocusRecordViewModel by viewModels { FocusRecordViewModel.factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityFocusRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivFocusrecordBack.setOnClickListener { finish() }
        binding.linechartFocusrecordDuration.setStepCalculator { maxNum, lineCount ->
            // 15min-1h-2h-3h-6h
            val steps = longArrayOf(900000, 3600000, 7200000, 10800000, 21600000)
            steps.find { it * (lineCount - 1) > maxNum } ?: 1L
        }
        binding.linechartFocusrecordDuration.setLineChartFormatter(object : LineChartFormatter {
            override fun formatBgNum(num: Long): String {
                return getTimeStr(num)
            }

            override fun formatDetailNum(num: Long): String {
                return getTimeStr(num)
            }

            override fun formatBgDate(date: LocalDate): String {
                val formatter = DateTimeFormatter.ofPattern("M/d")
                return date.format(formatter)
            }

            override fun formatDetailDate(date: LocalDate): String {
                val pattern = getString(R.string.all_month_day_format)
                val formatter = DateTimeFormatter.ofPattern(pattern)
                return date.format(formatter)
            }
        })

        lifecycleScope.launch {
            viewModel.focusRecordsFlow.collectLatest {
                val datesDurationWithin =
                    generateDataSetWithin(calcDurationOfDates(it), NUM_DAYS)
                val total = calcTotalMillis(it)
                val avg = calcAvgDuration(datesDurationWithin.values)
                binding.tvFocusrecordTotalduration.text = getTimeStr(total)
                binding.tvFocusrecordDailyaverduration.text = getTimeStr(avg)
                binding.linechartFocusrecordDuration.setData(datesDurationWithin)
            }
        }
    }

    private fun getTimeStr(millis: Long): String {
        val h = millis / 3600000
        val m = millis / 60000 % 60
        return "$h${getString(R.string.all_abbr_hour)} $m${getString(R.string.all_abbr_minute)}"
    }

    private fun calcAvgDuration(duras: Collection<Long>): Long {
        if (duras.isEmpty()) return -1
        return duras.sum() / duras.size
    }

    private fun calcTotalMillis(focusRecords: List<FocusRecord>): Long =
        focusRecords.map { it.end - it.begin }.filter { it > 0 }.sum()

    private fun calcDurationOfDates(focusRecords: List<FocusRecord>): Map<LocalDate, Long> {
        val result = mutableMapOf<LocalDate, Long>()
        for (rec in focusRecords) {
            // 剔除不合法数据
            if (rec.begin >= rec.end) continue
            val beginDate = rec.begin.toLocalDateTime().toLocalDate()
            val endDate = rec.end.toLocalDateTime().toLocalDate()
            // 专注的开始时间和结束时间在同一天内
            if (beginDate.isEqual(endDate)) {
                val duration = rec.end - rec.begin
                result.merge(beginDate, duration) { a, b -> a + b }
                continue
            }
            // 专注的开始时间和结束时间不在同一天内，
            // 计算从专注开始到结束期间每天的专注时长
            var date = beginDate
            while (!date.isAfter(endDate)) {
                val duration = if (date.isEqual(beginDate)) {
                    date.plusDays(1)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli() - rec.begin
                } else if (date.isEqual(endDate)) {
                    rec.end - date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                } else {
                    86400000L
                }
                result.merge(date, duration) { a, b -> a + b }
                date = date.plusDays(1)
            }
        }
        return result
    }

    /**
     * 生成最近 [dayCount] 天内的专注时长数据集
     */
    private fun generateDataSetWithin(
        dateToDuration: Map<LocalDate, Long>,
        dayCount: Int
    ): Map<LocalDate, Long> {
        val result = mutableMapOf<LocalDate, Long>()
        val today = LocalDate.now()
        for (i in dayCount - 1 downTo 0) {
            val date = today.minusDays(i.toLong())
            result[date] = dateToDuration[date] ?: 0L
        }
        return result
    }

    companion object {
        private const val NUM_DAYS = 30
    }
}