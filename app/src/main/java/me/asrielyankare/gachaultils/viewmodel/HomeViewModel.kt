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
import me.asrielyankare.gachaultils.blackbox.FallbackBlackBoxIntegration
import java.io.File

data class HomeUiState(
    val instances: List<InstanceId> = emptyList(),
    val selectedInstance: InstanceId? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val launcherState: LauncherState = LauncherState.Idle,
    // Reinstall dialog state
    val pendingReinstall: PendingReinstall? = null
)

data class PendingReinstall(
    val instanceId: Int,
    val instanceName: String,
    val packageName: String,
    val apkPath: String,
    val apkFileName: String
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

    // Lazy BlackBox integration — only initialized when first needed
    private var blackBoxInitialized = false
    private var usingFallback = false
    private fun ensureBlackBoxInitialized(): Boolean {
        if (blackBoxInitialized) return true
        return try {
            NewBlackboxIntegration(context).also {
                it.initialize()
                it.registerImplementations()
            }
            blackBoxInitialized = true
            true
        } catch (e: Throwable) {
            // Catch both Exception and Error (e.g. UnsatisfiedLinkError, NoClassDefFoundError)
            android.util.Log.e("HomeViewModel", "NewBlackbox init failed: ${e.javaClass.simpleName}: ${e.message}", e)
            // Fall back to stub — app can still create/manage instances
            android.util.Log.w("HomeViewModel", "Using fallback stub integration")
            try {
                FallbackBlackBoxIntegration(context).also {
                    it.initialize()
                    it.registerImplementations()
                }
                usingFallback = true
                blackBoxInitialized = true
                true
            } catch (e2: Throwable) {
                android.util.Log.e("HomeViewModel", "Fallback also failed: ${e2.message}", e2)
                false
            }
        }
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
     * If an instance with the same package name exists, shows reinstall dialog.
     */
    fun handleApkSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            if (!ensureBlackBoxInitialized()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "NewBlackbox engine failed to initialize. Check logcat for details."
                )
                return@launch
            }
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

                // Validate APK to get package name
                val profileResult = apkImporter.validateAndExtract(apkFile.absolutePath)
                when (profileResult) {
                    is GachaResult.Success -> {
                        val packageName = profileResult.data.gameId.packageName
                        // Check if instance with same package already exists
                        val existing = InstanceStorage.findByPackageName(packageName)
                        if (existing.isNotEmpty()) {
                            // Show reinstall dialog
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                pendingReinstall = PendingReinstall(
                                    instanceId = existing.first().id,
                                    instanceName = existing.first().displayName,
                                    packageName = packageName,
                                    apkPath = apkFile.absolutePath,
                                    apkFileName = displayName
                                )
                            )
                        } else {
                            // No existing instance — create new
                            doImport(apkFile.absolutePath, displayName)
                        }
                    }
                    is GachaResult.Failure -> _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = profileResult.error.message
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
     * User confirmed reinstall on existing instance.
     */
    fun confirmReinstall() {
        val pending = _uiState.value.pendingReinstall ?: return
        _uiState.value = _uiState.value.copy(pendingReinstall = null, isLoading = true)
        viewModelScope.launch {
            if (!ensureBlackBoxInitialized()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "NewBlackbox engine not available."
                )
                return@launch
            }
            try {
                when (val result = instanceManager.reinstallApk(pending.instanceId, pending.apkPath)) {
                    is GachaResult.Success -> loadInstances()
                    is GachaResult.Failure -> _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.error.message
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Reinstall failed: ${e.message}"
                )
            }
        }
    }

    /**
     * User chose to create a new instance instead of reinstalling.
     */
    fun dismissReinstall() {
        val pending = _uiState.value.pendingReinstall ?: return
        _uiState.value = _uiState.value.copy(pendingReinstall = null, isLoading = true)
        viewModelScope.launch {
            doImport(pending.apkPath, pending.apkFileName)
        }
    }

    fun cancelReinstall() {
        _uiState.value = _uiState.value.copy(pendingReinstall = null)
    }

    private fun doImport(apkPath: String, displayName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            if (!ensureBlackBoxInitialized()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "NewBlackbox engine not available."
                )
                return@launch
            }
            try {
                when (val result = apkImporter.importApk(apkPath, displayName, instanceManager)) {
                    is GachaResult.Success -> loadInstances()
                    is GachaResult.Failure -> _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.error.message
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Import failed: ${e.message}"
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
            if (!ensureBlackBoxInitialized()) {
                _uiState.value = _uiState.value.copy(
                    launcherState = LauncherState.Error,
                    error = "NewBlackbox not available"
                )
                return@launch
            }
            try {
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
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    launcherState = LauncherState.Error,
                    error = "Launch failed: ${e.message}"
                )
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
