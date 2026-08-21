package me.asrielyankare.gachaultils.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.asrielyankare.gachaultils.viewmodel.SettingsViewModel
import me.asrielyankare.gachaultils.viewmodel.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Launcher Section
            Text(
                text = "Launcher",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Appearance
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingItem(
                title = "Theme",
                subtitle = uiState.theme.name,
                onClick = { viewModel.updateTheme(
                    when (uiState.theme) {
                        ThemeMode.Light -> ThemeMode.Dark
                        ThemeMode.Dark -> ThemeMode.System
                        ThemeMode.System -> ThemeMode.Light
                    }
                ) }
            )

            SettingToggle(
                title = "Dynamic colors",
                checked = uiState.dynamicColors,
                onCheckedChange = { viewModel.updateDynamicColors(it) }
            )

            SettingToggle(
                title = "Animations",
                checked = uiState.animations,
                onCheckedChange = { viewModel.updateAnimations(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Launcher behavior
            Text(
                text = "Launcher behavior",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingToggle(
                title = "Confirm before launch",
                checked = uiState.confirmBeforeLaunch,
                onCheckedChange = { viewModel.updateConfirmBeforeLaunch(it) }
            )

            SettingToggle(
                title = "Auto backup",
                checked = uiState.autoBackup,
                onCheckedChange = { viewModel.updateAutoBackup(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Storage
            Text(
                text = "Storage",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingItem(
                title = "Instance location",
                subtitle = uiState.instanceLocation.ifEmpty { "Default" },
                onClick = { }
            )

            SettingItem(
                title = "Backup location",
                subtitle = uiState.backupLocation.ifEmpty { "Default" },
                onClick = { }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Advanced
            Text(
                text = "Advanced",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingItem(
                title = "Debug information",
                subtitle = "View debug logs",
                onClick = { }
            )

            SettingItem(
                title = "Logs",
                subtitle = "View application logs",
                onClick = { }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // About
            Text(
                text = "About",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            SettingItem(
                title = "GachaUltils version",
                subtitle = uiState.versionName,
                onClick = { }
            )

            SettingItem(
                title = "Open source licenses",
                subtitle = "View licenses",
                onClick = { }
            )
        }
    }
}

@Composable
private fun SettingItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingToggle(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
