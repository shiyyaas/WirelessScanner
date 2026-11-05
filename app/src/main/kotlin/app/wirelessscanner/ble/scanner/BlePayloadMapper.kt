package app.wirelessscanner.ble.scanner

import app.wirelessscanner.core.model.Observation
import app.wirelessscanner.core.model.RadioType
import app.wirelessscanner.core.model.ReceiverSource
import app.wirelessscanner.core.model.SignalStrength
import java.util.UUID

/**
 * Common abstraction representing raw or parsed BLE advertisement data for mapping and testing.
 */
interface BleAdvertisementPayload {
    val address: String
    val advertisedName: String?
    val cachedSystemName: String?
    val rssi: Int
    val rawBytes: ByteArray?
    val txPower: Int?
    val isConnectable: Boolean?
    val serviceUuids: List<String>
    val manufacturerData: Map<Int, ByteArray>
    val serviceData: Map<String, ByteArray>
    val addressType: String?
}

/**
 * Data holder for testing mapper logic without requiring Android framework mocks.
 */
data class TestBlePayload(
    override val address: String,
    override val advertisedName: String? = null,
    override val cachedSystemName: String? = null,
    override val rssi: Int = -70,
    override val rawBytes: ByteArray? = null,
    override val txPower: Int? = null,
    override val isConnectable: Boolean? = null,
    override val serviceUuids: List<String> = emptyList(),
    override val manufacturerData: Map<Int, ByteArray> = emptyMap(),
    override val serviceData: Map<String, ByteArray> = emptyMap(),
    override val addressType: String? = "UNKNOWN"
) : BleAdvertisementPayload

object BlePayloadMapper {
    fun map(payload: BleAdvertisementPayload, epochTimestamp: Long = System.currentTimeMillis()): Observation {
        val metadata = mutableMapOf<String, String>()

        payload.cachedSystemName?.let { metadata["cachedSystemName"] = it }
        payload.addressType?.let { metadata["addressType"] = it }
        payload.txPower?.let { metadata["txPowerLevel"] = it.toString() }
        payload.isConnectable?.let { metadata["isConnectable"] = it.toString() }

        if (payload.serviceUuids.isNotEmpty()) {
            metadata["serviceUuids"] = payload.serviceUuids.joinToString(",")
        }

        payload.manufacturerData.forEach { (id, bytes) ->
            metadata["mfg_$id"] = bytes.joinToString("") { "%02X".format(it) }
        }

        payload.serviceData.forEach { (uuid, bytes) ->
            metadata["svc_data_$uuid"] = bytes.joinToString("") { "%02X".format(it) }
        }

        val rssiDbm = payload.rssi.coerceIn(SignalStrength.VALID_RANGE)

        return Observation(
            id = UUID.randomUUID().toString(),
            timestampEpochMillis = epochTimestamp,
            radioType = RadioType.BLE,
            receiverSource = ReceiverSource.Internal(),
            rawAddress = payload.address,
            advertisedName = payload.advertisedName,
            signalStrength = SignalStrength(rssiDbm),
            rawPayload = payload.rawBytes?.toList() ?: emptyList(),
            metadata = metadata
        )
    }
}
