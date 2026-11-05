package app.wirelessscanner.core.model

/**
 * Identifies the physical radio interface or protocol family of an observation.
 */
enum class RadioType {
    BLE,
    WIFI,
    CELLULAR,
    EXTERNAL_RADIO
}
