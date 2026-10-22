package io.github.clfundev.better.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.clfundev.better.data.db.entity.PlanRecord

@Dao
interface PlanRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanRecord(planRecord: PlanRecord)

    @Query("DELETE FROM tb_plan_record WHERE plan_id = :planId")
    suspend fun deletePlanRecordsByPlanId(planId: Long)

    @Query("SELECT * FROM tb_plan_record WHERE plan_id = :planId AND plan_start = :planStart")
    suspend fun getPlanRecord(planId: Long, planStart: Long): PlanRecord?
}
