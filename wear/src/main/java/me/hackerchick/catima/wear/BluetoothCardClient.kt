package me.hackerchick.catima.wear

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter

object BluetoothCardClient {

    private const val TAG = "CatimaBtClient"

    fun fetchCards(context: Context, onResult: (cards: String?) -> Unit) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "BLUETOOTH_CONNECT permission not granted")
            onResult(null)
            return
        }

        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        if (adapter == null || !adapter.isEnabled) {
            Log.w(TAG, "Bluetooth not available or disabled")
            onResult(null)
            return
        }

        val bondedDevices = adapter.bondedDevices
        if (bondedDevices.isEmpty()) {
            Log.w(TAG, "No bonded devices found")
            onResult(null)
            return
        }

        Log.d(TAG, "Trying ${bondedDevices.size} bonded device(s)")

        Thread {
            var result: String? = null
            for (device in bondedDevices) {
                val deviceName = try { device.name } catch (e: Exception) { "unknown" }
                Log.d(TAG, "Trying $deviceName")
                var socket: BluetoothSocket? = null
                try {
                    socket = device.createRfcommSocketToServiceRecord(WearProtocol.BT_SERVICE_UUID)
                    socket.connect()
                    Log.d(TAG, "Connected to $deviceName")

                    val writer = PrintWriter(socket.outputStream, false, Charsets.UTF_8)
                    val reader = BufferedReader(InputStreamReader(socket.inputStream, Charsets.UTF_8))

                    writer.print(WearProtocol.BT_CMD_CARDS_REQUEST)
                    writer.flush()

                    val json = reader.readLine()?.trim() ?: ""
                    if (json.isNotEmpty()) {
                        Log.d(TAG, "Received ${json.length} bytes from $deviceName")
                        result = json
                        break
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Failed to connect to $deviceName: ${e.message}")
                } finally {
                    try { socket?.close() } catch (e: Exception) { }
                }
            }

            if (result == null) {
                Log.w(TAG, "No Catima phone found among bonded devices")
            }
            onResult(result)
        }.start()
    }
}
