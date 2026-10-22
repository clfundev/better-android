package io.github.clfundev.better.service

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.ServiceInfo
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import io.github.clfundev.better.BuildConfig
import io.github.clfundev.better.R
import io.github.clfundev.better.application.AppContainer
import io.github.clfundev.better.constant.AppChannel
import io.github.clfundev.better.data.db.entity.FocusRecord
import io.github.clfundev.better.data.db.entity.Plan
import io.github.clfundev.better.data.db.entity.PlanRecord
import io.github.clfundev.better.data.db.entity.SpecPlan
import io.github.clfundev.better.data.pref.AppPreferences
import io.github.clfundev.better.occur.toLocalDateTime
import io.github.clfundev.better.ui.main.MainActivity
import io.github.clfundev.better.utils.FlashHelper
import io.github.clfundev.better.utils.getAppContainer
import io.github.clfundev.better.utils.waveformVibrate
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.NumberFormat

class FocusService : LifecycleService(), OnSharedPreferenceChangeListener {

    private lateinit var appContainer: AppContainer

    // 专注任务数据
    private var remainTime = 0L // 任务剩余时间
    private var nextStart = 0L // 任务结束时间(下一任务开始时间)
    private var isFocus = false // 任务是专注还是休息
    private var focusBegin = 0L // 专注开始时间
    private var planFocusBegin = 0L // 计划的专注开始时间
    private var specPlan: SpecPlan? = null // 专注计划

    // 专注任务相关设置
    private var focusDuration = 0L
    private var relaxDuration = 0L
    private var strictTime = false

    private val planIdFlow = MutableStateFlow(-1L)

    private val planStartFlow = MutableStateFlow(-1L)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val planFlow = planIdFlow.flatMapLatest { planId ->
        // 专注期间，用户可以编辑计划，因此需要监听数据库中的计划，不能仅用单次查询
        appContainer.planRepository.getPlanById(planId)
    }

    private val specPlanFlow = combine(planFlow, planStartFlow) { plan, planStart ->
        val isValidSpecPlan = plan != null && plan.goalType == Plan.GOAL_TYPE_FOCUS
                && plan.occur.isOccurDuring(planStart, planStart + 1)
        if (isValidSpecPlan) {
            SpecPlan(plan, planStart)
        } else {
            // 计划不合法，重置planId和planStart
            planIdFlow.value = -1L
            planStartFlow.value = -1L
            null
        }
    }.distinctUntilChanged().onEach { Log.i(TAG, "specPlan = $it") }

    private val timeFormat: NumberFormat = NumberFormat.getNumberInstance().apply {
        maximumIntegerDigits = 2
        minimumIntegerDigits = 2
    }

    private val timeObservers = mutableListOf<TimeObserver>()
    private val planObservers = mutableListOf<PlanObserver>()
    private var focusReceiver: FocusReceiver? = null
    private val binder = FocusServiceBinder()
    private val handler = Handler(Looper.getMainLooper())

    @SuppressLint("MissingPermission")
    private val runnable: Runnable = Runnable { // 计算进度、剩余时间
        val curTime = System.currentTimeMillis()
        remainTime = nextStart - curTime
        // 更新通知
        val notificationTitle = buildString {
            if (isFocus) append(getString(R.string.all_focusing)) else append(getString(R.string.all_relaxing))
            val planTitle = specPlan?.plan?.title
            if (!planTitle.isNullOrEmpty()) append(" ($planTitle)")
        }
        val notificationContent = buildString {
            append(timeFormat.format(remainTime / 60000))
            append(':')
            append(timeFormat.format(remainTime / 1000 % 60))
        }
        val notification = buildNotification(notificationTitle, notificationContent)
        NotificationManagerCompat.from(this@FocusService)
            .notify(NOTIFICATION_ID_FOCUS, notification)
        // 通知订阅者倒计时更新
        for (observer in timeObservers) {
            observer.onCountdown(remainTime)
        }
        // 为保证秒数显示稳定、不会跳数，计算下次刷新的延迟，控制每次在一秒的中间刷新
        val delayMillis = 1000 + (500 - (curTime % 1000))
        handler.postDelayed(runnable, delayMillis)
    }

    override fun onCreate() {
        super.onCreate()
        appContainer = getAppContainer()
        // 注册专注广播接收器
        focusReceiver = FocusReceiver()
        val filter = IntentFilter().apply { addAction(ACTION_FOCUS_REMINDER) }
        ContextCompat.registerReceiver(
            this,
            focusReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        // 初始化专注任务
        refreshFocusTask()
        // 设置为前台服务
        startForeground(
            NOTIFICATION_ID_FOCUS,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        )
        // 注册设置监听器
        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this)
        // 启动倒计时
        handler.post(runnable)
        // 记录专注任务开始时间
        focusBegin = System.currentTimeMillis()
        // 观察专注计划
        lifecycleScope.launch {
            specPlanFlow.collectLatest {
                // 保存原专注计划的完成记录，并刷新计划专注开始时间
                savePlanRecord()
                planFocusBegin = System.currentTimeMillis()
                specPlan = it
                // 通知观察者计划更新
                for (observer in planObservers) {
                    observer.onPlanChanged(it)
                }
            }
        }
        // 专注开始振动
        waveformVibrate(this, longArrayOf(0, 10))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        planIdFlow.value = intent?.getLongExtra(EXTRA_PLAN_ID, -1L) ?: -1L
        planStartFlow.value = intent?.getLongExtra(EXTRA_PLAN_START, -1L) ?: -1L
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onDestroy() {
        GlobalScope.launch {
            // 保存专注记录
            saveFocusRecord()
            // 保存计划完成记录
            savePlanRecord()
        }
        // 停止刷新任务
        handler.removeCallbacks(runnable)
        // 注销广播接收器
        unregisterReceiver(focusReceiver)
        focusReceiver = null
        // 移除设置监听
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this)
        // 取消定时任务
        cancelReminder()
        // 专注结束振动
        waveformVibrate(this, longArrayOf(0, 10))
        // 结束前台状态并移除通知
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    fun addTimeObserver(observer: TimeObserver) {
        timeObservers.add(observer)
        // 首次通知观察者
        observer.onStateChanged(
            remainTime,
            if (isFocus) focusDuration else relaxDuration,
            isFocus
        )
    }

    fun removeTimeObserver(observer: TimeObserver) {
        timeObservers.remove(observer)
    }

    fun addPlanObserver(observer: PlanObserver) {
        planObservers.add(observer)
        // 首次通知观察者
        observer.onPlanChanged(specPlan)
    }

    fun removePlanObserver(observer: PlanObserver) {
        planObservers.remove(observer)
    }

    /**
     * 读取用户设置的 [focusDuration] 和 [strictTime]，计算专注任务数据
     */
    private fun refreshFocusTask() {
        // 读取相关设置
        focusDuration = appContainer.appPreferences.focusDuration.also {
            relaxDuration = if (it < 1800000) 1800000 - it else 3600000 - it
        }
        strictTime = appContainer.appPreferences.strictTime

        if (BuildConfig.DEBUG) {
            focusDuration = 10000
            relaxDuration = 5000
            strictTime = false
        }

        // 计算专注任务数据
        val curTime = System.currentTimeMillis()
        if (strictTime) {
            // 计算现在到下次提醒剩余毫秒
            remainTime = 3600000 - (curTime % 3600000) // 距下一小时剩余毫秒
            if (focusDuration < 1800000) {
                // focusDuration < 1800000说明半小时为一个周期
                remainTime = if (remainTime > 1800000) remainTime - 1800000 else remainTime
            }
            if (remainTime > relaxDuration) {
                // 当前在专注时段
                remainTime -= relaxDuration
                nextStart = curTime + remainTime
                isFocus = true
            } else {
                // 当前在休息时段
                nextStart = curTime + remainTime
                isFocus = false
            }
        } else {
            // 不是严格时间模式
            remainTime = focusDuration
            nextStart = curTime + remainTime
            isFocus = true
        }

        // 设置提醒
        setupReminder(nextStart)

        // 通知观察者状态更新
        for (observer in timeObservers) {
            observer.onStateChanged(
                remainTime,
                if (isFocus) focusDuration else relaxDuration,
                isFocus
            )
        }
    }

    private suspend fun saveFocusRecord() {
        val focusEnd = System.currentTimeMillis()
        val duration = focusEnd - focusBegin
        if (duration < 300000) return // 专注小于5分钟，不记录
        appContainer.focusRecordRepository.insertFocusRecord(FocusRecord(focusBegin, focusEnd))
    }

    private suspend fun savePlanRecord() {
        specPlan?.let {
            val planId = it.plan.id
            val planStart = it.specPlanStart
            val planFocusEnd = System.currentTimeMillis()
            val duration = planFocusEnd - planFocusBegin
            if (duration < 300000) return // 专注小于5分钟，不记录
            val oldRecord = appContainer.planRecordRepository.getPlanRecord(planId, planStart)
            val newRecord = if (oldRecord != null) {
                // 在已有完成记录上增加专注时长
                PlanRecord(planId, planStart, oldRecord.completeAmount + duration)
            } else {
                PlanRecord(planId, planStart, duration)
            }
            appContainer.planRecordRepository.insertPlanRecord(newRecord)
        }
    }

    /**
     * 设置专注提醒定时任务
     * 
     * @param triggerAtMillis 任务触发时间(EpochMilli)
     */
    private fun setupReminder(triggerAtMillis: Long) {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        if (!alarmManager.canScheduleExactAlarms()) {
            Log.e(TAG, "setupReminder error: cannot schedule exact alarms")
            return
        }
        val intent = Intent(ACTION_FOCUS_REMINDER).setPackage(packageName)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            REQUEST_CODE_FOCUS_REMINDER,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
        Log.i(TAG, "Focus reminder set at ${triggerAtMillis.toLocalDateTime()}")
    }

    /**
     * 取消专注提醒定时任务
     */
    private fun cancelReminder() {
        val intent = Intent(ACTION_FOCUS_REMINDER).setPackage(packageName)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            REQUEST_CODE_FOCUS_REMINDER,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        if (pendingIntent != null) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun buildNotification(title: String = "", content: String = ""): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, AppChannel.FOCUS.channelId)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .build()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            AppPreferences.KEY_FOCUS_DURATION, AppPreferences.KEY_STRICT_TIME -> {
                refreshFocusTask()
            }
        }
    }

    // 用于接收AlarmManager定时广播
    private inner class FocusReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            Log.i(TAG, "Focus reminder triggered")
            if (isFocus) {
                // 专注结束，切换到休息
                remainTime = relaxDuration
                nextStart += relaxDuration
                isFocus = false
            } else {
                // 休息结束，切换到专注
                remainTime = focusDuration
                nextStart += focusDuration
                isFocus = true
            }
            // 通知订阅者专注状态切换
            for (observer in timeObservers) {
                observer.onStateChanged(
                    remainTime,
                    if (isFocus) focusDuration else relaxDuration,
                    isFocus
                )
            }
            // 发出提醒
            remind(context)
            // 设置下次专注提醒
            setupReminder(nextStart)
        }

        // 专注提醒
        private fun remind(context: Context) {
            val timings = if (isFocus) FOCUS_VIBRATE_FLASH_TIMINGS else RELAX_VIBRATE_FLASH_TIMINGS
            if (appContainer.appPreferences.sound) {
                val mediaPlayer = MediaPlayer.create(
                    context.applicationContext,
                    R.raw.focus_reminder_sound
                )
                mediaPlayer.setOnCompletionListener { it.release() }
                mediaPlayer.start()
            }
            if (appContainer.appPreferences.vibration) {
                waveformVibrate(context, timings)
            }
            if (appContainer.appPreferences.flashlight) {
                FlashHelper.flash(lifecycleScope, context, timings)
            }
        }
    }

    inner class FocusServiceBinder : Binder() {
        fun getService(): FocusService = this@FocusService
    }

    // TODO: 改用事件总线

    /**
     * 专注计时观察者
     */
    interface TimeObserver {
        fun onCountdown(remainTime: Long)

        fun onStateChanged(remainTime: Long, duration: Long, isFocus: Boolean)
    }

    /**
     * 专注计划观察者
     */
    interface PlanObserver {
        fun onPlanChanged(specPlan: SpecPlan?)
    }

    companion object {
        private const val TAG = "FocusService"

        // 用于接收专注计划的信息
        const val EXTRA_PLAN_ID = "EXTRA_PLAN_ID"
        const val EXTRA_PLAN_START = "EXTRA_PLAN_START"

        private const val NOTIFICATION_ID_FOCUS = 9
        private const val REQUEST_CODE_FOCUS_REMINDER = 99

        // 闪光、振动时间
        private val RELAX_VIBRATE_FLASH_TIMINGS = longArrayOf(0, 100, 100, 100, 100, 100, 100, 100)
        private val FOCUS_VIBRATE_FLASH_TIMINGS = longArrayOf(0, 800, 100, 800)

        private const val ACTION_FOCUS_REMINDER = "ACTION_FOCUS_REMINDER"
    }
}