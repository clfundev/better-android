package io.github.clfundev.better.ui.backup

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import io.github.clfundev.better.R
import io.github.clfundev.better.application.BetterApplication
import io.github.clfundev.better.data.db.AppDatabase
import io.github.clfundev.better.databinding.ActivityBackupBinding
import io.github.clfundev.better.ui.BaseActivity
import io.github.clfundev.better.ui.main.MainActivity
import io.github.clfundev.better.utils.getAppContainer
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

class BackupActivity : BaseActivity() {
    private lateinit var binding: ActivityBackupBinding
    private val handler = Handler(Looper.getMainLooper())
    private val backRunnable = Runnable {
        // 销毁所有已打开的Activity，释放旧的数据库实例，回到首页
        (application as BetterApplication).rebuildAppContainer()
        val intent = Intent(this@BackupActivity, MainActivity::class.java).apply {
            setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBackupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 拦截返回操作，提示用户不要离开
        onBackPressedDispatcher.addCallback {
            Toast.makeText(
                this@BackupActivity,
                "DO NOT LEAVE until backup finished",
                Toast.LENGTH_SHORT
            ).show()
        }

        // 关闭数据库，使得db-wal、db-shm文件合并到db文件中，便于备份
        AppDatabase.closeDatabase()

        val dbFile = getDatabasePath(AppDatabase.DB_NAME)
        val requestBody = dbFile.asRequestBody("*/*".toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("file", dbFile.getName(), requestBody)
        val backupApi = getAppContainer().retrofitClient.backupApi
        lifecycleScope.launch {
            val response = backupApi.upload(filePart)
            if (response.isSuccessful()) {
                onBackupSucceeded()
            } else {
                onBackupFailed(response.message)
            }
        }
    }

    private fun onBackupSucceeded() {
        binding.tvBackup.text = getString(R.string.backup_succeeded)
        binding.pbBackup.visibility = View.GONE
        handler.postDelayed(backRunnable, 3000)
    }

    private fun onBackupFailed(errorMsg: String?) {
        binding.tvBackup.text = getString(R.string.backup_failed)
        binding.pbBackup.visibility = View.GONE
        // 展示错误信息
        binding.tvBackupErr.text = getString(R.string.backup_error, errorMsg)
        binding.tvBackupErr.visibility = View.VISIBLE
        handler.postDelayed(backRunnable, 3000)
    }
}