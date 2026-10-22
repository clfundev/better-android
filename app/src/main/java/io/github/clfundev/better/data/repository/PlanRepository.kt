package io.github.clfundev.better.data.repository

import android.content.Context
import io.github.clfundev.better.data.db.dao.PlanDao
import io.github.clfundev.better.data.db.entity.Plan
import io.github.clfundev.better.data.db.entity.PlanWithRecord
import io.github.clfundev.better.receiver.PlanAlarmReceiver
import kotlinx.coroutines.flow.Flow

class PlanRepository(
    private val planDao: PlanDao
) {
    suspend fun insertPlan(context: Context, plan: Plan) {
        planDao.insertPlan(plan)
        PlanAlarmReceiver.setNextAlarm(context, System.currentTimeMillis())
    }

    suspend fun updatePlan(context: Context, plan: Plan) {
        planDao.updatePlan(plan)
        PlanAlarmReceiver.setNextAlarm(context, System.currentTimeMillis())
    }

    suspend fun deletePlan(context: Context, plan: Plan) {
        planDao.deletePlan(plan)
        PlanAlarmReceiver.setNextAlarm(context, System.currentTimeMillis())
    }

    suspend fun deleteAllPlans(context: Context) {
        planDao.deleteAllPlans()
        PlanAlarmReceiver.setNextAlarm(context, System.currentTimeMillis())
    }

    fun getAllPlansWithRecords(): Flow<List<PlanWithRecord>> = planDao.getAllPlansWithRecords()

    fun getPlanById(id: Long): Flow<Plan?> = planDao.getPlanById(id)

    fun getPlansByGoalType(goalType: Int): Flow<List<Plan>> = planDao.getPlansByGoalType(goalType)

    suspend fun getPlansHaveAlarm(): List<Plan> = planDao.getPlansHaveAlarm()

    fun getPlansHaveAlarmSync(): List<Plan> = planDao.getPlansHaveAlarmSync()

    suspend fun getPlansByIds(ids: List<Long>): List<Plan> = planDao.getPlansByIds(ids)

    fun getPlansByIdsSync(ids: List<Long>): List<Plan> = planDao.getPlansByIdsSync(ids)
}
