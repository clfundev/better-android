package io.github.clfundev.better.ui.restore

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.github.clfundev.better.R
import io.github.clfundev.better.data.network.model.Backup
import io.github.clfundev.better.databinding.ItemBackupBinding
import io.github.clfundev.better.ui.restore.BackupPagingAdapter.BackupViewHolder
import io.github.clfundev.better.widget.DownloadButton
import java.time.format.DateTimeFormatter

class BackupPagingAdapter(
    private val context: Context,
    diffCallback: DiffUtil.ItemCallback<Backup>,
    private val onItemClick: (Backup) -> Unit
) : PagingDataAdapter<Backup, BackupViewHolder>(diffCallback) {

    private var downloadState: DownloadState = DownloadState.Idle
    private var lastDownloadState: DownloadState = DownloadState.Idle

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BackupViewHolder {
        val binding = ItemBackupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BackupViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: BackupViewHolder, position: Int) {
        val backup = getItem(position)
        if (backup != null) {
            val pattern = context.getString(R.string.all_date_hour_minute_neat_format)
            val formatter = DateTimeFormatter.ofPattern(pattern)
            holder.timeTv.text = backup.createTime.format(formatter)
            holder.sizeTv.text = formatFileSize(backup.size)
            when (val state = downloadState) {
                is DownloadState.Downloading if state.backupId == backup.backupId -> {
                    holder.downloadBtn.setProgress(state.progress)
                }

                is DownloadState.Downloaded if state.backupId == backup.backupId -> {
                    holder.downloadBtn.setState(DownloadButton.FINISHED)
                }

                else -> {
                    holder.downloadBtn.setState(DownloadButton.IDLE)
                }
            }
            holder.downloadBtn.setOnClickListener { onItemClick(backup) }
        } else {
            holder.timeTv.text = "null"
            holder.sizeTv.text = "null"
            holder.downloadBtn.setState(DownloadButton.IDLE)
            holder.downloadBtn.setOnClickListener(null)
        }
    }

    fun updateDownloadState(downloadState: DownloadState) {
        this.downloadState = downloadState
        val items = snapshot().items
        arrayOf(downloadState, lastDownloadState).forEach { state ->
            val changedId = when (state) {
                is DownloadState.Downloading -> state.backupId
                is DownloadState.Downloaded -> state.backupId
                else -> null
            }
            if (changedId != null) {
                val index = items.indexOfFirst { it.backupId == changedId }
                if (index in items.indices) {
                    notifyItemChanged(index)
                }
            }
        }
        lastDownloadState = downloadState
    }

    /**
     * 文件大小单位转换
     * 
     * @param fileSizeInBytes 文件大小（字节）
     * @return 合适的文件大小表示字符串
     */
    private fun formatFileSize(fileSizeInBytes: Long): String {
        var size = fileSizeInBytes
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var unitIndex = 0
        while (size / 1024 > 0 && unitIndex + 1 < units.size) {
            size /= 1024
            unitIndex++
        }
        return "$size ${units[unitIndex]}"
    }

    class BackupViewHolder(
        binding: ItemBackupBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        val timeTv: TextView = binding.tvRvbackupitemTime
        val sizeTv: TextView = binding.tvRvbackupitemSize
        val downloadBtn: DownloadButton = binding.downloadbtnRvbackupitem
    }
}
