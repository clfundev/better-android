package io.github.clfundev.better.occur

import android.content.Context
import android.os.Parcelable
import io.github.clfundev.better.R
import kotlinx.parcelize.Parcelize

@Parcelize
data class RecurRule(val freq: String, val interval: Int, val byDays: Set<Int>?) : Parcelable {
    /**
     * 转换成rrule字符串
     */
    fun toPattern(): String? {
        if (!isValid()) return null
        return buildString {
            append("$FREQ=$freq;$INTERVAL=$interval")
            if (freq == WEEKLY) {
                check(!byDays.isNullOrEmpty()) { "Error: byDays is null or empty" }
                append(";$BYDAY=${byDays.joinToString(",") { dayOfWeekIntToStr(it) }}")
            }
        }
    }

    /**
     * 检查规则是否合法
     */
    private fun isValid(): Boolean {
        if (freq !in listOf(DAILY, WEEKLY, MONTHLY, YEARLY)) return false
        if (interval < 1) return false
        if (freq == WEEKLY) {
            if (byDays.isNullOrEmpty()) return false
            for (day in byDays) {
                if (day !in 1..7) return false
            }
        }
        return true
    }

    /**
     * 将RecurRule格式化为用户易读的文本形式，返回null表示该RecurRule不合法
     */
    fun toReadableStringOrNull(context: Context): String? {
        return when (freq) {
            DAILY -> context.resources.getQuantityString(
                R.plurals.editplan_repeat_daily,
                interval,
                interval
            )

            WEEKLY -> buildString {
                check(!byDays.isNullOrEmpty()) { "Error: byDays is null or empty" }
                append(
                    context.resources.getQuantityString(
                        R.plurals.editplan_repeat_weekly,
                        interval,
                        interval
                    )
                )
                val readableByDays = byDays.toList().sorted().joinToString(",") {
                    dayOfWeekIntToReadableStr(it, context)
                }
                append("($readableByDays)")
            }

            MONTHLY -> context.resources.getQuantityString(
                R.plurals.editplan_repeat_monthly,
                interval,
                interval
            )

            YEARLY -> context.resources.getQuantityString(
                R.plurals.editplan_repeat_yearly,
                interval,
                interval
            )

            else -> null
        }
    }

    companion object {
        const val FREQ: String = "FREQ"
        const val DAILY: String = "DAILY"
        const val WEEKLY: String = "WEEKLY"
        const val MONTHLY: String = "MONTHLY"
        const val YEARLY: String = "YEARLY"

        const val BYDAY: String = "BYDAY"
        const val MO: String = "MO"
        const val TU: String = "TU"
        const val WE: String = "WE"
        const val TH: String = "TH"
        const val FR: String = "FR"
        const val SA: String = "SA"
        const val SU: String = "SU"

        const val INTERVAL: String = "INTERVAL"

        /**
         * 解析 [rrule] 字符串，返回 [RecurRule] 对象
         */
        fun ofPattern(rrule: String?): RecurRule? {
            if (rrule == null) return null
            var freq = ""
            var interval = 1
            var byDays = emptySet<Int>()
            val pairs = rrule.split(";")
            for (pair in pairs) {
                val kv = pair.split("=")
                if (kv.size != 2) continue
                val k = kv[0]
                val v = kv[1]
                when (k) {
                    FREQ -> freq = v
                    INTERVAL -> interval = v.toInt()
                    BYDAY -> byDays = v.split(',').map { dayOfWeekStrToInt(it) }.toSet()
                }
            }
            // 构造RecurRule
            val recurRule = when (freq) {
                DAILY, MONTHLY, YEARLY -> RecurRule(freq, interval, null)
                WEEKLY -> RecurRule(freq, interval, byDays)
                else -> null
            }
            // 检查合法性
            if (recurRule != null && recurRule.isValid()) {
                return recurRule
            }
            return null
        }

        private fun dayOfWeekStrToInt(dayOfWeekStr: String): Int =
            when (dayOfWeekStr) {
                MO -> 1
                TU -> 2
                WE -> 3
                TH -> 4
                FR -> 5
                SA -> 6
                SU -> 7
                else -> -1
            }

        private fun dayOfWeekIntToStr(dayOfWeekInt: Int): String =
            when (dayOfWeekInt) {
                1 -> MO
                2 -> TU
                3 -> WE
                4 -> TH
                5 -> FR
                6 -> SA
                7 -> SU
                else -> "NA"
            }

        private fun dayOfWeekIntToReadableStr(dayOfWeekInt: Int, context: Context): String =
            when (dayOfWeekInt) {
                1 -> context.getString(R.string.all_mon)
                2 -> context.getString(R.string.all_tue)
                3 -> context.getString(R.string.all_wed)
                4 -> context.getString(R.string.all_thu)
                5 -> context.getString(R.string.all_fri)
                6 -> context.getString(R.string.all_sat)
                7 -> context.getString(R.string.all_sun)
                else -> "NA"
            }
    }
}
