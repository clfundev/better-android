package io.github.clfundev.better.application

import android.content.Context
import io.github.clfundev.better.data.db.AppDatabase
import io.github.clfundev.better.data.network.retrofit.RetrofitClient
import io.github.clfundev.better.data.pref.AppPreferences
import io.github.clfundev.better.data.repository.FocusRecordRepository
import io.github.clfundev.better.data.repository.PlanRecordRepository
import io.github.clfundev.better.data.repository.PlanRepository

class AppContainer(context: Context) {
    private val appDatabase = AppDatabase.getInstance(context)

    val planRepository = PlanRepository(appDatabase.planDao())

    val planRecordRepository = PlanRecordRepository(appDatabase.planRecordDao())

    val focusRecordRepository = FocusRecordRepository(appDatabase.focusRecordDao())

    val appPreferences = AppPreferences(context)

    val retrofitClient = RetrofitClient.getInstance(appPreferences)
}