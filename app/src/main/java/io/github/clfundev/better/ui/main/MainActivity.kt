package io.github.clfundev.better.ui.main

import android.Manifest
import android.app.AlarmManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI.setupWithNavController
import io.github.clfundev.better.R
import io.github.clfundev.better.data.db.entity.SpecPlan
import io.github.clfundev.better.databinding.ActivityMainBinding
import io.github.clfundev.better.service.FocusService
import io.github.clfundev.better.ui.BaseActivity

class MainActivity : BaseActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.i(TAG, "Focus service connected")
            val binder = service as FocusService.FocusServiceBinder
            val focusService = binder.getService()
            mainViewModel.setFocusService(focusService)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Log.i(TAG, "Focus service disconnected")
            mainViewModel.setFocusService(null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置底部导航栏，使其能跳转目的地
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navhost_main) as NavHostFragment
        val navController = navHostFragment.navController
        val navView = binding.navviewMain
        setupWithNavController(navView, navController)

        // BottomNavigationView内部做了系统栏避让，与BaseActivity的公共避让逻辑冲突
        navView.setOnApplyWindowInsetsListener(null)
        navView.setPadding(0, 0, 0, 0)

        // 请求通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0
            )
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, FocusService::class.java)
        // activity启动后初次绑定服务，flags传0是为了在服务未启动时不要创建服务
        bindService(intent, serviceConnection, 0)
    }

    override fun onStop() {
        super.onStop()
        // 解绑服务
        if (mainViewModel.focusServiceFlow.value != null) {
            unbindService(serviceConnection)
        }
    }

    /**
     * 开始专注
     */
    fun startFocus(specPlan: SpecPlan?) {
        // 检查是否有权限设置精确闹钟
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        if (!alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(
                this,
                "Please grant the alarm permission first",
                Toast.LENGTH_SHORT
            ).show()

            // 拉起精确闹钟授权页面
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            intent.setData("package:${packageName}".toUri())
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Log.e(TAG, "startActivity error", e)
            }
            return
        }

        val intent = Intent(this, FocusService::class.java)
        // 传入要专注的计划
        if (specPlan != null) {
            intent.putExtra(FocusService.EXTRA_PLAN_ID, specPlan.plan.id)
            intent.putExtra(FocusService.EXTRA_PLAN_START, specPlan.specPlanStart)
        }
        // 启动服务
        startService(intent)
        // 绑定服务，flags传0是为了在服务未启动时不要创建服务
        bindService(intent, serviceConnection, 0)
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}