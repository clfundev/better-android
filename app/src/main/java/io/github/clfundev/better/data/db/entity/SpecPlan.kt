package io.github.clfundev.better.data.db.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 具体某一次发生的计划
 */
@Parcelize
data class SpecPlan(val plan: Plan, val specPlanStart: Long) : Parcelable