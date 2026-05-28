package com.appstairs.hospitaldeliveries

import android.app.Application
import com.appstairs.hospitaldeliveries.data.local.AppDatabase
import com.appstairs.hospitaldeliveries.data.remote.MockRobot
import com.appstairs.hospitaldeliveries.data.repository.TaskRepositoryImpl
import com.appstairs.hospitaldeliveries.domain.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class HospitalDeliveriesApp : Application() {
    lateinit var repository: TaskRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default) // scope will stay alive even when 1 coroutine failed
        val database = AppDatabase.get(this)
        val mockRobot = MockRobot(appScope).also { it.onStart() }
        repository = TaskRepositoryImpl(
            dao = database.taskDao(),
            robot = mockRobot,
            appScope = appScope
        )
    }
}