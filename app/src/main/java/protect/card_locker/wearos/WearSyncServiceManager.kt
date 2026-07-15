package protect.card_locker.wearos

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import protect.card_locker.preferences.Settings
import protect.card_locker.shared.BluetoothPermissionHelper

class WearSyncPermissionRequester(
    caller: ActivityResultCaller,
    context: Context
) {
    private val context = context.applicationContext
    private val permissionLauncher = caller.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        WearSyncServiceManager.onPermissionResult(this.context, granted)
    }

    fun synchronize() {
        WearSyncServiceManager.synchronize(context) {
            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    fun onWearSyncChanged(enabled: Boolean) {
        WearSyncServiceManager.onWearSyncChanged(context, enabled) {
            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }
}

object WearSyncServiceManager {
    fun synchronize(context: Context, requestPermission: (() -> Unit)? = null) {
        if (!Settings(context).wearSyncEnabled) {
            stop(context)
        } else if (BluetoothPermissionHelper.isBluetoothConnectGranted(context)) {
            start(context)
        } else if (requestPermission != null) {
            requestPermission()
        } else {
            stop(context)
        }
    }

    fun onWearSyncChanged(
        context: Context,
        enabled: Boolean,
        requestPermission: () -> Unit
    ) {
        if (enabled) {
            if (BluetoothPermissionHelper.isBluetoothConnectGranted(context)) {
                start(context)
            } else {
                requestPermission()
            }
        } else {
            stop(context)
        }
    }

    fun onPermissionResult(context: Context, granted: Boolean) {
        if (granted && Settings(context).wearSyncEnabled) {
            start(context)
        } else {
            stop(context)
        }
    }

    private fun start(context: Context) {
        context.startService(Intent(context, BluetoothServerService::class.java))
    }

    private fun stop(context: Context) {
        context.stopService(Intent(context, BluetoothServerService::class.java))
    }
}
