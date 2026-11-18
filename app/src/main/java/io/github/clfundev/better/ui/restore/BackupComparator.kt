package io.github.clfundev.better.ui.restore

import androidx.recyclerview.widget.DiffUtil
import io.github.clfundev.better.data.network.model.Backup

class BackupComparator : DiffUtil.ItemCallback<Backup>() {
    override fun areItemsTheSame(oldItem: Backup, newItem: Backup): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Backup, newItem: Backup): Boolean {
        return oldItem == newItem
    }
}
