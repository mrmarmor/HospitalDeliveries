package com.appstairs.hospitaldeliveries.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.appstairs.hospitaldeliveries.domain.model.Location
import com.appstairs.hospitaldeliveries.domain.model.Task
import com.appstairs.hospitaldeliveries.domain.model.TaskStatus

@Entity(tableName = "tasks")
data class TaskDbEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    @ColumnInfo(name = "from_location") val from: Location,
    @ColumnInfo(name = "to_location") val to: Location,
    val status: TaskStatus,
    val createdAt: Long,
    val updatedAt: Long,
)

fun TaskDbEntity.toObject() = Task(id, title, description, from, to, status, createdAt, updatedAt)
fun Task.toDbEntity() = TaskDbEntity(id, title, description, from, to, status, createdAt, updatedAt)