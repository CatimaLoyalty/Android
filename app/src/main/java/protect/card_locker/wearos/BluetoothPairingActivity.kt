package protect.card_locker.wearos

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import protect.card_locker.R
import protect.card_locker.shared.WearBluetoothSecurity

class BluetoothPairingActivity : AppCompatActivity() {

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
        val address = intent.getStringExtra(BluetoothPairingConstants.DEVICE_ADDRESS) ?: return finish()
        val deviceName = intent.getStringExtra(BluetoothPairingConstants.DEVICE_NAME) ?: address
        val token = intent.getStringExtra(BluetoothPairingConstants.DEVICE_TOKEN)
        val isMismatch = intent.getBooleanExtra(BluetoothPairingConstants.IS_MISMATCH, false)

        val titleRes = if (isMismatch) {
            R.string.wear_bt_pairing_mismatch_title
        } else {
            R.string.wear_bt_pairing_title
        }
        val messageRes = if (isMismatch) {
            R.string.wear_bt_pairing_mismatch_message
        } else {
            R.string.wear_bt_pairing_message
        }

        currentDialog?.dismiss()
        currentDialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(titleRes, deviceName))
            .setMessage(getString(messageRes, deviceName))
            .setPositiveButton(R.string.wear_bt_pairing_allow) { _, _ ->
                WearBluetoothSecurity.trustDevice(this, address, token)
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
