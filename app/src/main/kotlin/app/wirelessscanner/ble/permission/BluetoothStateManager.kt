package app.wirelessscanner.ble.permission

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Hardware state of the device's Bluetooth adapter.
 */
sealed interface BluetoothHardwareState {
    data object Unsupported : BluetoothHardwareState
    data object Disabled : BluetoothHardwareState
    data object Enabled : BluetoothHardwareState
    data object TurningOn : BluetoothHardwareState
    data object TurningOff : BluetoothHardwareState
}

interface BluetoothStateObserver {
    val currentState: BluetoothHardwareState
    fun observeState(): Flow<BluetoothHardwareState>
}

/**
 * Monitors and emits Bluetooth adapter state changes.
 */
class BluetoothStateManager(
    private val context: Context
) : BluetoothStateObserver {
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        manager?.adapter
    }

    override val currentState: BluetoothHardwareState
        get() {
            val adapter = bluetoothAdapter ?: return BluetoothHardwareState.Unsupported
            return if (adapter.isEnabled) {
                BluetoothHardwareState.Enabled
            } else {
                BluetoothHardwareState.Disabled
            }
        }

    override fun observeState(): Flow<BluetoothHardwareState> = callbackFlow {
        if (bluetoothAdapter == null) {
            trySend(BluetoothHardwareState.Unsupported)
            close()
            return@callbackFlow
        }

        // Send initial state
        trySend(currentState)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    val newState = when (state) {
                        BluetoothAdapter.STATE_ON -> BluetoothHardwareState.Enabled
                        BluetoothAdapter.STATE_OFF -> BluetoothHardwareState.Disabled
                        BluetoothAdapter.STATE_TURNING_ON -> BluetoothHardwareState.TurningOn
                        BluetoothAdapter.STATE_TURNING_OFF -> BluetoothHardwareState.TurningOff
                        else -> currentState
                    }
                    trySend(newState)
                }
            }
        }

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(receiver, filter)

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: IllegalArgumentException) {
            }
        }
    }.distinctUntilChanged()
}
