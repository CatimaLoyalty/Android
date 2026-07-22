package protect.card_locker.wearos

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import org.json.JSONArray
import org.json.JSONObject
import protect.card_locker.DBHelper
import protect.card_locker.NotificationInfo
import protect.card_locker.R
import protect.card_locker.Utils
import protect.card_locker.shared.BluetoothPermissionHelper
import protect.card_locker.shared.WearBluetoothProtocol
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter

class BluetoothServerService : Service() {

    companion object {
        private const val TAG = "CatimaBtServer"
        private const val NOTIFICATION_ID = NotificationInfo.WearBluetooth.NOTIFICATION_ID
        private const val CHANNEL_ID = NotificationInfo.WearBluetooth.CHANNEL_ID
    }

    private var serverThread: AcceptThread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!BluetoothPermissionHelper.isBluetoothConnectGranted(this)) {
            Log.w(TAG, "BLUETOOTH_CONNECT permission not granted, stopping")
            stopSelf()
            return START_NOT_STICKY
        }
        val adapter = (getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        if (adapter == null || !adapter.isEnabled) {
            Log.w(TAG, "Bluetooth not available or disabled")
            stopSelf()
            return START_NOT_STICKY
        }
        startForegroundWithNotification()
        serverThread?.cancel()
        serverThread = AcceptThread(adapter).also { it.start() }
        Log.d(TAG, "Bluetooth server started")
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.wear_bt_channel_name),
                NotificationManager.IMPORTANCE_MIN
            ).apply { setShowBadge(false) }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.wear_bt_notification_title))
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        serverThread?.cancel()
        serverThread = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private inner class AcceptThread(adapter: BluetoothAdapter) : Thread() {
        private var serverSocket: BluetoothServerSocket? = null
        @Volatile private var running = true

        init {
            try {
                if (BluetoothPermissionHelper.isBluetoothConnectGranted(this@BluetoothServerService)) {
                    serverSocket = adapter.listenUsingRfcommWithServiceRecord(
                        WearBluetoothProtocol.BT_SERVICE_NAME,
                        WearBluetoothProtocol.BT_SERVICE_UUID
                    )
                } else {
                    Log.w(TAG, "BLUETOOTH_CONNECT permission missing, cannot open server socket")
                    running = false
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "BLUETOOTH_CONNECT permission missing, cannot create server socket", e)
                running = false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create server socket", e)
                running = false
            }
        }

        override fun run() {
            Log.d(TAG, "Listening for Bluetooth connections")
            while (running) {
                val socket: BluetoothSocket = try {
                    serverSocket?.accept() ?: break
                } catch (e: Exception) {
                    if (running) Log.e(TAG, "Accept failed", e)
                    break
                }
                handleConnection(socket)
            }
            Log.d(TAG, "Accept loop ended")
            if (running) {
                Log.w(TAG, "Accept loop exited unexpectedly, restarting service")
                stopSelf()
            }
        }

        private fun handleConnection(socket: BluetoothSocket) {
            val deviceName = try { socket.remoteDevice.name } catch (_: SecurityException) { "unknown" }
            Log.d(TAG, "Connected to $deviceName")
            try {
                val reader = BufferedReader(InputStreamReader(socket.inputStream, "UTF-8"))
                val writer = PrintWriter(OutputStreamWriter(socket.outputStream, "UTF-8"), false)
                val command = reader.readLine()?.trim()
                Log.d(TAG, "Received command: $command from $deviceName")
                if (command == WearBluetoothProtocol.BT_CMD_VERSIONS) {
                    val versions = JSONArray().put(WearBluetoothProtocol.PROTOCOL_VERSION).toString()
                    writer.println(versions)
                    writer.flush()
                    Log.d(TAG, "Sent supported versions to $deviceName")
                } else if (command != null && command.startsWith("/V1/")) {
                    when {
                        command.startsWith(WearBluetoothProtocol.BT_CMD_CARDS_PAGE_PREFIX) -> {
                            val pageIndex = command.removePrefix(WearBluetoothProtocol.BT_CMD_CARDS_PAGE_PREFIX).toIntOrNull()
                            if (pageIndex == null || pageIndex < 0) {
                                Log.w(TAG, "Invalid page index in command: $command from $deviceName")
                            } else {
                                val json = buildCardsPageJson(pageIndex)
                                writer.println(json)
                                writer.flush()
                                Log.d(TAG, "Sent page $pageIndex (${json.length} bytes) to $deviceName")
                            }
                        }
                        else -> Log.w(TAG, "Unsupported V1 command: $command from $deviceName")
                    }
                } else {
                    Log.w(TAG, "Unsupported protocol version for command: $command from $deviceName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling connection from $deviceName", e)
            } finally {
                try { socket.close() } catch (_: Exception) { }
            }
        }

        private fun buildCardsPageJson(pageIndex: Int): String {
            val dbHelper = DBHelper(this@BluetoothServerService)
            val db = dbHelper.readableDatabase
            val order = Utils.getLoyaltyCardOrder(this@BluetoothServerService)
            val orderDirection = Utils.getLoyaltyCardOrderDirection(this@BluetoothServerService)
            val cursor = DBHelper.getLoyaltyCardCursor(
                db, "", null, order, orderDirection,
                DBHelper.LoyaltyCardArchiveFilter.Unarchived
            )
            val totalCards = cursor.count
            val totalPages = if (totalCards == 0) 1 else (totalCards + WearBluetoothProtocol.PAGE_SIZE - 1) / WearBluetoothProtocol.PAGE_SIZE
            val offset = pageIndex * WearBluetoothProtocol.PAGE_SIZE
            val array = JSONArray()
            cursor.use {
                if (it.move(offset + 1)) {
                    val idIdx = it.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.ID)
                    val storeIdx = it.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.STORE)
                    val cardIdIdx = it.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.CARD_ID)
                    val barcodeIdIdx = it.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BARCODE_ID)
                    val barcodeTypeIdx = it.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BARCODE_TYPE)
                    val headerColorIdx = it.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.HEADER_COLOR)
                    var count = 0
                    do {
                        array.put(JSONObject().apply {
                            put("id", it.getInt(idIdx))
                            put("store", it.getString(storeIdx) ?: "")
                            put("cardId", it.getString(cardIdIdx) ?: "")
                            put("barcodeId", if (it.isNull(barcodeIdIdx)) JSONObject.NULL else it.getString(barcodeIdIdx))
                            put("barcodeType", if (it.isNull(barcodeTypeIdx)) JSONObject.NULL else it.getString(barcodeTypeIdx))
                            put("headerColor", if (it.isNull(headerColorIdx)) JSONObject.NULL else it.getInt(headerColorIdx))
                        })
                        count++
                    } while (count < WearBluetoothProtocol.PAGE_SIZE && it.moveToNext())
                }
            }
            return JSONObject().apply {
                put("page", pageIndex)
                put("totalPages", totalPages)
                put("cards", array)
            }.toString()
        }

        fun cancel() {
            running = false
            try { serverSocket?.close() } catch (_: Exception) { }
        }
    }
}
