package me.asrielyankare.gachaultils.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.asrielyankare.gachaultils.core.GameId
import me.asrielyankare.gachaultils.core.GachaResult
import me.asrielyankare.gachaultils.core.InstanceState
import me.asrielyankare.gachaultils.core.InstanceId
import me.asrielyankare.gachaultils.core.InstanceManager
import me.asrielyankare.gachaultils.core.InstanceStorage
import me.asrielyankare.gachaultils.core.ApkImporter
import me.asrielyankare.gachaultils.blackbox.NewBlackboxIntegration
import java.io.File

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

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application
    private val instanceManager = InstanceManager()
    private val apkImporter = ApkImporter(context)

    // Initialize BlackBox integration (uses NewBlackboxIntegration wrapper)
    private val blackBoxIntegration = NewBlackboxIntegration(context).also {
        it.initialize()
        it.registerImplementations()
    }

    // Initialize persistent storage
    init {
        InstanceStorage.init(context.filesDir)
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadInstances()
    }

    fun loadInstances() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val instances = instanceManager.listInstances()
            val selected = instances.firstOrNull { it.state == InstanceState.RUNNING }
                ?: instances.firstOrNull()
            _uiState.value = _uiState.value.copy(
                instances = instances,
                selectedInstance = selected,
                isLoading = false
            )
        }
    }

    fun selectInstance(instance: InstanceId) {
        _uiState.value = _uiState.value.copy(selectedInstance = instance)
    }

    /**
     * Handles APK file selected via SAF (Storage Access Framework).
     * Copies the APK to internal storage, then imports it.
     */
    fun handleApkSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // Copy APK to internal storage
                val apkDir = File(context.filesDir, "apks").apply { mkdirs() }
                val fileName = "imported_${System.currentTimeMillis()}.apk"
                val apkFile = File(apkDir, fileName)

                context.contentResolver.openInputStream(uri)?.use { input ->
                    apkFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: throw Exception("Failed to open APK file")

                // Get display name from content resolver
                val displayName = getDisplayNameFromUri(uri) ?: "New Instance"

                // Import the APK
                when (val result = apkImporter.importApk(apkFile.absolutePath, displayName, instanceManager)) {
                    is GachaResult.Success -> loadInstances()
                    is GachaResult.Failure -> _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.error.message
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to import APK: ${e.message}"
                )
            }
        }
    }

    /**
     * Gets display name from content URI.
     */
    private fun getDisplayNameFromUri(uri: Uri): String? {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) it.getString(nameIndex) else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun createInstance(
        packageName: String,
        gameId: GameId,
        displayName: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = instanceManager.createInstance(packageName, gameId, displayName)) {
                is GachaResult.Success -> loadInstances()
                is GachaResult.Failure -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.error.message
                )
            }
        }
    }

    fun launchInstance() {
        val instance = _uiState.value.selectedInstance ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(launcherState = LauncherState.Starting)
            when (val result = instanceManager.launchInstance(instance.id)) {
                is GachaResult.Success -> {
                    _uiState.value = _uiState.value.copy(launcherState = LauncherState.Running)
                    loadInstances()
                }
                is GachaResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        launcherState = LauncherState.Error,
                        error = result.error.message
                    )
                }
            }
        }
    }

    fun stopInstance() {
        val instance = _uiState.value.selectedInstance ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(launcherState = LauncherState.Stopped)
            when (val result = instanceManager.stopInstance(instance.id)) {
                is GachaResult.Success -> loadInstances()
                is GachaResult.Failure -> _uiState.value = _uiState.value.copy(
                    error = result.error.message
                )
            }
        }
    }

    fun importApk(apkPath: String, displayName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = apkImporter.importApk(apkPath, displayName, instanceManager)) {
                is GachaResult.Success -> loadInstances()
                is GachaResult.Failure -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.error.message
                )
            }
        }
    }

    fun deleteInstance(instanceId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = instanceManager.deleteInstance(instanceId)) {
                is GachaResult.Success -> loadInstances()
                is GachaResult.Failure -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.error.message
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
