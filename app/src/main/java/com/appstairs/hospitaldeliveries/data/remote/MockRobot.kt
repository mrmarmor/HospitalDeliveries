package com.appstairs.hospitaldeliveries.data.remote

import com.appstairs.hospitaldeliveries.domain.model.Location
import com.appstairs.hospitaldeliveries.domain.model.TaskStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

// There is no real socket - "connect" is a delay and the "robot" is only a state
// This format of the Api matches what a WebSocket wrapper meet.
// replacing this with a real websocket communication involves only this file! :-)

class MockRobot(private val scope: CoroutineScope, private val random: Random = Random.Default) {
    private val _state = MutableStateFlow<RobotConnectionState>(RobotConnectionState.Connecting)
    val state: StateFlow<RobotConnectionState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<RobotEvent>() // for one-time task events with state mechanism
    val events: SharedFlow<RobotEvent> = _events.asSharedFlow()

    private val outbox = Channel<RobotCommand>(Channel.UNLIMITED)//queue of robot commands between coroutines
    //the 1st coroutine sends the command, but if websocket is off it won't done, so we need to let it move to another coroutine by this channel
    //when connection returns, all commands will be sent to the robot automatically(auto-retry)
    private val taskStates = mutableMapOf<String, TaskStatus>()
    private val taskJobs = mutableMapOf<String, Job>()//storing tasks, in order to manage cancelling/stopping

    fun onStart() {
        scope.launch { mockConnectWithRetry() }
        scope.launch { runTasks() }
    }

    suspend fun send(command: RobotCommand) = outbox.send(command)

    private suspend fun mockConnectWithRetry() {
        var attempts = 0
        while (scope.isActive) {
            if (attempts == 0) {
                _state.value = RobotConnectionState.Connecting
            } else {
                val delayMs = attempts * 2000L
                _state.value = RobotConnectionState.Reconnecting(attempts, delayMs)
                delay(delayMs)
            }
            //this is only a mockup. in a real app websocket should be implemented here, waiting for onOpen().
            //reconnection mechanism is also implemented here
            delay(500L)
            _state.value = RobotConnectionState.Connected

            // a real app should wait for websocket onClosed/onFailure callbacks before retrying(and attempts++)
            // this will keep the coroutine alive instead of onClosed/onFailure
            awaitCancellation()
        }
    }

    private suspend fun runTasks() {
        while (scope.isActive) {
            _state.first { it is RobotConnectionState.Connected }//this blocks until connected so commands won't missed while the socket is off
            val command = outbox.receive()
            when (command) {
                is RobotCommand.CreateTask -> startSimulation(command.taskId)
                is RobotCommand.CancelTask -> cancelSimulation(command.taskId)
            }
        }
    }

    // -- this is only a mock, in production get from server ---

    private fun startSimulation(taskId: String) {
        taskStates[taskId] = TaskStatus.CREATED
        taskJobs[taskId] = scope.launch {
            delay(2000)
            emit(taskId, TaskStatus.IDLE)
            delay(4000)
            emit(taskId, TaskStatus.ASSIGNED)
            delay(6000)
            emit(taskId, TaskStatus.IN_PROGRESS)
            delay(10000)
            val final = if (random.nextDouble() < FAIL_RATE) TaskStatus.FAILED else TaskStatus.DONE
            emit(taskId, final)
        }
    }

    private fun cancelSimulation(taskId: String) {
        taskJobs.remove(taskId)?.cancel() // remove coroutine from list + cancel it
        scope.launch { emit(taskId, TaskStatus.CANCELLED) }
    }

    private suspend fun emit(id: String, status: TaskStatus) {
        taskStates[id] = status
        _events.emit(RobotEvent.StatusUpdated(id, status))
    }

    private companion object {
        const val FAIL_RATE = 0.4
    }
}

sealed class RobotConnectionState {
    data object Connecting : RobotConnectionState() // 1st try
    data object Connected : RobotConnectionState()
    data class Reconnecting(val attempt: Int, val nextRetryInMs: Long) : RobotConnectionState()
}

sealed class RobotCommand {
    data class CreateTask( val taskId: String, val from: Location, val to: Location) : RobotCommand()
    data class CancelTask( val taskId: String) : RobotCommand()
}

sealed class RobotEvent {
    data class StatusUpdated(val taskId: String, val status: TaskStatus) : RobotEvent()
}