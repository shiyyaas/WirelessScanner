package app.wirelessscanner.core.model

/**
 * Distinguishes the receiver hardware capturing the wireless observation.
 * Preserves observation provenance between internal handset radios and external peripherals.
 */
sealed interface ReceiverSource {
    val identifier: String
    val displayName: String

    /**
     * An internal radio receiver hosted directly on the Android smartphone or tablet.
     */
    data class Internal(
        override val identifier: String = "internal_host",
        override val displayName: String = "Internal Device Radio"
    ) : ReceiverSource

    /**
     * An external peripheral receiver (such as an ESP32 CYD or USB receiver module)
     * connected via USB OTG or serial interface.
     */
    data class ExternalUsb(
        override val identifier: String,
        override val displayName: String,
        val vendorId: Int? = null,
        val productId: Int? = null,
        val firmwareVersion: String? = null
    ) : ReceiverSource
}
