package app.wirelessscanner.ui.screens.wifi

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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import app.wirelessscanner.core.model.SignalCategory
import app.wirelessscanner.wifi.model.DiscoveredWifiAccessPoint
import app.wirelessscanner.wifi.model.WifiBand
import app.wirelessscanner.wifi.model.WifiSecurityType
import app.wirelessscanner.wifi.permission.WifiPermissionManager
import app.wirelessscanner.wifi.scanner.ScanFreshness
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WifiScreen(
    modifier: Modifier = Modifier,
    viewModel: WifiViewModel = run {
        val context = LocalContext.current
        viewModel(factory = WifiViewModel.provideFactory(context))
    }
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionManager = WifiPermissionManager(context)

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
        WifiScanControlHeader(
            uiState = uiState,
            onStartScan = {
                if (!permissionManager.hasScanPermission()) {
                    permissionLauncher.launch(permissionManager.requiredPermissions.toTypedArray())
                } else {
                    viewModel.startScan()
                }
            },
            onStopScan = { viewModel.stopScan() },
            onClear = { viewModel.clearObservations() },
            onLoadCached = { viewModel.loadCachedResults() }
        )

        // --- 2. Permission / Hardware Alert Banners ---
        when (uiState.status) {
            is WifiUiStatus.PermissionDenied -> {
                WifiAlertBanner(
                    title = "Location Permission Required",
                    message = permissionManager.rationaleDescription,
                    actionButton = {
                        Button(onClick = { permissionLauncher.launch(permissionManager.requiredPermissions.toTypedArray()) }) {
                            Text("Grant Permission")
                        }
                    },
                    secondaryButton = {
                        OutlinedButton(onClick = {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                            context.startActivity(intent)
                        }) {
                            Text("App Settings")
                        }
                    }
                )
            }
            is WifiUiStatus.WifiDisabled -> {
                WifiAlertBanner(
                    title = "Wi-Fi Disabled",
                    message = "Please turn on Wi-Fi in Android Settings to discover access points.",
                    icon = Icons.Default.WifiOff,
                    actionButton = {
                        Button(onClick = {
                            context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                        }) {
                            Text("Open Wi-Fi Settings")
                        }
                    }
                )
            }
            is WifiUiStatus.LocationServiceDisabled -> {
                WifiAlertBanner(
                    title = "Location Services Required",
                    message = "Android 9+ requires system Location Services (GPS/Network) to be enabled to deliver Wi-Fi scan results with SSIDs and BSSIDs.",
                    icon = Icons.Default.LocationOff,
                    actionButton = {
                        Button(onClick = {
                            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        }) {
                            Text("Enable Location")
                        }
                    }
                )
            }
            is WifiUiStatus.Error -> {
                WifiAlertBanner(
                    title = "Scan Status",
                    message = (uiState.status as WifiUiStatus.Error).message,
                    icon = Icons.Default.Warning
                )
            }
            else -> {}
        }

        // --- 3. View Switcher Tabs (Access Points vs Channel Analysis) ---
        TabRow(selectedTabIndex = if (uiState.showChannelAnalysis) 1 else 0) {
            Tab(
                selected = !uiState.showChannelAnalysis,
                onClick = { viewModel.toggleChannelAnalysis(false) },
                text = { Text("Access Points (${uiState.totalAccessPointsCount})") },
                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) }
            )
            Tab(
                selected = uiState.showChannelAnalysis,
                onClick = { viewModel.toggleChannelAnalysis(true) },
                text = { Text("Channel Analysis") },
                icon = { Icon(Icons.Default.BarChart, contentDescription = null) }
            )
        }

        if (uiState.showChannelAnalysis) {
            ChannelAnalysisView(
                metrics = uiState.channelMetrics,
                selectedBand = uiState.selectedBand,
                onSelectBand = { viewModel.setSelectedBand(it) }
            )
        } else {
            // --- 4. Search and Filter Controls ---
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by SSID or BSSID...") },
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

            // Band and Sorting Filter Chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedBand == null,
                    onClick = { viewModel.setSelectedBand(null) },
                    label = { Text("All Bands") }
                )
                FilterChip(
                    selected = uiState.selectedBand == WifiBand.BAND_2_4_GHZ,
                    onClick = {
                        viewModel.setSelectedBand(if (uiState.selectedBand == WifiBand.BAND_2_4_GHZ) null else WifiBand.BAND_2_4_GHZ)
                    },
                    label = { Text("2.4 GHz") }
                )
                FilterChip(
                    selected = uiState.selectedBand == WifiBand.BAND_5_GHZ,
                    onClick = {
                        viewModel.setSelectedBand(if (uiState.selectedBand == WifiBand.BAND_5_GHZ) null else WifiBand.BAND_5_GHZ)
                    },
                    label = { Text("5 GHz") }
                )
                FilterChip(
                    selected = uiState.selectedBand == WifiBand.BAND_6_GHZ,
                    onClick = {
                        viewModel.setSelectedBand(if (uiState.selectedBand == WifiBand.BAND_6_GHZ) null else WifiBand.BAND_6_GHZ)
                    },
                    label = { Text("6 GHz") }
                )
                FilterChip(
                    selected = uiState.sortOrder == WifiSortOrder.STRONGEST_SIGNAL,
                    onClick = { viewModel.setSortOrder(WifiSortOrder.STRONGEST_SIGNAL) },
                    label = { Text("Signal") }
                )
                FilterChip(
                    selected = uiState.sortOrder == WifiSortOrder.SSID,
                    onClick = { viewModel.setSortOrder(WifiSortOrder.SSID) },
                    label = { Text("SSID") }
                )
                FilterChip(
                    selected = uiState.sortOrder == WifiSortOrder.CHANNEL,
                    onClick = { viewModel.setSortOrder(WifiSortOrder.CHANNEL) },
                    label = { Text("Channel") }
                )
                FilterChip(
                    selected = uiState.sortOrder == WifiSortOrder.FREQUENCY,
                    onClick = { viewModel.setSortOrder(WifiSortOrder.FREQUENCY) },
                    label = { Text("Frequency") }
                )
                FilterChip(
                    selected = uiState.sortOrder == WifiSortOrder.MOST_RECENT,
                    onClick = { viewModel.setSortOrder(WifiSortOrder.MOST_RECENT) },
                    label = { Text("Recent") }
                )
            }

            // --- 5. Discovered AP List / Empty View ---
            if (uiState.displayedAccessPoints.isEmpty()) {
                WifiEmptyStateView(
                    isScanning = uiState.isScanning,
                    hasFilter = uiState.searchQuery.isNotEmpty() || uiState.selectedBand != null
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.displayedAccessPoints,
                        key = { it.bssid }
                    ) { ap ->
                        WifiAccessPointCard(
                            ap = ap,
                            isExpanded = uiState.selectedBssid == ap.bssid,
                            onToggleExpand = { viewModel.selectAccessPoint(ap.bssid) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WifiScanControlHeader(
    uiState: WifiUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onClear: () -> Unit,
    onLoadCached: () -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

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
                        if (uiState.isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Scanning Wi-Fi...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Wi-Fi Discovery Idle",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val freshnessText = when (uiState.scanFreshness) {
                        is ScanFreshness.Fresh -> "Fresh results"
                        is ScanFreshness.Cached -> "Cached results"
                        is ScanFreshness.Throttled -> "Scan throttled"
                        else -> "No scan yet"
                    }

                    val lastScanText = uiState.lastScanTimestamp?.let {
                        "Last: ${timeFormat.format(Date(it))}"
                    } ?: "Not scanned"

                    Text(
                        text = "${uiState.totalAccessPointsCount} access points · $freshnessText · $lastScanText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (uiState.isScanning) {
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
                            Text("Scan")
                        }
                    }

                    if (uiState.totalAccessPointsCount > 0 && !uiState.isScanning) {
                        IconButton(onClick = onClear) {
                            Icon(Icons.Default.Refresh, contentDescription = "Clear")
                        }
                    }
                }
            }

            if (uiState.isScanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun WifiAccessPointCard(
    ap: DiscoveredWifiAccessPoint,
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
                        text = ap.displaySsid,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (!ap.isHiddenSsid) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (!ap.isHiddenSsid) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = ap.bssid,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${ap.band} · Ch ${ap.channel} (${ap.frequencyMhz} MHz)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = ap.securityType.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WifiRssiBadge(rssiDbm = ap.latestRssi.dbm, category = ap.latestRssi.category)
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
                        text = "Access Point Evidence",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    WifiDetailRow(label = "First Observed", value = timeFormat.format(Date(ap.firstSeenEpochMillis)))
                    WifiDetailRow(label = "Last Observed", value = timeFormat.format(Date(ap.lastSeenEpochMillis)))
                    WifiDetailRow(label = "Observations Count", value = "${ap.observationCount} (fresh scans only)")
                    WifiDetailRow(label = "Median RSSI", value = "${ap.medianRssi} dBm (relative)")
                    WifiDetailRow(label = "RSSI History (last ${ap.rssiHistory.size})", value = ap.rssiHistory.joinToString(", ") { "$it" })
                    WifiDetailRow(label = "Channel Width", value = ap.channelWidth)
                    WifiDetailRow(label = "Data Freshness", value = if (ap.isFresh) "Fresh (from latest scan)" else "Cached")
                    WifiDetailRow(label = "Raw Capabilities", value = ap.capabilities)

                    ap.metadata["wifiStandard"]?.let {
                        WifiDetailRow(label = "Wi-Fi Standard", value = it)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelAnalysisView(
    metrics: List<ChannelMetric>,
    selectedBand: WifiBand?,
    onSelectBand: (WifiBand?) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Explanatory note
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = "Channel utilization displays detected access point counts and average RSSI per channel. Note: Access point count alone does not prove channel congestion, as it does not measure active data traffic volume or interference.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Band Filter
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedBand == null,
                onClick = { onSelectBand(null) },
                label = { Text("All Bands") }
            )
            FilterChip(
                selected = selectedBand == WifiBand.BAND_2_4_GHZ,
                onClick = { onSelectBand(if (selectedBand == WifiBand.BAND_2_4_GHZ) null else WifiBand.BAND_2_4_GHZ) },
                label = { Text("2.4 GHz") }
            )
            FilterChip(
                selected = selectedBand == WifiBand.BAND_5_GHZ,
                onClick = { onSelectBand(if (selectedBand == WifiBand.BAND_5_GHZ) null else WifiBand.BAND_5_GHZ) },
                label = { Text("5 GHz") }
            )
            FilterChip(
                selected = selectedBand == WifiBand.BAND_6_GHZ,
                onClick = { onSelectBand(if (selectedBand == WifiBand.BAND_6_GHZ) null else WifiBand.BAND_6_GHZ) },
                label = { Text("6 GHz") }
            )
        }

        if (metrics.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No access points available for channel analysis.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val maxCount = metrics.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(metrics, key = { "${it.band}_${it.channel}" }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Channel ${item.channel} (${item.band})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${item.count} APs · Avg ${item.averageRssiDbm} dBm",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { item.count.toFloat() / maxCount.toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WifiRssiBadge(
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
private fun WifiDetailRow(label: String, value: String) {
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
private fun WifiAlertBanner(
    title: String,
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Warning,
    actionButton: (@Composable () -> Unit)? = null,
    secondaryButton: (@Composable () -> Unit)? = null
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
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            if (actionButton != null || secondaryButton != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    actionButton?.invoke()
                    secondaryButton?.invoke()
                }
            }
        }
    }
}

@Composable
private fun WifiEmptyStateView(
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
                imageVector = Icons.Default.Wifi,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (isScanning) "Discovering Wi-Fi access points..." else if (hasFilter) "No matching access points" else "No Wi-Fi networks discovered yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (isScanning) {
                    "Requesting scan from Android Wi-Fi manager. Foreground scans are throttled to 4 requests per 2 minutes on Android 9+."
                } else if (hasFilter) {
                    "Try adjusting your band selection or search query."
                } else {
                    "Tap 'Scan' above to initiate a Wi-Fi access point discovery cycle."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
