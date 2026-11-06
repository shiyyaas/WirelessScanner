package app.wirelessscanner.ui.screens.wifi

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.wirelessscanner.collector.CollectorStatus
import app.wirelessscanner.wifi.model.DiscoveredWifiAccessPoint
import app.wirelessscanner.wifi.model.WifiBand
import app.wirelessscanner.wifi.model.WifiSecurityType
import app.wirelessscanner.wifi.permission.WifiPermissionChecker
import app.wirelessscanner.wifi.permission.WifiPermissionManager
import app.wirelessscanner.wifi.scanner.AndroidWifiScanner
import app.wirelessscanner.wifi.scanner.ScanFreshness
import app.wirelessscanner.wifi.scanner.WifiHardwareScanner
import app.wirelessscanner.wifi.scanner.WifiScanSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class WifiSortOrder {
    STRONGEST_SIGNAL,
    SSID,
    FREQUENCY,
    CHANNEL,
    MOST_RECENT
}

data class ChannelMetric(
    val channel: Int,
    val band: WifiBand,
    val count: Int,
    val averageRssiDbm: Int
)

sealed interface WifiUiStatus {
    data object Idle : WifiUiStatus
    data object Scanning : WifiUiStatus
    data object PermissionDenied : WifiUiStatus
    data object WifiDisabled : WifiUiStatus
    data object LocationServiceDisabled : WifiUiStatus
    data class Error(val message: String) : WifiUiStatus
}

data class WifiUiState(
    val status: WifiUiStatus = WifiUiStatus.Idle,
    val isScanning: Boolean = false,
    val scanFreshness: ScanFreshness = ScanFreshness.Unknown,
    val lastScanTimestamp: Long? = null,
    val totalAccessPointsCount: Int = 0,
    val displayedAccessPoints: List<DiscoveredWifiAccessPoint> = emptyList(),
    val channelMetrics: List<ChannelMetric> = emptyList(),
    val sortOrder: WifiSortOrder = WifiSortOrder.STRONGEST_SIGNAL,
    val selectedBand: WifiBand? = null,
    val selectedSecurityType: WifiSecurityType? = null,
    val minRssiDbm: Int? = null,
    val searchQuery: String = "",
    val selectedBssid: String? = null,
    val showChannelAnalysis: Boolean = false
)

class WifiViewModel(
    private val sessionManager: WifiScanSessionManager,
    private val permissionManager: WifiPermissionChecker
) : ViewModel() {

    private val _sortOrder = MutableStateFlow(WifiSortOrder.STRONGEST_SIGNAL)
    private val _selectedBand = MutableStateFlow<WifiBand?>(null)
    private val _selectedSecurityType = MutableStateFlow<WifiSecurityType?>(null)
    private val _minRssiDbm = MutableStateFlow<Int?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedBssid = MutableStateFlow<String?>(null)
    private val _showChannelAnalysis = MutableStateFlow(false)
    private val _permissionDenied = MutableStateFlow(false)

    val uiState: StateFlow<WifiUiState> = combine(
        sessionManager.status,
        sessionManager.discoveredAccessPoints,
        sessionManager.scanFreshness,
        sessionManager.lastScanTimestamp,
        _sortOrder,
        _selectedBand,
        _selectedSecurityType,
        _minRssiDbm,
        _searchQuery,
        _selectedBssid,
        _showChannelAnalysis,
        _permissionDenied
    ) { args: Array<Any?> ->
        val status = args[0] as CollectorStatus
        @Suppress("UNCHECKED_CAST")
        val aps = args[1] as List<DiscoveredWifiAccessPoint>
        val freshness = args[2] as ScanFreshness
        val lastScan = args[3] as? Long
        val sort = args[4] as WifiSortOrder
        val band = args[5] as? WifiBand
        val sec = args[6] as? WifiSecurityType
        val minRssi = args[7] as? Int
        val query = args[8] as String
        val selectedBssid = args[9] as? String
        val showAnalysis = args[10] as Boolean
        val permDenied = args[11] as Boolean

        val wifiUiStatus: WifiUiStatus = when {
            permDenied || !permissionManager.hasScanPermission() -> WifiUiStatus.PermissionDenied
            !permissionManager.isWifiEnabled() -> WifiUiStatus.WifiDisabled
            !permissionManager.isLocationServiceEnabled() -> WifiUiStatus.LocationServiceDisabled
            status is CollectorStatus.Error -> WifiUiStatus.Error(status.message)
            freshness is ScanFreshness.Throttled -> WifiUiStatus.Error(freshness.message)
            status is CollectorStatus.Observing || status is CollectorStatus.Starting -> WifiUiStatus.Scanning
            else -> WifiUiStatus.Idle
        }

        // Filtering
        val filtered = aps.filter { ap ->
            val matchesBand = band == null || ap.band == band
            val matchesSecurity = sec == null || ap.securityType == sec
            val matchesRssi = minRssi == null || ap.latestRssi.dbm >= minRssi
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                val q = query.trim().lowercase()
                ap.bssid.lowercase().contains(q) || (ap.ssid?.lowercase()?.contains(q) == true)
            }
            matchesBand && matchesSecurity && matchesRssi && matchesQuery
        }

        // Sorting
        val sorted = when (sort) {
            WifiSortOrder.STRONGEST_SIGNAL -> filtered.sortedByDescending { it.latestRssi.dbm }
            WifiSortOrder.SSID -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.displaySsid })
            WifiSortOrder.FREQUENCY -> filtered.sortedBy { it.frequencyMhz }
            WifiSortOrder.CHANNEL -> filtered.sortedWith(compareBy({ it.band.ordinal }, { it.channel }))
            WifiSortOrder.MOST_RECENT -> filtered.sortedByDescending { it.lastSeenEpochMillis }
        }

        // Channel Analysis Metrics (from all available APs or band-filtered APs)
        val relevantAps = if (band != null) aps.filter { it.band == band } else aps
        val channelMetrics = relevantAps
            .groupBy { Pair(it.channel, it.band) }
            .map { (key, group) ->
                val avgRssi = if (group.isNotEmpty()) {
                    group.map { it.latestRssi.dbm }.average().toInt()
                } else 0
                ChannelMetric(
                    channel = key.first,
                    band = key.second,
                    count = group.size,
                    averageRssiDbm = avgRssi
                )
            }
            .sortedWith(compareBy({ it.band.ordinal }, { it.channel }))

        WifiUiState(
            status = wifiUiStatus,
            isScanning = wifiUiStatus == WifiUiStatus.Scanning,
            scanFreshness = freshness,
            lastScanTimestamp = lastScan,
            totalAccessPointsCount = aps.size,
            displayedAccessPoints = sorted,
            channelMetrics = channelMetrics,
            sortOrder = sort,
            selectedBand = band,
            selectedSecurityType = sec,
            minRssiDbm = minRssi,
            searchQuery = query,
            selectedBssid = selectedBssid,
            showChannelAnalysis = showAnalysis
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = WifiUiState(
            status = when {
                !permissionManager.hasScanPermission() -> WifiUiStatus.PermissionDenied
                !permissionManager.isWifiEnabled() -> WifiUiStatus.WifiDisabled
                !permissionManager.isLocationServiceEnabled() -> WifiUiStatus.LocationServiceDisabled
                else -> WifiUiStatus.Idle
            }
        )
    )

    fun startScan() {
        if (!permissionManager.hasScanPermission()) {
            _permissionDenied.value = true
            return
        }
        _permissionDenied.value = false
        viewModelScope.launch {
            sessionManager.triggerScan()
        }
    }

    fun stopScan() {
        viewModelScope.launch {
            sessionManager.stopObserving()
        }
    }

    fun loadCachedResults() {
        sessionManager.loadCachedResults()
    }

    fun clearObservations() {
        viewModelScope.launch {
            sessionManager.clearResults()
            _selectedBssid.value = null
        }
    }

    fun setSortOrder(order: WifiSortOrder) {
        _sortOrder.value = order
    }

    fun setSelectedBand(band: WifiBand?) {
        _selectedBand.value = band
    }

    fun setSelectedSecurityType(securityType: WifiSecurityType?) {
        _selectedSecurityType.value = securityType
    }

    fun setMinRssiDbm(minRssi: Int?) {
        _minRssiDbm.value = minRssi
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectAccessPoint(bssid: String?) {
        _selectedBssid.update { if (it == bssid) null else bssid }
    }

    fun toggleChannelAnalysis(show: Boolean) {
        _showChannelAnalysis.value = show
    }

    fun onPermissionResult(granted: Boolean) {
        _permissionDenied.value = !granted
        if (granted) {
            startScan()
        }
    }

    companion object {
        fun provideFactory(
            context: Context,
            hardwareScanner: WifiHardwareScanner = AndroidWifiScanner(context.applicationContext),
            coroutineScope: CoroutineScope? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appContext = context.applicationContext
                val permManager = WifiPermissionManager(appContext)
                val sessionScope = coroutineScope
                    ?: CoroutineScope(SupervisorJob() + Dispatchers.Default)
                val sessionManager = WifiScanSessionManager(
                    scanner = hardwareScanner,
                    scope = sessionScope
                )
                return WifiViewModel(sessionManager, permManager) as T
            }
        }
    }
}
