package me.asrielyankare.gachaultils.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.asrielyankare.gachaultils.core.GameId
import me.asrielyankare.gachaultils.core.GameType
import me.asrielyankare.gachaultils.core.InstanceId
import me.asrielyankare.gachaultils.core.InstanceManager
import me.asrielyankare.gachaultils.core.InstanceStorage

data class HomeUiState(
    val instances: List<InstanceId> = emptyList(),
    val selectedInstance: InstanceId? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val launcherState: LauncherState = LauncherState.Idle
)

enum class LauncherState {
    Idle,
    Starting,
    Running,
    Stopped,
    Error
}

class HomeViewModel : ViewModel() {
    private val instanceManager = InstanceManager()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadInstances()
    }

    fun loadInstances() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val instances = instanceManager.listInstances()
                _uiState.value = _uiState.value.copy(
                    instances = instances,
                    selectedInstance = instances.firstOrNull(),
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load instances"
                )
            }
        }
    }

    fun selectInstance(instance: InstanceId) {
        _uiState.value = _uiState.value.copy(selectedInstance = instance)
    }

    fun createInstance(
        packageName: String,
        gameId: GameId,
        displayName: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val nextId = _uiState.value.instances.size
                instanceManager.createInstance(nextId, packageName, gameId, displayName)
                loadInstances()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to create instance"
                )
            }
        }
    }

    fun launchInstance(apkPath: String) {
        val instance = _uiState.value.selectedInstance ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(launcherState = LauncherState.Starting)
            try {
                val success = instanceManager.launchInstance(instance.id, apkPath)
                _uiState.value = _uiState.value.copy(
                    launcherState = if (success) LauncherState.Running else LauncherState.Error
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(launcherState = LauncherState.Error)
            }
        }
    }

    fun stopInstance() {
        val instance = _uiState.value.selectedInstance ?: return
        viewModelScope.launch {
            try {
                instanceManager.stopInstance(instance.id)
                _uiState.value = _uiState.value.copy(launcherState = LauncherState.Stopped)
                loadInstances()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to stop instance"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
