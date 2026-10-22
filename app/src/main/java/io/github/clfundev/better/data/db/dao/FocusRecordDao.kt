package io.github.clfundev.better.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.github.clfundev.better.data.db.entity.FocusRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusRecordDao {
    @Insert
    suspend fun insertFocusRecord(focusRecord: FocusRecord)

    @Query("DELETE FROM tb_focus_record")
    suspend fun deleteAllFocusRecords()

    @Query("SELECT * FROM tb_focus_record")
    fun getAllFocusRecords(): Flow<List<FocusRecord>>
}