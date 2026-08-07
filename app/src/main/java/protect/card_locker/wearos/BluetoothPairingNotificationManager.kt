package protect.card_locker.wearos

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import protect.card_locker.NotificationInfo
import protect.card_locker.R

object BluetoothPairingNotificationManager {

    const val ACTION_DEVICES_CHANGED = "protect.card_locker.wearos.ACTION_DEVICES_CHANGED"

    fun notifyDevicesChanged(context: Context) {
        context.sendBroadcast(Intent(ACTION_DEVICES_CHANGED).apply { setPackage(context.packageName) })
    }

    fun showAuthorizationNotification(
        context: Context,
        deviceName: String,
        address: String,
        token: String,
        isMismatch: Boolean = false
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NotificationInfo.WearBluetooth.PAIRING_CHANNEL_ID,
                context.getString(R.string.wear_bt_pairing_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

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

        val allowIntent = Intent(context, BluetoothPairingReceiver::class.java).apply {
            action = BluetoothPairingReceiver.ACTION_ALLOW
            putExtra(BluetoothPairingConstants.DEVICE_ADDRESS, address)
            putExtra(BluetoothPairingConstants.DEVICE_NAME, deviceName)
            putExtra(BluetoothPairingConstants.DEVICE_TOKEN, token)
        }
        val blockIntent = Intent(context, BluetoothPairingReceiver::class.java).apply {
            action = BluetoothPairingReceiver.ACTION_BLOCK
            putExtra(BluetoothPairingConstants.DEVICE_ADDRESS, address)
            putExtra(BluetoothPairingConstants.DEVICE_NAME, deviceName)
            putExtra(BluetoothPairingConstants.IS_MISMATCH, isMismatch)
        }

        val allowPendingIntent = PendingIntent.getBroadcast(
            context,
            address.hashCode(),
            allowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val blockPendingIntent = PendingIntent.getBroadcast(
            context,
            address.hashCode() + 1,
            blockIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentIntent = PendingIntent.getActivity(
            context,
            address.hashCode() + 2,
            Intent(context, BluetoothPairingActivity::class.java).apply {
                putExtra(BluetoothPairingConstants.DEVICE_ADDRESS, address)
                putExtra(BluetoothPairingConstants.DEVICE_NAME, deviceName)
                putExtra(BluetoothPairingConstants.DEVICE_TOKEN, token)
                putExtra(BluetoothPairingConstants.IS_MISMATCH, isMismatch)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationInfo.WearBluetooth.PAIRING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_catima)
            .setContentTitle(context.getString(titleRes, deviceName))
            .setContentText(context.getString(messageRes, deviceName))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(messageRes, deviceName)))
            .addAction(0, context.getString(R.string.wear_bt_pairing_allow), allowPendingIntent)
            .addAction(0, context.getString(R.string.wear_bt_pairing_block), blockPendingIntent)
            .setContentIntent(contentIntent)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        notificationManager.notify(notificationId(address), notification)
    }

    fun cancel(context: Context, address: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId(address))
    }

    fun updateResultNotification(context: Context, address: String, deviceName: String, allowed: Boolean) {
        cancel(context, address)
        val message = if (allowed) {
            context.getString(R.string.wear_bt_pairing_allowed, deviceName)
        } else {
            context.getString(R.string.wear_bt_pairing_blocked, deviceName)
        }

        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun notificationId(address: String): Int = address.hashCode()
}
