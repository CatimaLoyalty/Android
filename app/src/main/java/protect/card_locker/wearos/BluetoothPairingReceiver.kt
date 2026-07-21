package protect.card_locker.wearos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import protect.card_locker.shared.WearBluetoothSecurity

class BluetoothPairingReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val address = intent.getStringExtra(EXTRA_DEVICE_ADDRESS) ?: return
        val deviceName = intent.getStringExtra(EXTRA_DEVICE_NAME) ?: address

        when (intent.action) {
            ACTION_ALLOW -> {
                WearBluetoothSecurity.trustDevice(context, address)
                BluetoothPairingNotificationManager.updateResultNotification(context, address, deviceName, true)
                BluetoothPairingNotificationManager.notifyDevicesChanged(context)
            }
            ACTION_BLOCK -> {
                WearBluetoothSecurity.blockDevice(context, address)
                BluetoothPairingNotificationManager.updateResultNotification(context, address, deviceName, false)
                BluetoothPairingNotificationManager.notifyDevicesChanged(context)
            }
        }
    }

    companion object {
        const val ACTION_ALLOW = "protect.card_locker.wearos.action.ALLOW_DEVICE"
        const val ACTION_BLOCK = "protect.card_locker.wearos.action.BLOCK_DEVICE"
        const val EXTRA_DEVICE_ADDRESS = "device_address"
        const val EXTRA_DEVICE_NAME = "device_name"
    }
}
