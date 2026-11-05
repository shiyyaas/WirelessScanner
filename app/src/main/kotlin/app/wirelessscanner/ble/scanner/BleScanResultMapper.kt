package app.wirelessscanner.ble.scanner

import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.os.Build
import app.wirelessscanner.core.model.Observation
import app.wirelessscanner.core.model.RadioType
import app.wirelessscanner.core.model.ReceiverSource
import app.wirelessscanner.core.model.SignalStrength
import java.util.UUID

/**
 * Maps Android [ScanResult] instances into domain [Observation] records.
 *
 * Requirements:
 * - Keeps raw byte payloads strictly immutable (`List<Byte>`).
 * - Accurately records receiver source as [ReceiverSource.Internal].
 * - Distinguishes advertised local name (`ScanRecord.deviceName`) from cached system name (`BluetoothDevice.name`).
 * - Extracts Tx power, service UUIDs, manufacturer data, service data, and PHY metadata.
 * - Generates unique observation IDs while preserving original MAC address.
 */
object BleScanResultMapper {

    fun map(
        scanResult: ScanResult,
        epochTimestamp: Long = System.currentTimeMillis()
    ): Observation {
        val device = scanResult.device
        val scanRecord: ScanRecord? = scanResult.scanRecord

        val rawAddress = device.address ?: "00:00:00:00:00:00"
        val advertisedName = scanRecord?.deviceName?.takeIf { it.isNotBlank() }

        // Cached Bluetooth system name is accessed safely. Note that accessing device.name
        // on Android 12+ may require BLUETOOTH_CONNECT if not already cached. We suppress
        // SecurityException if thrown so scanning is never interrupted.
        val cachedSystemName = try {
            @Suppress("MissingPermission")
            device.name?.takeIf { it.isNotBlank() }
        } catch (_: SecurityException) {
            null
        }

        // Clamp RSSI to SignalStrength valid range (-140..20)
        val rssiDbm = scanResult.rssi.coerceIn(SignalStrength.VALID_RANGE)
        val signalStrength = SignalStrength(rssiDbm)

        val metadata = mutableMapOf<String, String>()

        cachedSystemName?.let { metadata["cachedSystemName"] = it }

        // Address type (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val addrType = scanResult.javaClass.getMethod("getAddressType").invoke(scanResult) as? Int
                when (addrType) {
                    1 -> metadata["addressType"] = "RANDOM"
                    0 -> metadata["addressType"] = "PUBLIC"
                    else -> metadata["addressType"] = "UNKNOWN"
                }
            } catch (_: Throwable) {
                metadata["addressType"] = "UNKNOWN"
            }
        } else {
            metadata["addressType"] = "UNKNOWN"
        }

        // Connectable flag
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            metadata["isConnectable"] = scanResult.isConnectable.toString()
        }

        // Tx Power level
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val txPower = scanResult.txPower
            if (txPower != ScanResult.TX_POWER_NOT_PRESENT) {
                metadata["txPowerLevel"] = txPower.toString()
            }
        } else if (scanRecord != null && scanRecord.txPowerLevel != Int.MIN_VALUE) {
            metadata["txPowerLevel"] = scanRecord.txPowerLevel.toString()
        }

        // Primary / Secondary PHY (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            metadata["primaryPhy"] = scanResult.primaryPhy.toString()
            metadata["secondaryPhy"] = scanResult.secondaryPhy.toString()
        }

        // Periodic advertising interval (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            metadata["periodicAdvertisingInterval"] = scanResult.periodicAdvertisingInterval.toString()
        }

        // Service UUIDs
        val serviceUuids = scanRecord?.serviceUuids?.mapNotNull { it.uuid?.toString() } ?: emptyList()
        if (serviceUuids.isNotEmpty()) {
            metadata["serviceUuids"] = serviceUuids.joinToString(",")
        }

        // Manufacturer Specific Data
        scanRecord?.manufacturerSpecificData?.let { mfgSparseArray ->
            for (i in 0 until mfgSparseArray.size()) {
                val id = mfgSparseArray.keyAt(i)
                val bytes = mfgSparseArray.valueAt(i)
                if (bytes != null) {
                    metadata["mfg_$id"] = bytesToHexString(bytes)
                }
            }
        }

        // Service Data
        scanRecord?.serviceData?.forEach { (parcelUuid, bytes) ->
            if (parcelUuid != null && bytes != null) {
                metadata["svc_data_${parcelUuid.uuid}"] = bytesToHexString(bytes)
            }
        }

        // Advertisement Flags
        scanRecord?.advertiseFlags?.let { flags ->
            if (flags != -1) {
                metadata["advertiseFlags"] = flags.toString()
            }
        }

        val rawBytes = scanRecord?.bytes?.toList() ?: emptyList()

        return Observation(
            id = UUID.randomUUID().toString(),
            timestampEpochMillis = epochTimestamp,
            radioType = RadioType.BLE,
            receiverSource = ReceiverSource.Internal(),
            rawAddress = rawAddress,
            advertisedName = advertisedName,
            signalStrength = signalStrength,
            rawPayload = rawBytes,
            metadata = metadata
        )
    }

    private fun bytesToHexString(bytes: ByteArray): String =
        bytes.joinToString("") { "%02X".format(it) }
}
