package io.github.clfundev.better.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioAttributes.USAGE_ALARM
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log

private const val TAG = "VibrateUtils"

/**
 * 获取vibrator
 *
 * @param context Context
 * @return vibrator
 */
fun getVibrator(context: Context): Vibrator? {
    val vibrator = context.getSystemService(Vibrator::class.java)
    if (vibrator == null || !vibrator.hasVibrator()) {
        Log.w(TAG, "getVibrator failed: vibrator is unavailable")
        return null
    }
    return vibrator
}

/**
 * 按指定时间间隔振动
 */
@Suppress("DEPRECATION")
fun waveformVibrate(context: Context, timings: LongArray, repeat: Int = -1) {
    val vibrator = getVibrator(context) ?: return
    val vibe = VibrationEffect.createWaveform(timings, repeat)
    val attrs = AudioAttributes.Builder().setUsage(USAGE_ALARM).build() // 后台振动必须指定
    vibrator.vibrate(vibe, attrs)
}