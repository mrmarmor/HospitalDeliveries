package com.appstairs.hospitaldeliveries.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.appstairs.hospitaldeliveries.data.remote.RobotConnectionState
import com.appstairs.hospitaldeliveries.domain.model.Location
import com.appstairs.hospitaldeliveries.domain.model.Task
import com.appstairs.hospitaldeliveries.domain.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: TaskRepository) : ViewModel() {

    private val form = MutableStateFlow(FormState()) // the form that user fill

    val state: StateFlow<HomeUiState> = combine(
        repository.observeTasks(),
        repository.connectionState,
        form,
    ) { tasks, conn, form ->
        HomeUiState(
            tasks = tasks,
            connection = conn,
            title = form.title,
            description = form.description,
            from = form.from,
            to = form.to,
            submitting = form.submitting,
            error = form.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), HomeUiState())

    fun onTitleChange(value: String) = form.update { it.copy(title = value, error = null) }
    fun onDescriptionChange(value: String) = form.update { it.copy(description = value) }
    fun onFromChange(value: Location) = form.update { it.copy(from = value, error = null) }
    fun onToChange(value: Location) = form.update { it.copy(to = value, error = null) }
    fun dismissError() = form.update { it.copy(error = null) }

    fun submit() {
        val current = form.value
        if (current.submitting || current.title.isBlank() || current.from == current.to) return
        form.update { it.copy(submitting = true, error = null) }

        viewModelScope.launch {
            repository.createTask(
                title = current.title,
                description = current.description.takeIf { it.isNotBlank() },
                from = current.from,
                to = current.to,
            ).fold(
                onSuccess = { form.value = FormState() },
                onFailure = { e ->
                    form.update {
                        it.copy(submitting = false, error = e.message ?: "Failed to create task")
                    }
                },
            )
        }
    }

    fun cancel(id: String) {
        viewModelScope.launch {
            repository.cancelTask(id).onFailure { e ->
                form.update { it.copy(error = e.message ?: "Failed to cancel task") }
            }
        }
    }

    private data class FormState(
        val title: String = "",
        val description: String = "",
        val from: Location = Location.PHARMACY,
        val to: Location = Location.EMERGENCY,
        val submitting: Boolean = false,
        val error: String? = null,
    )

    // this factory should be replaced in production with a DI framework like Hilt
    class Factory(private val repository: TaskRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(repository) as T
    }
}

data class HomeUiState(
    val tasks: List<Task> = emptyList(),
    val connection: RobotConnectionState = RobotConnectionState.Connecting,
    val title: String = "",
    val description: String = "",
    val from: Location = Location.PHARMACY,
    val to: Location = Location.EMERGENCY,
    val submitting: Boolean = false,
    val error: String? = null,
) {
    val canSubmit: Boolean get() = !submitting && title.isNotBlank() && from != to
}