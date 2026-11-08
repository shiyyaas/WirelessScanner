package app.wirelessscanner.engine

import app.wirelessscanner.core.model.Observation
import app.wirelessscanner.core.model.RadioType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UnifiedObservationEngine(
    private val bleObservations: Flow<List<Observation>>,
    private val wifiObservations: Flow<List<Observation>>
) {
    private val _allObservations = MutableStateFlow<List<Observation>>(emptyList())
    val allObservations: StateFlow<List<Observation>> = _allObservations.asStateFlow()

    private val radioFeeds = mutableMapOf<RadioType, List<Observation>>(
        RadioType.BLE to emptyList(),
        RadioType.WIFI to emptyList()
    )

    fun ingestBle(observations: List<Observation>) {
        updateObservations(observations, RadioType.BLE)
    }

    fun ingestWifi(observations: List<Observation>) {
        updateObservations(observations, RadioType.WIFI)
    }

    private synchronized fun updateObservations(newObs: List<Observation>, type: RadioType) {
        radioFeeds[type] = newObs
        val combined = radioFeeds.values.flatten().sortedByDescending { it.timestampEpochMillis }
        _allObservations.value = combined
    }

    fun getObservationsByRadio(radioType: RadioType): List<Observation> {
        return _allObservations.value.filter { it.radioType == radioType }
    }
}
