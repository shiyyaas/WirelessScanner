package app.wirelessscanner.ui.screens.observations

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.wirelessscanner.ble.model.DiscoveredBleDevice
import app.wirelessscanner.ble.permission.BlePermissionManager
import app.wirelessscanner.core.model.SignalCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ObservationsScreen(
    modifier: Modifier = Modifier,
    viewModel: ObservationsViewModel = run {
        val context = LocalContext.current
        viewModel(factory = ObservationsViewModel.provideFactory(context))
    }
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionManager = BlePermissionManager(context)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        viewModel.onPermissionResult(allGranted)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- 1. Top Scan Session Control Bar ---
        ScanControlHeader(
            uiState = uiState,
            onStartScan = {
                if (!permissionManager.hasScanPermission()) {
                    permissionLauncher.launch(permissionManager.requiredPermissions.toTypedArray())
                } else {
                    viewModel.startScan()
                }
            },
            onStopScan = { viewModel.stopScan() },
            onClear = { viewModel.clearObservations() }
        )

        // --- 2. Permission / Hardware Alert Banners ---
        when (uiState.scanUiStatus) {
            is ScanUiStatus.PermissionDenied -> {
                PermissionBanner(
                    rationale = permissionManager.rationaleDescription,
                    onRequestPermission = {
                        permissionLauncher.launch(permissionManager.requiredPermissions.toTypedArray())
                    },
                    onOpenSettings = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    }
                )
            }
            is ScanUiStatus.BluetoothDisabled -> {
                BluetoothDisabledBanner()
            }
            is ScanUiStatus.BluetoothUnsupported -> {
                AlertBanner(
                    title = "BLE Unsupported",
                    message = "This device does not have Bluetooth Low Energy hardware."
                )
            }
            is ScanUiStatus.Error -> {
                AlertBanner(
                    title = "Scan Error",
                    message = (uiState.scanUiStatus as ScanUiStatus.Error).message
                )
            }
            else -> {}
        }

        // --- 3. Filter and Search Bar ---
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search by advertised name or address...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true
        )

        // Filter and Sort Chips
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = uiState.filterMode == NameFilterMode.ALL,
                onClick = { viewModel.setFilterMode(NameFilterMode.ALL) },
                label = { Text("All (${uiState.uniqueDeviceCount})") }
            )
            FilterChip(
                selected = uiState.filterMode == NameFilterMode.NAMED_ONLY,
                onClick = { viewModel.setFilterMode(NameFilterMode.NAMED_ONLY) },
                label = { Text("Named Only") }
            )
            FilterChip(
                selected = uiState.sortOrder == ObservationSortOrder.STRONGEST_SIGNAL,
                onClick = { viewModel.setSortOrder(ObservationSortOrder.STRONGEST_SIGNAL) },
                label = { Text("Signal") }
            )
            FilterChip(
                selected = uiState.sortOrder == ObservationSortOrder.MOST_RECENT,
                onClick = { viewModel.setSortOrder(ObservationSortOrder.MOST_RECENT) },
                label = { Text("Recent") }
            )
            FilterChip(
                selected = uiState.sortOrder == ObservationSortOrder.HIGHEST_COUNT,
                onClick = { viewModel.setSortOrder(ObservationSortOrder.HIGHEST_COUNT) },
                label = { Text("Count") }
            )
        }

        // --- 4. Discovered Device List / Empty State ---
        if (uiState.displayedDevices.isEmpty()) {
            EmptyStateView(
                isScanning = uiState.scanUiStatus == ScanUiStatus.Scanning,
                hasFilter = uiState.searchQuery.isNotEmpty() || uiState.filterMode != NameFilterMode.ALL
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = uiState.displayedDevices,
                    key = { it.rawAddress }
                ) { device ->
                    DeviceCard(
                        device = device,
                        isExpanded = uiState.selectedDeviceAddress == device.rawAddress,
                        onToggleExpand = { viewModel.selectDevice(device.rawAddress) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanControlHeader(
    uiState: ObservationsUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (uiState.scanUiStatus == ScanUiStatus.Scanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Scanning BLE... (${uiState.secondsRemaining}s)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "BLE Discovery Idle",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "${uiState.uniqueDeviceCount} devices · ${uiState.totalObservationsCount} packets",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (uiState.scanUiStatus == ScanUiStatus.Scanning) {
                        Button(
                            onClick = onStopScan,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop")
                            Spacer(Modifier.width(4.dp))
                            Text("Stop")
                        }
                    } else {
                        Button(onClick = onStartScan) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Scan")
                            Spacer(Modifier.width(4.dp))
                            Text("Scan (15s)")
                        }
                    }

                    if (uiState.uniqueDeviceCount > 0 && uiState.scanUiStatus != ScanUiStatus.Scanning) {
                        IconButton(onClick = onClear) {
                            Icon(Icons.Default.Refresh, contentDescription = "Clear")
                        }
                    }
                }
            }

            if (uiState.scanUiStatus == ScanUiStatus.Scanning) {
                LinearProgressIndicator(
                    progress = {
                        if (uiState.secondsRemaining > 0) {
                            uiState.secondsRemaining / 15f
                        } else 0f
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun DeviceCard(
    device: DiscoveredBleDevice,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.displayName ?: "Unnamed BLE Device",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (device.displayName != null) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (device.displayName != null) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = device.rawAddress,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Seen ${timeFormat.format(Date(device.lastSeenEpochMillis))} · ${device.observationCount} observations",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RssiBadge(rssiDbm = device.latestRssi.dbm, category = device.latestRssi.category)
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider()

                    Text(
                        text = "Advertisement Evidence",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    DetailRow(label = "First Observed", value = timeFormat.format(Date(device.firstSeenEpochMillis)))
                    DetailRow(label = "Last Observed", value = timeFormat.format(Date(device.lastSeenEpochMillis)))
                    DetailRow(label = "Median RSSI", value = "${device.medianRssi} dBm (relative)")
                    DetailRow(label = "RSSI History (last ${device.rssiHistory.size})", value = device.rssiHistory.joinToString(", ") { "$it" })

                    device.advertisedName?.let { DetailRow(label = "Advertised Name", value = it) }
                    device.cachedSystemName?.let { DetailRow(label = "Cached System Name", value = it) }

                    DetailRow(label = "Address Type", value = device.addressType.name)
                    device.txPowerLevel?.let { DetailRow(label = "Tx Power Level", value = "$it dBm") }
                    device.isConnectable?.let { DetailRow(label = "Connectable", value = if (it) "Yes" else "No") }

                    if (device.serviceUuids.isNotEmpty()) {
                        DetailRow(label = "Service UUIDs", value = device.serviceUuids.joinToString("\n"))
                    }

                    if (device.manufacturerData.isNotEmpty()) {
                        val mfgText = device.manufacturerData.entries.joinToString("\n") { (id, bytes) ->
                            "ID 0x${"%04X".format(id)} (${id}): ${bytes.joinToString("") { "%02X".format(it) }}"
                        }
                        DetailRow(label = "Manufacturer Data", value = mfgText)
                    }

                    if (device.serviceData.isNotEmpty()) {
                        val svcText = device.serviceData.entries.joinToString("\n") { (uuid, bytes) ->
                            "$uuid: ${bytes.joinToString("") { "%02X".format(it) }}"
                        }
                        DetailRow(label = "Service Data", value = svcText)
                    }

                    if (device.rawPayload.isNotEmpty()) {
                        val hexPayload = device.rawPayload.joinToString(" ") { "%02X".format(it) }
                        DetailRow(label = "Raw Payload Hex", value = hexPayload)
                    }
                }
            }
        }
    }
}

@Composable
private fun RssiBadge(
    rssiDbm: Int,
    category: SignalCategory
) {
    val bgColor = when (category) {
        SignalCategory.STRONG -> Color(0xFF2E7D32)
        SignalCategory.MEDIUM -> Color(0xFFE65100)
        SignalCategory.WEAK -> Color(0xFF757575)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = "$rssiDbm dBm",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun PermissionBanner(
    rationale: String,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text(
                    text = "Permission Required",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Text(
                text = rationale,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRequestPermission) {
                    Text("Grant Permission")
                }
                OutlinedButton(onClick = onOpenSettings) {
                    Text("App Settings")
                }
            }
        }
    }
}

@Composable
private fun BluetoothDisabledBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.BluetoothDisabled, contentDescription = null)
            Column {
                Text(
                    text = "Bluetooth Disabled",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Please enable Bluetooth in your device settings to discover nearby BLE signals.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun AlertBanner(title: String, message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun EmptyStateView(
    isScanning: Boolean,
    hasFilter: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (isScanning) "Listening for BLE signals..." else if (hasFilter) "No matching devices" else "No BLE devices discovered yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (isScanning) {
                    "Collecting unencrypted advertisement broadcasts from nearby devices."
                } else if (hasFilter) {
                    "Try adjusting your search query or filter mode."
                } else {
                    "Tap 'Scan (15s)' above to begin observing nearby Bluetooth Low Energy advertisements."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
