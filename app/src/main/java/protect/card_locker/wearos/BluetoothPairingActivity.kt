package protect.card_locker.wearos

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import protect.card_locker.R
import protect.card_locker.shared.WearBluetoothSecurity

class BluetoothPairingActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DEVICE_ADDRESS = "device_address"
        const val EXTRA_DEVICE_NAME = "device_name"
    }

    private var currentDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showPairingDialog(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        showPairingDialog(intent)
    }

    override fun onDestroy() {
        currentDialog?.dismiss()
        currentDialog = null
        super.onDestroy()
    }

    private fun showPairingDialog(intent: Intent) {
        val address = intent.getStringExtra(EXTRA_DEVICE_ADDRESS) ?: return finish()
        val deviceName = intent.getStringExtra(EXTRA_DEVICE_NAME) ?: address

        currentDialog?.dismiss()
        currentDialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.wear_bt_pairing_title)
            .setMessage(getString(R.string.wear_bt_pairing_message, deviceName))
            .setPositiveButton(R.string.wear_bt_pairing_allow) { _, _ ->
                WearBluetoothSecurity.trustDevice(this, address)
                BluetoothPairingNotificationManager.updateResultNotification(this, address, deviceName, true)
                BluetoothPairingNotificationManager.notifyDevicesChanged(this)
                finish()
            }
            .setNegativeButton(R.string.wear_bt_pairing_block) { _, _ ->
                WearBluetoothSecurity.blockDevice(this, address)
                BluetoothPairingNotificationManager.updateResultNotification(this, address, deviceName, false)
                BluetoothPairingNotificationManager.notifyDevicesChanged(this)
                finish()
            }
            .setOnCancelListener { finish() }
            .setCancelable(true)
            .show()
    }
}
