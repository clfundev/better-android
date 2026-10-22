package io.github.clfundev.better.ui.restore

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import io.github.clfundev.better.application.BetterApplication
import io.github.clfundev.better.data.db.AppDatabase
import io.github.clfundev.better.data.network.api.BackupApi
import io.github.clfundev.better.data.network.model.Backup
import io.github.clfundev.better.utils.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RestoreViewModel(private val backupApi: BackupApi) : ViewModel() {

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)

    private val _messages: MutableStateFlow<List<Message>> = MutableStateFlow(emptyList())

    val downloadState = _downloadState.asStateFlow()

    val messages = _messages.asStateFlow()

    val backupPagingFlow: Flow<PagingData<Backup>> = Pager(
        PagingConfig(pageSize = 10)
    ) {
        BackupPagingSource(backupApi)
    }.flow.cachedIn(viewModelScope)

    fun downloadBackup(backupId: String, context: Context) {
        when (_downloadState.value) {
            is DownloadState.Idle -> {
                // 执行下载
                download(backupId, context)
            }

            is DownloadState.Downloading -> {
                showMessage(Message(plainText = "Please wait for the download to complete"))
            }

            is DownloadState.Downloaded -> {
                val id = (_downloadState.value as DownloadState.Downloaded).backupId
                if (backupId == id) {
                    // 恢复备份
                    AppDatabase.restoreFromBackup(context)
                } else {
                    // 执行下载
                    download(backupId, context)
                }
            }

            else -> {}
        }
    }

    private fun download(backupId: String, context: Context) {
        _downloadState.value = DownloadState.Downloading(backupId, 0f)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resp = backupApi.downloadBackup(backupId)
                val body = resp.body()
                if (!resp.isSuccessful || body == null) {
                    showMessage(Message(plainText = "downloadBackup error: ${resp.message()}"))
                    _downloadState.value = DownloadState.Idle
                    return@launch
                }
                AppDatabase.saveResponseBodyAsBackup(body, context) { progress ->
                    _downloadState.value = DownloadState.Downloading(backupId, progress)
                }
                _downloadState.value = DownloadState.Downloaded(backupId)
            } catch (e: Exception) {
                showMessage(Message(plainText = "download error: ${e.message}"))
                _downloadState.value = DownloadState.Idle
            }
        }
    }

    private fun showMessage(message: Message) {
        _messages.update { currentMessages ->
            currentMessages + message
        }
    }

    /**
     * 暂时性展示的信息（比如Toast），展示过后UI应通知ViewModel信息已经展示，否则配置更改后信息会重复展示
     * (参考自https://developer.android.google.cn/topic/architecture/ui-layer/events#consuming-trigger-updates)
     */
    fun messageShown(message: Message) {
        _messages.update { currentMessages ->
            currentMessages.filterNot { it.id == message.id }
        }
    }

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val appContainer = (this[APPLICATION_KEY] as BetterApplication).appContainer
                RestoreViewModel(
                    backupApi = appContainer.retrofitClient.backupApi
                )
            }
        }
    }
}

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(val backupId: String, val progress: Float) : DownloadState
    data class Downloaded(val backupId: String) : DownloadState
    data object Restored : DownloadState
}