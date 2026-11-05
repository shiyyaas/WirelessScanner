package app.wirelessscanner.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class ObservationModelTest {

    @Test
    fun `observation preserves provenance and data fields`() {
        val internalSource = ReceiverSource.Internal()
        val signal = SignalStrength(-72)
        val payload = byteArrayOf(0x02, 0x01, 0x06)

        val obs = Observation.createWithByteArray(
            id = "obs-1",
            timestampEpochMillis = 1710000000000L,
            radioType = RadioType.BLE,
            receiverSource = internalSource,
            rawAddress = "AA:BB:CC:DD:EE:FF",
            advertisedName = "Beacon_Test",
            signalStrength = signal,
            rawBytes = payload,
            metadata = mapOf("txPower" to "-59")
        )

        assertThat(obs.id).isEqualTo("obs-1")
        assertThat(obs.timestampEpochMillis).isEqualTo(1710000000000L)
        assertThat(obs.radioType).isEqualTo(RadioType.BLE)
        assertThat(obs.receiverSource).isEqualTo(internalSource)
        assertThat(obs.rawAddress).isEqualTo("AA:BB:CC:DD:EE:FF")
        assertThat(obs.advertisedName).isEqualTo("Beacon_Test")
        assertThat(obs.signalStrength.dbm).isEqualTo(-72)
        assertThat(obs.signalStrength.category).isEqualTo(SignalCategory.MEDIUM)
        assertThat(obs.rawPayload).containsExactly(2.toByte(), 1.toByte(), 6.toByte()).inOrder()
        assertThat(obs.metadata["txPower"]).isEqualTo("-59")
    }

    @Test
    fun `observation payload is immutable against source array mutations`() {
        val payload = byteArrayOf(0x10, 0x20)
        val obs = Observation.createWithByteArray(
            id = "obs-2",
            timestampEpochMillis = 1710000000001L,
            radioType = RadioType.WIFI,
            receiverSource = ReceiverSource.Internal(),
            rawAddress = "11:22:33:44:55:66",
            advertisedName = "TestAP",
            signalStrength = SignalStrength(-50),
            rawBytes = payload
        )

        // Mutate original array
        payload[0] = 0x99.toByte()

        // Observation payload must retain original value
        assertThat(obs.rawPayload[0]).isEqualTo(0x10.toByte())
    }

    @Test
    fun `external usb receiver source preserves peripheral metadata`() {
        val usbSource = ReceiverSource.ExternalUsb(
            identifier = "usb_cyd_01",
            displayName = "ESP32 CYD Receiver",
            vendorId = 0x10c4,
            productId = 0xea60,
            firmwareVersion = "2.0.4"
        )

        val obs = Observation(
            id = "obs-3",
            timestampEpochMillis = 1710000000002L,
            radioType = RadioType.EXTERNAL_RADIO,
            receiverSource = usbSource,
            rawAddress = "12:34:56:78:90:AB",
            signalStrength = SignalStrength(-45)
        )

        assertThat(obs.receiverSource).isEqualTo(usbSource)
        assertThat((obs.receiverSource as ReceiverSource.ExternalUsb).firmwareVersion).isEqualTo("2.0.4")
        assertThat(obs.signalStrength.category).isEqualTo(SignalCategory.STRONG)
    }

    @Test
    fun `invalid signal strength throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            SignalStrength(-200)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SignalStrength(50)
        }
    }

    @Test
    fun `blank observation identifiers throw IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            Observation(
                id = "",
                timestampEpochMillis = 1000L,
                radioType = RadioType.BLE,
                receiverSource = ReceiverSource.Internal(),
                rawAddress = "00:11:22:33:44:55",
                signalStrength = SignalStrength(-70)
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            Observation(
                id = "valid-id",
                timestampEpochMillis = 1000L,
                radioType = RadioType.BLE,
                receiverSource = ReceiverSource.Internal(),
                rawAddress = "  ",
                signalStrength = SignalStrength(-70)
            )
        }
    }
}
