package io.github.clfundev.better.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.clfundev.better.data.db.entity.Plan
import io.github.clfundev.better.data.db.entity.PlanWithRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {
    @Insert
    suspend fun insertPlan(plan: Plan)

    @Update
    suspend fun updatePlan(plan: Plan)

    @Delete
    suspend fun deletePlan(plan: Plan)

    @Query("DELETE FROM tb_plan")
    suspend fun deleteAllPlans()

    // 该方法需要Room运行两次查询，故添加@Transaction注解，以确保整个操作以原子方式执行
    @Transaction
    @Query("SELECT * FROM tb_plan")
    fun getAllPlansWithRecords(): Flow<List<PlanWithRecord>>

    @Query("SELECT * FROM tb_plan WHERE id = :id")
    fun getPlanById(id: Long): Flow<Plan?>

    @Query("SELECT * FROM tb_plan WHERE goal_type = :goalType")
    fun getPlansByGoalType(goalType: Int): Flow<List<Plan>>

    @Query("SELECT * FROM tb_plan WHERE has_alarm = 1")
    suspend fun getPlansHaveAlarm(): List<Plan>

    @Query("SELECT * FROM tb_plan WHERE has_alarm = 1")
    fun getPlansHaveAlarmSync(): List<Plan>

    @Query("SELECT * FROM tb_plan WHERE id IN (:ids)")
    suspend fun getPlansByIds(ids: List<Long>): List<Plan>

    @Query("SELECT * FROM tb_plan WHERE id IN (:ids)")
    fun getPlansByIdsSync(ids: List<Long>): List<Plan>
}