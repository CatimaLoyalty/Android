package protect.card_locker.shared

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.core.content.ContextCompat

object BluetoothPermissionHelper {
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    fun isBluetoothConnectRequired(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    fun isBluetoothConnectGranted(context: Context): Boolean =
        !isBluetoothConnectRequired() ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED

    fun isPostNotificationsGranted(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    fun requestBluetoothConnectIfNeeded(
        context: Context,
        launcher: ActivityResultLauncher<String>,
        onGranted: () -> Unit
    ) {
        if (isBluetoothConnectGranted(context)) {
            onGranted()
        } else {
            launcher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }
}
