package me.asrielyankare.gachaultils.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val theme: ThemeMode = ThemeMode.System,
    val dynamicColors: Boolean = false,
    val animations: Boolean = true,
    val defaultInstanceId: Int? = null,
    val confirmBeforeLaunch: Boolean = true,
    val autoBackup: Boolean = false,
    val instanceLocation: String = "",
    val backupLocation: String = "",
    val versionName: String = "1.0"
)

enum class ThemeMode {
    Light, Dark, System
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("gacha_settings", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val themeMode = prefs.getString("theme", "system") ?: "system"
        val theme = when (themeMode) {
            "light" -> ThemeMode.Light
            "dark" -> ThemeMode.Dark
            else -> ThemeMode.System
        }

        _uiState.value = SettingsUiState(
            theme = theme,
            dynamicColors = prefs.getBoolean("dynamic_colors", false),
            animations = prefs.getBoolean("animations", true),
            defaultInstanceId = if (prefs.contains("default_instance_id")) {
                prefs.getInt("default_instance_id", 0)
            } else null,
            confirmBeforeLaunch = prefs.getBoolean("confirm_before_launch", true),
            autoBackup = prefs.getBoolean("auto_backup", false),
            instanceLocation = prefs.getString("instance_location", "") ?: "",
            backupLocation = prefs.getString("backup_location", "") ?: "",
            versionName = try {
                application.packageManager.getPackageInfo(application.packageName, 0).versionName ?: "1.0"
            } catch (e: Exception) { "1.0" }
        )
    }

    fun updateTheme(theme: ThemeMode) {
        viewModelScope.launch {
            prefs.edit().putString("theme", theme.name.lowercase()).apply()
            _uiState.value = _uiState.value.copy(theme = theme)
        }
    }

    fun updateDynamicColors(enabled: Boolean) {
        viewModelScope.launch {
            prefs.edit().putBoolean("dynamic_colors", enabled).apply()
            _uiState.value = _uiState.value.copy(dynamicColors = enabled)
        }
    }

    fun updateAnimations(enabled: Boolean) {
        viewModelScope.launch {
            prefs.edit().putBoolean("animations", enabled).apply()
            _uiState.value = _uiState.value.copy(animations = enabled)
        }
    }

    fun updateDefaultInstance(instanceId: Int?) {
        viewModelScope.launch {
            if (instanceId != null) {
                prefs.edit().putInt("default_instance_id", instanceId).apply()
            } else {
                prefs.edit().remove("default_instance_id").apply()
            }
            _uiState.value = _uiState.value.copy(defaultInstanceId = instanceId)
        }
    }

    fun updateConfirmBeforeLaunch(enabled: Boolean) {
        viewModelScope.launch {
            prefs.edit().putBoolean("confirm_before_launch", enabled).apply()
            _uiState.value = _uiState.value.copy(confirmBeforeLaunch = enabled)
        }
    }

    fun updateAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            prefs.edit().putBoolean("auto_backup", enabled).apply()
            _uiState.value = _uiState.value.copy(autoBackup = enabled)
        }
    }

    fun updateInstanceLocation(path: String) {
        viewModelScope.launch {
            prefs.edit().putString("instance_location", path).apply()
            _uiState.value = _uiState.value.copy(instanceLocation = path)
        }
    }

    fun updateBackupLocation(path: String) {
        viewModelScope.launch {
            prefs.edit().putString("backup_location", path).apply()
            _uiState.value = _uiState.value.copy(backupLocation = path)
        }
    }
}
