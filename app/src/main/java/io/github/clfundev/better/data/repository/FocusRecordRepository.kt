package io.github.clfundev.better.data.repository

import io.github.clfundev.better.data.db.dao.FocusRecordDao
import io.github.clfundev.better.data.db.entity.FocusRecord
import kotlinx.coroutines.flow.Flow

class FocusRecordRepository(private val focusRecordDao: FocusRecordDao) {
    suspend fun insertFocusRecord(focusRecord: FocusRecord) =
        focusRecordDao.insertFocusRecord(focusRecord)

    suspend fun deleteAllFocusRecords() = focusRecordDao.deleteAllFocusRecords()

    fun getAllFocusRecords(): Flow<List<FocusRecord>> = focusRecordDao.getAllFocusRecords()
}
