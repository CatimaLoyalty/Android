package protect.card_locker.wearos

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
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
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.all { it.value }
        pendingResultCallback?.invoke(allGranted)
        WearSyncServiceManager.onPermissionResult(this.context, allGranted)
        pendingResultCallback = null
    }

    fun synchronize() {
        WearSyncServiceManager.synchronize(context) {
            permissionLauncher.launch(getRequiredPermissions())
        }
    }

    fun onWearSyncChanged(enabled: Boolean, onPermissionResult: ((Boolean) -> Unit)? = null) {
        val applied = WearSyncServiceManager.onWearSyncChanged(context, enabled) {
            pendingResultCallback = onPermissionResult
            permissionLauncher.launch(getRequiredPermissions())
        }
        if (applied) {
            onPermissionResult?.invoke(true)
        }
    }

    private fun getRequiredPermissions(): Array<String> = buildList {
        add(Manifest.permission.BLUETOOTH_CONNECT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()
}

object WearSyncServiceManager {
    fun synchronize(context: Context, requestPermission: (() -> Unit)? = null) {
        if (!Settings(context).wearSyncEnabled) {
            stop(context)
        } else if (arePermissionsGranted(context)) {
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
            if (arePermissionsGranted(context)) {
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

    private fun arePermissionsGranted(context: Context): Boolean {
        return BluetoothPermissionHelper.isBluetoothConnectGranted(context) &&
            BluetoothPermissionHelper.isPostNotificationsGranted(context)
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
        ContextCompat.startForegroundService(context, Intent(context, BluetoothServerService::class.java))
    }

    private fun stop(context: Context) {
        context.stopService(Intent(context, BluetoothServerService::class.java))
    }
}
