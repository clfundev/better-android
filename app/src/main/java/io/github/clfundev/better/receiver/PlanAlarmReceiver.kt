package io.github.clfundev.better.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.clfundev.better.R
import io.github.clfundev.better.constant.AppChannel
import io.github.clfundev.better.ui.main.MainActivity
import io.github.clfundev.better.utils.getAppContainer

class PlanAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val planIdsStr = intent.getStringExtra(EXTRA_PLAN_IDS)
        val triggerTime = intent.getLongExtra(EXTRA_TRIGGER_TIME, System.currentTimeMillis())
        val planRepo = context.getAppContainer().planRepository
        if (!planIdsStr.isNullOrEmpty()) {
            val planIds = planIdsStr.split(',').map { it.toLong() }
            val plans = planRepo.getPlansByIdsSync(planIds)
            if (plans.isEmpty()) return
            var curTime = System.currentTimeMillis()
            val ni = Intent(context, MainActivity::class.java)
            val npi = PendingIntent.getActivity(context, 0, ni, PendingIntent.FLAG_IMMUTABLE)
            val builder = NotificationCompat.Builder(context, AppChannel.PLAN.channelId)
                .setContentIntent(npi)
                .setGroup(GROUP_KEY_PLAN)
                .setAutoCancel(true)
                .setOnlyAlertOnce(false)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
            val nm = NotificationManagerCompat.from(context)
            if (plans.size > 1) {
                // 通知需进行分组，创建一个Group summary通知
                builder.setGroupSummary(true)
                nm.notify((curTime++ % 1000000000).toInt(), builder.build())
                builder.setGroupSummary(false)
            }
            for (plan in plans) {
                val content = plan.description.ifEmpty {
                    context.getString(R.string.planalarm_keep_moving)
                }
                builder.setContentTitle(plan.title).setContentText(content)
                nm.notify((curTime++ % 1000000000).toInt(), builder.build())
            }
        }
        // 安排下一次事件提醒
        setNextAlarm(context, triggerTime)
    }

    companion object {
        private const val TAG = "PlanAlarmReceiver"
        private const val GROUP_KEY_PLAN = "GROUP_KEY_PLAN"
        private const val REQUEST_CODE_PLAN_ALARM = 88
        private const val ACTION_PLAN_ALARM = "ACTION_PLAN_ALARM" // 注意与清单文件中一致
        private const val EXTRA_PLAN_IDS = "EXTRA_PLAN_IDS"
        private const val EXTRA_TRIGGER_TIME = "EXTRA_TRIGGER_TIME"

        /**
         * 取消计划闹钟
         *
         * @param context Context
         */
        fun cancelAlarm(context: Context) {
            val intent = Intent(context, PlanAlarmReceiver::class.java)
                .apply { setAction(ACTION_PLAN_ALARM) }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_PLAN_ALARM,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            )
            if (pendingIntent != null) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }

        /**
         * 判断指定时间之后下一次有闹钟计划，并为其设置闹钟
         *
         * @param context Context
         * @param millis 指定时间
         */
        fun setNextAlarm(context: Context, millis: Long) {
            val plans = context.getAppContainer().planRepository.getPlansHaveAlarmSync()
            val planIds = mutableListOf<Long>()
            var nextOccurMillis = 0L
            for (plan in plans) {
                val occurMillis = plan.occur.getNextOccur(millis)
                // 在指定时间之前的发生时间不合法，剔除
                if (occurMillis <= millis) continue
                if (nextOccurMillis <= millis) {
                    // 找到第一个合法发生时间
                    nextOccurMillis = occurMillis
                    planIds.add(plan.id)
                } else if (occurMillis < nextOccurMillis) {
                    // 比之前的最近发生时间更近
                    nextOccurMillis = occurMillis
                    planIds.clear()
                    planIds.add(plan.id)
                } else if (occurMillis == nextOccurMillis) {
                    // 出现发生时间相同的计划
                    planIds.add(plan.id)
                }
            }
            // 检查发生时间是不是在指定时间的未来，然后设置闹钟
            if (nextOccurMillis > millis) {
                setAlarm(context, planIds.joinToString(","), nextOccurMillis)
            }
        }

        private fun setAlarm(context: Context, planIds: String, triggerAtMillis: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e(TAG, "setAlarm error: cannot schedule exact alarm")
                return
            }
            val intent = Intent(context, PlanAlarmReceiver::class.java).apply {
                putExtra(EXTRA_PLAN_IDS, planIds)
                putExtra(EXTRA_TRIGGER_TIME, triggerAtMillis)
                setAction(ACTION_PLAN_ALARM)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_PLAN_ALARM,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
            )
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }
}