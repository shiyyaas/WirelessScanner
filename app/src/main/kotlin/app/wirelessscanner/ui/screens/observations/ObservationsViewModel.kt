package app.wirelessscanner.ui.screens.observations

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.wirelessscanner.ble.model.DiscoveredBleDevice
import app.wirelessscanner.ble.permission.BlePermissionChecker
import app.wirelessscanner.ble.permission.BlePermissionManager
import app.wirelessscanner.ble.permission.BluetoothHardwareState
import app.wirelessscanner.ble.permission.BluetoothStateManager
import app.wirelessscanner.ble.permission.BluetoothStateObserver
import app.wirelessscanner.ble.scanner.AndroidBleScanner
import app.wirelessscanner.ble.scanner.BleHardwareScanner
import app.wirelessscanner.ble.scanner.BleScanSessionManager
import app.wirelessscanner.collector.CollectorStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Sort options for the observations list.
 */
enum class ObservationSortOrder {
    STRONGEST_SIGNAL,
    MOST_RECENT,
    HIGHEST_COUNT
}

/**
 * Filter mode for discovered devices.
 */
enum class NameFilterMode {
    ALL,
    NAMED_ONLY,
    UNNAMED_ONLY
}

/**
 * Scanning lifecycle / permission / hardware UI states.
 */
sealed interface ScanUiStatus {
    data object Idle : ScanUiStatus
    data object Scanning : ScanUiStatus
    data object PermissionDenied : ScanUiStatus
    data object BluetoothDisabled : ScanUiStatus
    data object BluetoothUnsupported : ScanUiStatus
    data class Error(val message: String) : ScanUiStatus
}

data class ObservationsUiState(
    val scanUiStatus: ScanUiStatus = ScanUiStatus.Idle,
    val secondsRemaining: Int = 0,
    val totalObservationsCount: Int = 0,
    val uniqueDeviceCount: Int = 0,
    val displayedDevices: List<DiscoveredBleDevice> = emptyList(),
    val sortOrder: ObservationSortOrder = ObservationSortOrder.STRONGEST_SIGNAL,
    val filterMode: NameFilterMode = NameFilterMode.ALL,
    val searchQuery: String = "",
    val selectedDeviceAddress: String? = null
)

class ObservationsViewModel(
    private val sessionManager: BleScanSessionManager,
    private val permissionManager: BlePermissionChecker,
    private val bluetoothStateManager: BluetoothStateObserver
) : ViewModel() {

    private val _sortOrder = MutableStateFlow(ObservationSortOrder.STRONGEST_SIGNAL)
    private val _filterMode = MutableStateFlow(NameFilterMode.ALL)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedDeviceAddress = MutableStateFlow<String?>(null)
    private val _permissionDenied = MutableStateFlow(false)

    val uiState: StateFlow<ObservationsUiState> = combine(
        sessionManager.status,
        sessionManager.discoveredDevices,
        bluetoothStateManager.observeState(),
        _sortOrder,
        _filterMode,
        _searchQuery,
        _selectedDeviceAddress,
        _permissionDenied
    ) { args: Array<Any?> ->
        val status = args[0] as CollectorStatus
        @Suppress("UNCHECKED_CAST")
        val devices = args[1] as List<DiscoveredBleDevice>
        val btState = args[2] as BluetoothHardwareState
        val sort = args[3] as ObservationSortOrder
        val filter = args[4] as NameFilterMode
        val query = args[5] as String
        val selectedAddress = args[6] as? String
        val permDenied = args[7] as Boolean

        val scanUiStatus: ScanUiStatus = when {
            permDenied || !permissionManager.hasScanPermission() -> ScanUiStatus.PermissionDenied
            btState is BluetoothHardwareState.Unsupported -> ScanUiStatus.BluetoothUnsupported
            btState is BluetoothHardwareState.Disabled -> ScanUiStatus.BluetoothDisabled
            status is CollectorStatus.Error -> ScanUiStatus.Error(status.message)
            status is CollectorStatus.Observing || status is CollectorStatus.Starting -> ScanUiStatus.Scanning
            else -> ScanUiStatus.Idle
        }

        val filtered = devices.filter { device ->
            val matchesFilter = when (filter) {
                NameFilterMode.ALL -> true
                NameFilterMode.NAMED_ONLY -> !device.displayName.isNullOrBlank()
                NameFilterMode.UNNAMED_ONLY -> device.displayName.isNullOrBlank()
            }

            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                val q = query.trim().lowercase()
                device.rawAddress.lowercase().contains(q) ||
                    (device.displayName?.lowercase()?.contains(q) == true)
            }

            matchesFilter && matchesQuery
        }

        val sorted = when (sort) {
            ObservationSortOrder.STRONGEST_SIGNAL -> filtered.sortedByDescending { it.latestRssi.dbm }
            ObservationSortOrder.MOST_RECENT -> filtered.sortedByDescending { it.lastSeenEpochMillis }
            ObservationSortOrder.HIGHEST_COUNT -> filtered.sortedByDescending { it.observationCount }
        }

        ObservationsUiState(
            scanUiStatus = scanUiStatus,
            secondsRemaining = sessionManager.secondsRemaining.value,
            totalObservationsCount = sessionManager.observations.value.size,
            uniqueDeviceCount = devices.size,
            displayedDevices = sorted,
            sortOrder = sort,
            filterMode = filter,
            searchQuery = query,
            selectedDeviceAddress = selectedAddress
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ObservationsUiState(
            scanUiStatus = when {
                !permissionManager.hasScanPermission() -> ScanUiStatus.PermissionDenied
                bluetoothStateManager.currentState is BluetoothHardwareState.Unsupported -> ScanUiStatus.BluetoothUnsupported
                bluetoothStateManager.currentState is BluetoothHardwareState.Disabled -> ScanUiStatus.BluetoothDisabled
                else -> ScanUiStatus.Idle
            }
        )
    )

    fun startScan(durationSeconds: Int = BleScanSessionManager.DEFAULT_SCAN_DURATION_SECONDS) {
        if (!permissionManager.hasScanPermission()) {
            _permissionDenied.value = true
            return
        }
        _permissionDenied.value = false
        viewModelScope.launch {
            sessionManager.startObserving(durationSeconds)
        }
    }

    fun stopScan() {
        viewModelScope.launch {
            sessionManager.stopObserving()
        }
    }

    fun clearObservations() {
        viewModelScope.launch {
            sessionManager.clearResults()
            _selectedDeviceAddress.value = null
        }
    }

    fun setSortOrder(order: ObservationSortOrder) {
        _sortOrder.value = order
    }

    fun setFilterMode(mode: NameFilterMode) {
        _filterMode.value = mode
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectDevice(address: String?) {
        _selectedDeviceAddress.update { if (it == address) null else address }
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
            hardwareScanner: BleHardwareScanner = AndroidBleScanner(context.applicationContext)
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appContext = context.applicationContext
                val permManager = BlePermissionManager(appContext)
                val btManager = BluetoothStateManager(appContext)
                val sessionManager = BleScanSessionManager(
                    scanner = hardwareScanner,
                    scope = kotlinx.coroutines.GlobalScope
                )
                return ObservationsViewModel(sessionManager, permManager, btManager) as T
            }
        }
    }
}
