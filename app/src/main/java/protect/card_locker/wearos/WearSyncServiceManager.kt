package protect.card_locker.wearos

import android.Manifest
import android.app.ForegroundServiceStartNotAllowedException
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import protect.card_locker.NotificationInfo
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
        if (BluetoothPermissionHelper.isBluetoothConnectRequired()) {
            add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (BluetoothPermissionHelper.isPostNotificationsRequired()) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()
}

object WearSyncServiceManager {
    private const val TAG = "CatimaWearSyncServiceManager"

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
        // Exception can only throw on API level 31 and up
        if (Build.VERSION.SDK_INT >= 31) {
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, BluetoothServerService::class.java)
                )
            } catch (e: ForegroundServiceStartNotAllowedException) {
                // We naively assume we have enough permissions to show a notification as the service should never try to start
                // before the user gave notification access anyway
                Log.d(TAG, "Foreground service failed to launch: $e")
                val channel = NotificationChannel(
                    NotificationInfo.WearBluetooth.CRITICAL_ERROR_CHANNEL_ID,
                    context.getString(R.string.wear_bt_critical_error_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { setShowBadge(false) }
                context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
                val notification = NotificationCompat.Builder(context, NotificationInfo.WearBluetooth.CRITICAL_ERROR_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification_error)
                    .setContentTitle(context.getString(R.string.wear_bt_failed_foreground_notification_title))
                    .setContentText(e.message)
                    .build()
                with(NotificationManagerCompat.from(context)) {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return@with
                    }
                    notify(
                        NotificationInfo.WearBluetooth.CRITICAL_ERROR_NOTIFICATION_ID,
                        notification
                    )
                }
            }
        } else {
            ContextCompat.startForegroundService(
                context,
                Intent(context, BluetoothServerService::class.java)
            )
        }
    }

    private fun stop(context: Context) {
        context.stopService(Intent(context, BluetoothServerService::class.java))
    }
}
