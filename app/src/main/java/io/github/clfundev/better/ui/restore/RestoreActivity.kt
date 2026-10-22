package io.github.clfundev.better.ui.restore

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.clfundev.better.R
import io.github.clfundev.better.databinding.ActivityRestoreBinding
import io.github.clfundev.better.ui.BaseActivity
import io.github.clfundev.better.ui.main.MainActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RestoreActivity : BaseActivity() {

    private lateinit var binding: ActivityRestoreBinding
    private val vm: RestoreViewModel by viewModels { RestoreViewModel.factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRestoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivRestoreBack.setOnClickListener { finish() }

        val pagingAdapter = BackupPagingAdapter(this, BackupComparator()) {
            vm.downloadBackup(it.backupId, this)
        }

        val linearLayoutManager = LinearLayoutManager(this)
        val decor = DividerItemDecoration(this, linearLayoutManager.orientation)
        binding.rvRestoreBackups.apply {
            layoutManager = linearLayoutManager
            adapter = pagingAdapter
            addItemDecoration(decor)
            itemAnimator = null
        }

        lifecycleScope.launch {
            vm.downloadState.collectLatest { downloadState ->
                when (downloadState) {
                    is DownloadState.Downloaded, is DownloadState.Downloading, is DownloadState.Idle -> {
                        pagingAdapter.updateDownloadState(downloadState)
                    }

                    is DownloadState.Restored -> {
                        showMsg(getString(R.string.restore_succeeded))
                        // 回到主界面
                        val intent = Intent(this@RestoreActivity, MainActivity::class.java).apply {
                            // 使本应用中所有已打开的Activity关闭，防止旧的数据库实例被引用
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(intent)
                    }
                }
            }
        }
        lifecycleScope.launch {
            vm.messages.collectLatest { messages ->
                messages.firstOrNull()?.let {
                    it.showAsShortToast(this@RestoreActivity)
                    vm.messageShown(it)
                }
            }
        }
        lifecycleScope.launch {
            vm.backupPagingFlow.collectLatest { pagingData ->
                pagingAdapter.submitData(pagingData)
            }
        }
        lifecycleScope.launch {
            pagingAdapter.loadStateFlow.collectLatest { loadStates ->
                val refreshState = loadStates.refresh
                val appendState = loadStates.append
                val prependState = loadStates.prepend
                // 如果正在加载，显示进度条
                binding.lpiRestoreLoadstate.isVisible = refreshState is LoadState.Loading
                        || prependState is LoadState.Loading
                        || appendState is LoadState.Loading
                // 如果出现错误，发出Toast提示
                if (refreshState is LoadState.Error) {
                    showMsg(refreshState.error.message)
                }
                if (appendState is LoadState.Error) {
                    showMsg(appendState.error.message)
                }
                if (prependState is LoadState.Error) {
                    showMsg(prependState.error.message)
                }
            }
        }
    }

    private fun showMsg(msg: String?) {
        if (msg != null) {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "RestoreActivity"
    }
}