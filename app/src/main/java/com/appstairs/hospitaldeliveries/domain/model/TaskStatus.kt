package com.appstairs.hospitaldeliveries.domain.model

enum class TaskStatus {
    CREATED, IDLE, ASSIGNED, IN_PROGRESS, DONE, CANCELLED, FAILED;

    // this will indicates if user has a button for cancelling the task
    val isCancellable: Boolean get() = this == CREATED || this == IDLE || this == ASSIGNED

    // in production when moving between statuses, we should detect if allowed to do that
    /*fun canMoveTo(nextStatus: TaskStatus): Boolean = when (this) {
        CREATED -> nextStatus == IDLE || nextStatus == CANCELLED || nextStatus == FAILED || nextStatus == ASSIGNED
        IDLE -> nextStatus == ASSIGNED || nextStatus == CANCELLED || nextStatus == FAILED
        ASSIGNED -> nextStatus == IN_PROGRESS || nextStatus == CANCELLED || nextStatus == FAILED
        IN_PROGRESS -> nextStatus == DONE || nextStatus == FAILED || nextStatus == CANCELLED
        DONE, CANCELLED, FAILED -> false
    }*/
}