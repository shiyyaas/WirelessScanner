package app.wirelessscanner.ble.scanner

import app.wirelessscanner.core.model.RadioType
import app.wirelessscanner.core.model.ReceiverSource
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BleScanResultMapperTest {

    @Test
    fun map_populatesBasicObservationFields() {
        val payload = TestBlePayload(
            address = "AA:BB:CC:DD:EE:FF",
            advertisedName = "AdvertisedBeacon",
            cachedSystemName = "CachedSystemDevice",
            rssi = -65,
            rawBytes = byteArrayOf(0x02, 0x01, 0x06),
            txPower = -12,
            isConnectable = true,
            addressType = "PUBLIC"
        )

        val timestamp = 1700000000000L
        val observation = BlePayloadMapper.map(payload, timestamp)

        assertThat(observation.rawAddress).isEqualTo("AA:BB:CC:DD:EE:FF")
        assertThat(observation.advertisedName).isEqualTo("AdvertisedBeacon")
        assertThat(observation.radioType).isEqualTo(RadioType.BLE)
        assertThat(observation.receiverSource).isEqualTo(ReceiverSource.Internal())
        assertThat(observation.signalStrength.dbm).isEqualTo(-65)
        assertThat(observation.timestampEpochMillis).isEqualTo(timestamp)
        assertThat(observation.rawPayload).containsExactly(0x02.toByte(), 0x01.toByte(), 0x06.toByte()).inOrder()
        assertThat(observation.metadata["cachedSystemName"]).isEqualTo("CachedSystemDevice")
        assertThat(observation.metadata["txPowerLevel"]).isEqualTo("-12")
        assertThat(observation.metadata["isConnectable"]).isEqualTo("true")
        assertThat(observation.metadata["addressType"]).isEqualTo("PUBLIC")
    }

    @Test
    fun map_handlesEmptyPayloadGracefully() {
        val payload = TestBlePayload(
            address = "11:22:33:44:55:66",
            rssi = -82
        )

        val observation = BlePayloadMapper.map(payload)

        assertThat(observation.rawAddress).isEqualTo("11:22:33:44:55:66")
        assertThat(observation.advertisedName).isNull()
        assertThat(observation.rawPayload).isEmpty()
        assertThat(observation.signalStrength.dbm).isEqualTo(-82)
    }

    @Test
    fun map_parsesManufacturerDataAndServiceUuids() {
        val payload = TestBlePayload(
            address = "00:11:22:33:44:55",
            serviceUuids = listOf("0000180d-0000-1000-8000-00805f9b34fb"),
            manufacturerData = mapOf(0x004C to byteArrayOf(0x02, 0x15)),
            serviceData = mapOf("0000feaa-0000-1000-8000-00805f9b34fb" to byteArrayOf(0x10, 0x00))
        )

        val observation = BlePayloadMapper.map(payload)

        assertThat(observation.metadata["mfg_76"]).isEqualTo("0215")
        assertThat(observation.metadata["serviceUuids"]).isEqualTo("0000180d-0000-1000-8000-00805f9b34fb")
        assertThat(observation.metadata["svc_data_0000feaa-0000-1000-8000-00805f9b34fb"]).isEqualTo("1000")
    }
}
