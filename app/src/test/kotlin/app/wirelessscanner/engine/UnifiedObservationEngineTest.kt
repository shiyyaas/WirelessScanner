package app.wirelessscanner.engine

import app.wirelessscanner.core.model.Observation
import app.wirelessscanner.core.model.RadioType
import app.wirelessscanner.core.model.ReceiverSource
import app.wirelessscanner.core.model.SignalStrength
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import org.junit.Test

class UnifiedObservationEngineTest {

    @Test
    fun engine_mergesBleAndWifiObservationsPreservingProvenance() {
        val engine = UnifiedObservationEngine(flowOf(emptyList()), flowOf(emptyList()))

        val bleObs = Observation(
            id = "ble-1",
            timestampEpochMillis = 100L,
            radioType = RadioType.BLE,
            receiverSource = ReceiverSource.Internal(),
            rawAddress = "AA:BB:CC:DD:EE:FF",
            signalStrength = SignalStrength(-70)
        )

        val wifiObs = Observation(
            id = "wifi-1",
            timestampEpochMillis = 200L,
            radioType = RadioType.WIFI,
            receiverSource = ReceiverSource.Internal(),
            rawAddress = "11:22:33:44:55:66",
            signalStrength = SignalStrength(-55)
        )

        engine.ingestBle(listOf(bleObs))
        engine.ingestWifi(listOf(wifiObs))

        val all = engine.allObservations.value
        assertThat(all).hasSize(2)
        assertThat(all[0].id).isEqualTo("wifi-1")
        assertThat(all[1].id).isEqualTo("ble-1")
        assertThat(engine.getObservationsByRadio(RadioType.BLE)).containsExactly(bleObs)
        assertThat(engine.getObservationsByRadio(RadioType.WIFI)).containsExactly(wifiObs)
    }
}
