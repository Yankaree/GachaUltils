package me.asrielyankare.gachaultils.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.asrielyankare.gachaultils.core.InstanceId
import me.asrielyankare.gachaultils.core.InstanceState
import me.asrielyankare.gachaultils.ui.theme.PrimaryOrange
import me.asrielyankare.gachaultils.ui.theme.StateError
import me.asrielyankare.gachaultils.ui.theme.StateRunning
import me.asrielyankare.gachaultils.ui.theme.StateStarting
import me.asrielyankare.gachaultils.ui.theme.StateStopped

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstanceCard(
    instance: InstanceId,
    isSelected: Boolean,
    launcherState: String? = null,
    onClick: () -> Unit,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Gamepad,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (isSelected) PrimaryOrange else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = instance.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = instance.gameId.packageName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                val displayState = launcherState ?: instance.state.toDisplayString()
                Text(
                    text = displayState,
                    style = MaterialTheme.typography.labelMedium,
                    color = when (instance.state) {
                        InstanceState.RUNNING -> StateRunning
                        InstanceState.READY -> StateStopped
                        InstanceState.INSTALLING -> StateStarting
                        InstanceState.ERROR -> StateError
                        InstanceState.STOPPING -> StateStarting
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Launch",
                tint = PrimaryOrange,
                modifier = Modifier
                    .size(32.dp)
                    .padding(4.dp)
            )
        }
    }
}

private fun InstanceState.toDisplayString(): String {
    return when (this) {
        InstanceState.CREATED -> "Created"
        InstanceState.INSTALLING -> "Installing"
        InstanceState.READY -> "Ready"
        InstanceState.RUNNING -> "Running"
        InstanceState.STOPPING -> "Stopping"
        InstanceState.ERROR -> "Error"
    }
}
