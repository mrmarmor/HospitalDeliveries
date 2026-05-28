package com.appstairs.hospitaldeliveries.domain.repository

import com.appstairs.hospitaldeliveries.data.remote.RobotConnectionState
import com.appstairs.hospitaldeliveries.domain.model.Location
import com.appstairs.hospitaldeliveries.domain.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface TaskRepository {
    fun observeTasks(): Flow<List<Task>>
    val connectionState: StateFlow<RobotConnectionState>

    /* Stored locally as task created, then sends to the robot system.
     * If the connection is off, the command is queued and sent on reconnect.*/
    suspend fun createTask(title: String, description: String?, from: Location, to: Location): Result<Task>

    suspend fun cancelTask(id: String): Result<Unit>
}