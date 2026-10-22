package io.github.clfundev.better.occur

class OccurFactory {
    companion object {
        fun createOccur(start: Long, recurRule: RecurRule?): Occur {
            return when (recurRule?.freq) {
                RecurRule.DAILY -> DailyRecur(start, recurRule.interval)
                RecurRule.WEEKLY -> WeeklyRecur(
                    start,
                    recurRule.interval,
                    recurRule.byDays?.sorted() ?: emptyList()
                )

                RecurRule.MONTHLY -> MonthlyRecur(start, recurRule.interval)
                RecurRule.YEARLY -> YearlyRecur(start, recurRule.interval)
                else -> OnceOccur(start)
            }
        }
    }
}
