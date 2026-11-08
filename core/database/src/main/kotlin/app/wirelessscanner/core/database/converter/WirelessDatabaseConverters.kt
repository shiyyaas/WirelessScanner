package app.wirelessscanner.core.database.converter

import androidx.room.TypeConverter
import app.wirelessscanner.core.model.RadioType
import app.wirelessscanner.core.model.ReceiverSource
import app.wirelessscanner.core.model.SignalStrength

class WirelessDatabaseConverters {

    @TypeConverter
    fun fromRadioType(value: RadioType): String = value.name

    @TypeConverter
    fun toRadioType(value: String): RadioType = RadioType.valueOf(value)

    @TypeConverter
    fun fromReceiverSource(source: ReceiverSource): String {
        return when (source) {
            is ReceiverSource.Internal -> "INTERNAL:${source.identifier}:${source.displayName}"
            is ReceiverSource.ExternalUsb -> "USB:${source.identifier}:${source.displayName}:${source.vendorId ?: -1}:${source.productId ?: -1}:${source.firmwareVersion ?: ""}"
        }
    }

    @TypeConverter
    fun toReceiverSource(value: String): ReceiverSource {
        val parts = value.split(":")
        return if (parts.firstOrNull() == "USB") {
            ReceiverSource.ExternalUsb(
                identifier = parts.getOrNull(1) ?: "unknown_usb",
                displayName = parts.getOrNull(2) ?: "External USB Receiver",
                vendorId = parts.getOrNull(3)?.toIntOrNull()?.takeIf { it != -1 },
                productId = parts.getOrNull(4)?.toIntOrNull()?.takeIf { it != -1 },
                firmwareVersion = parts.getOrNull(5)?.takeIf { it.isNotEmpty() }
            )
        } else {
            ReceiverSource.Internal(
                identifier = parts.getOrNull(1) ?: "internal_host",
                displayName = parts.getOrNull(2) ?: "Internal Device Radio"
            )
        }
    }

    @TypeConverter
    fun fromSignalStrength(signal: SignalStrength): Int = signal.dbm

    @TypeConverter
    fun toSignalStrength(dbm: Int): SignalStrength = SignalStrength(dbm)

    @TypeConverter
    fun fromByteList(bytes: List<Byte>): ByteArray = bytes.toByteArray()

    @TypeConverter
    fun toByteList(bytes: ByteArray): List<Byte> = bytes.toList()

    @TypeConverter
    fun fromStringMap(map: Map<String, String>): String {
        return map.entries.joinToString(";") { "${it.key}=${it.value}" }
    }

    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        if (value.isBlank()) return emptyMap()
        return value.split(";")
            .mapNotNull { entry ->
                val kv = entry.split("=", limit = 2)
                if (kv.size == 2) kv[0] to kv[1] else null
            }
            .toMap()
    }
}
