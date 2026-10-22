package io.github.clfundev.better.data.repository

import io.github.clfundev.better.data.db.dao.PlanRecordDao
import io.github.clfundev.better.data.db.entity.PlanRecord

class PlanRecordRepository(private val planRecordDao: PlanRecordDao) {
    suspend fun insertPlanRecord(planRecord: PlanRecord) =
        planRecordDao.insertPlanRecord(planRecord)

    suspend fun deletePlanRecordsByPlanId(planId: Long) =
        planRecordDao.deletePlanRecordsByPlanId(planId)

    suspend fun getPlanRecord(planId: Long, planStart: Long): PlanRecord? =
        planRecordDao.getPlanRecord(planId, planStart)
}
