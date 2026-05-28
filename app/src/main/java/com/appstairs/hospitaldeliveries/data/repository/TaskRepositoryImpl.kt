package com.appstairs.hospitaldeliveries.data.repository

import com.appstairs.hospitaldeliveries.data.local.TaskDao
import com.appstairs.hospitaldeliveries.data.local.toObject
import com.appstairs.hospitaldeliveries.data.local.toDbEntity
import com.appstairs.hospitaldeliveries.data.remote.RobotCommand
import com.appstairs.hospitaldeliveries.data.remote.RobotConnectionState
import com.appstairs.hospitaldeliveries.data.remote.RobotEvent
import com.appstairs.hospitaldeliveries.data.remote.MockRobot
import com.appstairs.hospitaldeliveries.domain.model.Location
import com.appstairs.hospitaldeliveries.domain.model.Task
import com.appstairs.hospitaldeliveries.domain.model.TaskStatus
import com.appstairs.hospitaldeliveries.domain.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class TaskRepositoryImpl(
    private val dao: TaskDao,
    private val robot: MockRobot,
    appScope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
) : TaskRepository {

    override val connectionState: StateFlow<RobotConnectionState> = robot.state

    init {
        // This will handle all events from the robot + the DB is the single source of truth for the UI.
        appScope.launch {
            robot.events.collect { handleEvent(it) }
        }
    }

    override fun observeTasks(): Flow<List<Task>> =
        dao.getAllTasksFlow().map { list -> list.map { it.toObject() } }

    override suspend fun createTask(title: String, description: String?, from: Location, to: Location): Result<Task> = runCatching {
        require(title.isNotBlank()) { "Title is required" }
        require(from != to) { "From and To must be different" }

        val now = clock()
        val task = Task(
            id = idGenerator(),
            title = title,
            description = description?.trim()?.takeIf { it.isNotEmpty() },
            from = from,
            to = to,
            status = TaskStatus.CREATED,
            createdAt = now,
            updatedAt = now,
        )
        dao.insert(task.toDbEntity())
        robot.send(RobotCommand.CreateTask(task.id, from, to))
        task // return the created task. in exception(missing title/from), it will return task with failed state
    }

    override suspend fun cancelTask(id: String): Result<Unit> = runCatching {
        robot.send(RobotCommand.CancelTask(id))
    }

    private suspend fun handleEvent(event: RobotEvent) = when (event) {
        is RobotEvent.StatusUpdated -> dao.updateStatus(event.taskId, event.status, clock())
    }
}