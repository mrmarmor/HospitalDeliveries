package com.appstairs.hospitaldeliveries.domain.model

data class Task(
    val id: String,
    val title: String,
    val description: String?,
    val from: Location, // enum that describes places in a hospital
    val to: Location,
    val status: TaskStatus,
    val createdAt: Long, // this is critical in multithread systems to prevent race conditions
    val updatedAt: Long, // this is also critical in multithread systems to prevent race conditions
)