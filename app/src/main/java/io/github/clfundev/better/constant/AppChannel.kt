package io.github.clfundev.better.constant

import android.app.NotificationManager

/**
 * All notification channels of this app
 */
enum class AppChannel(val channelId: String, val channelName: String, val importance: Int) {
    FOCUS(
        "focus",
        "Focus notifications",
        NotificationManager.IMPORTANCE_DEFAULT
    ),
    PLAN(
        "plan",
        "Plan notifications",
        NotificationManager.IMPORTANCE_HIGH
    )
}
