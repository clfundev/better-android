package io.github.clfundev.better.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            // 启动计划提醒
            PlanAlarmReceiver.setNextAlarm(context, System.currentTimeMillis())
        }
    }
}