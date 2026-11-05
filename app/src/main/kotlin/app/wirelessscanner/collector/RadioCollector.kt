package app.wirelessscanner.collector

import app.wirelessscanner.core.model.Observation
import app.wirelessscanner.core.model.RadioType
import kotlinx.coroutines.flow.StateFlow

/**
 * Pluggable contract for radio collectors.
 *
 * Temporarily declared in `:app` during Milestone 1 foundation.
 * Will be extracted to `:core:scanner-api` in Milestone 2.
 *
 * NOTE: Scanning implementations (BLE, Wi-Fi, external hardware) are intentionally
 * out of scope in this milestone.
 */
interface RadioCollector {
    val radioType: RadioType
    val status: StateFlow<CollectorStatus>
    val observations: StateFlow<List<Observation>>

    suspend fun startObserving()
    suspend fun stopObserving()
}

sealed interface CollectorStatus {
    data object Idle : CollectorStatus
    data object Starting : CollectorStatus
    data object Observing : CollectorStatus
    data class Unavailable(val reason: String) : CollectorStatus
    data class Error(val message: String) : CollectorStatus
}
