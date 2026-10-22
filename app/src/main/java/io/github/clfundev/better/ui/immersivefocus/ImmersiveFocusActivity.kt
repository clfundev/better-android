package io.github.clfundev.better.ui.immersivefocus

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.IntDef
import io.github.clfundev.better.R
import io.github.clfundev.better.data.db.entity.SpecPlan
import io.github.clfundev.better.databinding.ActivityImmersiveFocusBinding
import io.github.clfundev.better.service.FocusService
import io.github.clfundev.better.service.FocusService.PlanObserver
import io.github.clfundev.better.service.FocusService.TimeObserver
import io.github.clfundev.better.ui.BaseActivity
import io.github.clfundev.better.utils.getAppContainer
import java.text.NumberFormat

class ImmersiveFocusActivity : BaseActivity(), TimeObserver, PlanObserver {
    @DisplayMode
    private var displayMode: Int = SHOW_ALL

    private lateinit var timeTv: TextView
    private lateinit var stateTv: TextView
    private lateinit var planTv: TextView
    private lateinit var exitIv: ImageView

    private val numberFormat: NumberFormat = NumberFormat.getNumberInstance().apply {
        maximumIntegerDigits = 2
        minimumIntegerDigits = 2
    }

    private var focusService: FocusService? = null
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as FocusService.FocusServiceBinder
            focusService = binder.getService().apply {
                addTimeObserver(this@ImmersiveFocusActivity)
                addPlanObserver(this@ImmersiveFocusActivity)
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            focusService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityImmersiveFocusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        timeTv = binding.tvImmersivefocusTime
        stateTv = binding.tvImmersivefocusState
        planTv = binding.tvImmersivefocusPlan
        exitIv = binding.ivImmersivefocusExit

        exitIv.setOnClickListener { finish() }
        binding.root.setOnClickListener { v ->
            // 切换显示模式，并轻振反馈
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            alterDisplayMode()
        }

        // 设置屏幕常亮
        if (getAppContainer().appPreferences.keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onStart() {
        super.onStart()
        // 绑定服务
        val intent = Intent(this, FocusService::class.java)
        bindService(intent, serviceConnection, 0)
    }

    override fun onStop() {
        super.onStop()
        // 解绑服务
        focusService?.let {
            it.removeTimeObserver(this)
            it.removePlanObserver(this)
            unbindService(serviceConnection)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // 进入粘性沉浸模式
            hideSystemUI()
        }
    }

    override fun onCountdown(remainTime: Long) {
        val remainTimeStr = (numberFormat.format(remainTime / 60000) + ":"
                + numberFormat.format(remainTime / 1000 % 60))
        timeTv.text = remainTimeStr
    }

    override fun onStateChanged(remainTime: Long, duration: Long, isFocus: Boolean) {
        val remainTimeStr = (numberFormat.format(remainTime / 60000) + ":"
                + numberFormat.format(remainTime / 1000 % 60))
        timeTv.text = remainTimeStr
        stateTv.text = if (isFocus)
            getString(R.string.all_focusing)
        else
            getString(R.string.all_relaxing)
    }

    override fun onPlanChanged(specPlan: SpecPlan?) {
        planTv.text = specPlan?.plan?.title ?: ""
    }

    /**
     * 启用全屏模式(粘性沉浸模式)
     */
    @Suppress("DEPRECATION")
    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    private fun alterDisplayMode() {
        when (displayMode) {
            SHOW_ALL -> {
                displayMode = SHOW_TIME
                fadeOutView(stateTv)
                fadeOutView(planTv)
                fadeOutView(exitIv)
            }

            SHOW_TIME -> {
                displayMode = SHOW_ALL
                fadeInView(stateTv)
                fadeInView(planTv)
                fadeInView(exitIv)
            }
        }
    }

    // 淡入
    private fun fadeInView(v: View) {
        v.animate()
            .alpha(1f)
            .setDuration(((1f - v.alpha) * ANIM_DURATION).toLong())
            .setListener(null)
        // 发起淡入动画会立即取消未结束的淡出动画并触发它的onAnimationEnd回调，
        // 因此下面这行必须放在淡入动画发起之后
        v.visibility = View.VISIBLE
    }

    // 淡出
    private fun fadeOutView(v: View) {
        v.animate()
            .alpha(0f)
            .setDuration((v.alpha * ANIM_DURATION).toLong())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    v.visibility = View.INVISIBLE
                }
            })
    }

    companion object {
        private const val ANIM_DURATION = 300

        private const val SHOW_ALL = 0
        private const val SHOW_TIME = 1
    }

    @IntDef(SHOW_ALL, SHOW_TIME)
    @Retention(AnnotationRetention.SOURCE)
    annotation class DisplayMode
}