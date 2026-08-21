package me.asrielyankare.gachaultils

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import me.asrielyankare.gachaultils.ui.screens.backups.BackupScreen
import me.asrielyankare.gachaultils.ui.screens.home.HomeScreen
import me.asrielyankare.gachaultils.ui.screens.settings.SettingsScreen
import me.asrielyankare.gachaultils.ui.theme.GachaUltilsTheme
import me.asrielyankare.gachaultils.viewmodel.BackupViewModel
import me.asrielyankare.gachaultils.viewmodel.HomeViewModel
import me.asrielyankare.gachaultils.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GachaUltilsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GachaUltilsApp()
                }
            }
        }
    }
}

@Composable
fun GachaUltilsApp() {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = viewModel()
    val backupViewModel: BackupViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToBackup = { navController.navigate("backups") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("backups") {
            BackupScreen(
                viewModel = backupViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
