package io.github.clfundev.better.utils

import android.content.Context
import android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE
import android.hardware.camera2.CameraManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object FlashHelper {

    private var job: Job? = null

    /**
     * 按指定时间间隔，交替开关闪光灯
     * 例：调用后经过timings[0]开启闪光灯，又经过timings[1]关闭闪光灯，按此规律直至timings遍历完，最终保证闪光灯关闭
     */
    fun flash(scope: CoroutineScope, context: Context, timings: LongArray) {
        job?.cancel()
        job = scope.launch {
            val cameraMgr = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            // 获取支持闪光灯的摄像头
            val flashCameraId = cameraMgr.cameraIdList.find {
                cameraMgr.getCameraCharacteristics(it).get(FLASH_INFO_AVAILABLE) ?: false
            }
            if (flashCameraId == null) return@launch
            try {
                // 闪光灯按时间间隔开始闪烁
                var enabled = false
                for (timing in timings) {
                    cameraMgr.setTorchMode(flashCameraId, enabled)
                    delay(timing)
                    enabled = !enabled
                }
            } finally {
                // 确保最后闪光灯关闭
                cameraMgr.setTorchMode(flashCameraId, false)
            }
        }
    }

    /**
     * 结束闪光
     */
    fun cancel() {
        job?.cancel()
    }
}