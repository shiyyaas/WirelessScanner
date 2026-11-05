package app.wirelessscanner.core.model

/**
 * Type-safe representation of received radio signal strength indicator (RSSI).
 *
 * @property dbm The raw RSSI measured in dBm (typically between -120 and 0 for BLE/Wi-Fi).
 */
@JvmInline
value class SignalStrength(val dbm: Int) : Comparable<SignalStrength> {

    init {
        require(dbm in VALID_RANGE) {
            "Signal strength dBm ($dbm) is outside plausible physical range ($VALID_RANGE)"
        }
    }

    override fun compareTo(other: SignalStrength): Int = this.dbm.compareTo(other.dbm)

    /**
     * Approximate categorization of signal strength for presentation and filtering.
     * Note: This does NOT represent physical distance or proximity.
     */
    val category: SignalCategory
        get() = when {
            dbm >= -60 -> SignalCategory.STRONG
            dbm >= -80 -> SignalCategory.MEDIUM
            else -> SignalCategory.WEAK
        }

    companion object {
        val VALID_RANGE = -140..20
    }
}

enum class SignalCategory {
    STRONG,
    MEDIUM,
    WEAK
}
