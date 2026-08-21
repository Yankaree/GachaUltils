package me.asrielyankare.gachaultils.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.asrielyankare.gachaultils.core.GachaResult
import me.asrielyankare.gachaultils.core.InstanceId
import me.asrielyankare.gachaultils.core.InstanceStorage
import me.asrielyankare.gachaultils.core.SaveManager
import me.asrielyankare.gachaultils.core.SaveSnapshot

data class BackupUiState(
    val instances: List<InstanceId> = emptyList(),
    val selectedInstance: InstanceId? = null,
    val backups: Map<Int, List<SaveSnapshot>> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCreateDialog: Boolean = false,
    val operationStatus: String? = null
)

class BackupViewModel(application: Application) : AndroidViewModel(application) {
    private val saveManager = SaveManager()

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val instances = InstanceStorage.getAllInstances()
                val backups = mutableMapOf<Int, List<SaveSnapshot>>()
                instances.forEach { instance ->
                    when (val result = saveManager.detectSaves(instance.id, instance.packageName)) {
                        is GachaResult.Success -> backups[instance.id] = result.data
                        is GachaResult.Failure -> backups[instance.id] = emptyList()
                    }
                }
                val selected = _uiState.value.selectedInstance
                    ?: instances.firstOrNull()
                _uiState.value = _uiState.value.copy(
                    instances = instances,
                    selectedInstance = selected,
                    backups = backups,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load backups"
                )
            }
        }
    }

    fun selectInstance(instance: InstanceId) {
        _uiState.value = _uiState.value.copy(selectedInstance = instance)
        // Reload backups for selected instance
        viewModelScope.launch {
            val backups = mutableMapOf<Int, List<SaveSnapshot>>()
            when (val result = saveManager.detectSaves(instance.id, instance.packageName)) {
                is GachaResult.Success -> backups[instance.id] = result.data
                is GachaResult.Failure -> backups[instance.id] = emptyList()
            }
            _uiState.value = _uiState.value.copy(backups = backups)
        }
    }

    fun createBackup(snapshot: SaveSnapshot) {
        viewModelScope.launch {
            val instanceId = _uiState.value.selectedInstance?.id ?: return@launch
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                operationStatus = "Backing up ${snapshot.fileName}..."
            )
            when (val result = saveManager.backupSave(instanceId, snapshot)) {
                is GachaResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        showCreateDialog = false,
                        operationStatus = "Backup complete: ${snapshot.fileName}"
                    )
                    loadData()
                }
                is GachaResult.Failure -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.error.message,
                    operationStatus = null
                )
            }
        }
    }

    fun restoreBackup(snapshot: SaveSnapshot) {
        viewModelScope.launch {
            val instanceId = _uiState.value.selectedInstance?.id ?: return@launch
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                operationStatus = "Restoring ${snapshot.fileName}..."
            )
            when (val result = saveManager.restoreSave(instanceId, snapshot)) {
                is GachaResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        operationStatus = "Restore complete: ${snapshot.fileName}"
                    )
                    loadData()
                }
                is GachaResult.Failure -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.error.message,
                    operationStatus = null
                )
            }
        }
    }

    fun deleteBackup(snapshot: SaveSnapshot) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = saveManager.deleteBackup(snapshot)) {
                is GachaResult.Success -> loadData()
                is GachaResult.Failure -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.error.message
                )
            }
        }
    }

    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true)
    }

    fun hideCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearOperationStatus() {
        _uiState.value = _uiState.value.copy(operationStatus = null)
    }
}
