package io.github.clfundev.better.application

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import io.github.clfundev.better.constant.AppChannel

class BetterApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // 创建所有通知渠道
        AppChannel.entries.forEach { createNotificationChannel(it) }
        // 创建依赖容器
        appContainer = AppContainer(this)
    }

    /**
     * 重新创建依赖容器（用于释放旧数据库实例）
     */
    fun rebuildAppContainer() {
        appContainer = AppContainer(this)
    }

    private fun createNotificationChannel(appChannel: AppChannel) {
        val channel = NotificationChannel(
            appChannel.channelId, appChannel.channelName, appChannel.importance
        )
        val notificationMgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationMgr.createNotificationChannel(channel)
    }
}
