package app.wirelessscanner.ui.screens.receivers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReceiversScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Receiver Hardware",
            style = MaterialTheme.typography.titleLarge
        )

        ReceiverCard(
            title = "Internal Device Radios",
            description = "Standard Android Bluetooth and Wi-Fi hardware adapters. Operates autonomously without external accessories.",
            status = "Ready (Milestone 1 Foundation)"
        )

        ReceiverCard(
            title = "External USB Receiver (Optional)",
            description = "Optional external hardware receiver (e.g. ESP32 CYD) connected over USB OTG. Extends observation capabilities while keeping external hardware completely optional.",
            status = "Scheduled for Milestone 5"
        )
    }
}

@Composable
private fun ReceiverCard(title: String, description: String, status: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Status: $status",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
