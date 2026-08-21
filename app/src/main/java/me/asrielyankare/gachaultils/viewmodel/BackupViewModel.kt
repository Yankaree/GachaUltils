package me.asrielyankare.gachaultils.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.asrielyankare.gachaultils.core.InstanceId
import me.asrielyankare.gachaultils.core.SaveManager
import me.asrielyankare.gachaultils.core.SaveSnapshot

data class BackupUiState(
    val instances: List<InstanceId> = emptyList(),
    val selectedInstance: InstanceId? = null,
    val backups: Map<Int, List<SaveSnapshot>> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCreateDialog: Boolean = false
)

class BackupViewModel : ViewModel() {
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
                val instances = me.asrielyankare.gachaultils.core.InstanceStorage.getAllInstances()
                val backups = mutableMapOf<Int, List<SaveSnapshot>>()
                instances.forEach { instance ->
                    val saves = saveManager.detectSaves(instance.id, instance.packageName)
                    backups[instance.id] = saves
                }
                _uiState.value = _uiState.value.copy(
                    instances = instances,
                    selectedInstance = instances.firstOrNull(),
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
    }

    fun createBackup(snapshot: SaveSnapshot) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val instanceId = _uiState.value.selectedInstance?.id ?: return@launch
                saveManager.backupSave(instanceId, snapshot)
                loadData()
                _uiState.value = _uiState.value.copy(showCreateDialog = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to create backup"
                )
            }
        }
    }

    fun restoreBackup(snapshot: SaveSnapshot) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val instanceId = _uiState.value.selectedInstance?.id ?: return@launch
                saveManager.restoreSave(instanceId, snapshot)
                loadData()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to restore backup"
                )
            }
        }
    }

    fun deleteBackup(snapshot: SaveSnapshot) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val backupFile = java.io.File(snapshot.getBackupPath())
                if (backupFile.exists()) {
                    backupFile.delete()
                }
                loadData()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to delete backup"
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
}
