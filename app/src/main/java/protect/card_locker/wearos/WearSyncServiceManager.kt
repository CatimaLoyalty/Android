package protect.card_locker.wearos

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import protect.card_locker.R
import protect.card_locker.preferences.Settings
import protect.card_locker.shared.BluetoothPermissionHelper

class WearSyncPermissionRequester(
    caller: ActivityResultCaller,
    context: Context
) {
    private val context = context.applicationContext
    private var pendingResultCallback: ((Boolean) -> Unit)? = null

    private val permissionLauncher = caller.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingResultCallback?.invoke(granted)
        WearSyncServiceManager.onPermissionResult(this.context, granted)
        pendingResultCallback = null
    }

    fun synchronize() {
        WearSyncServiceManager.synchronize(context) {
            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    fun onWearSyncChanged(enabled: Boolean, onPermissionResult: ((Boolean) -> Unit)? = null) {
        val applied = WearSyncServiceManager.onWearSyncChanged(context, enabled) {
            pendingResultCallback = onPermissionResult
            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (applied) {
            onPermissionResult?.invoke(true)
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
    ): Boolean {
        return if (enabled) {
            if (BluetoothPermissionHelper.isBluetoothConnectGranted(context)) {
                start(context)
                true
            } else {
                requestPermission()
                false
            }
        } else {
            stop(context)
            true
        }
    }

    fun onPermissionResult(context: Context, granted: Boolean) {
        if (granted && Settings(context).wearSyncEnabled) {
            start(context)
        } else {
            stop(context)
            if (!granted) {
                PreferenceManager.getDefaultSharedPreferences(context).edit {
                    putBoolean(context.getString(R.string.settings_key_wear_sync), false)
                }
            }
        }
    }

    private fun start(context: Context) {
        context.startService(Intent(context, BluetoothServerService::class.java))
    }

    private fun stop(context: Context) {
        context.stopService(Intent(context, BluetoothServerService::class.java))
    }
}
