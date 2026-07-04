package me.hackerchick.catima.wear

import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter

object BluetoothCardClient {

    private const val TAG = "CatimaBtClient"

    enum class FetchStatus {
        SUCCESS,
        PHONE_OUTDATED,
        WATCH_OUTDATED,
        NO_DEVICE
    }

    fun fetchCards(context: Context, onResult: (cards: String?, status: FetchStatus) -> Unit) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "BLUETOOTH_CONNECT permission not granted")
            onResult(null, FetchStatus.NO_DEVICE)
            return
        }

        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        if (adapter == null || !adapter.isEnabled) {
            Log.w(TAG, "Bluetooth not available or disabled")
            onResult(null, FetchStatus.NO_DEVICE)
            return
        }

        val bondedDevices = adapter.bondedDevices
        if (bondedDevices.isEmpty()) {
            Log.w(TAG, "No bonded devices found")
            onResult(null, FetchStatus.NO_DEVICE)
            return
        }

        Log.d(TAG, "Trying ${bondedDevices.size} bonded device(s)")

        Thread {
            var result: String? = null
            var status = FetchStatus.NO_DEVICE
            for (device in bondedDevices) {
                val deviceName = try { device.name } catch (_: Exception) { "unknown" }
                Log.d(TAG, "Trying $deviceName")
                var socket: BluetoothSocket? = null
                try {
                    socket = device.createRfcommSocketToServiceRecord(WearProtocol.BT_SERVICE_UUID)
                    socket.connect()
                    Log.d(TAG, "Connected to $deviceName")

                    val writer = PrintWriter(OutputStreamWriter(socket.outputStream, "UTF-8"), false)
                    val reader = BufferedReader(InputStreamReader(socket.inputStream, "UTF-8"))

                    writer.print(WearProtocol.BT_CMD_CARDS_REQUEST_V1)
                    writer.flush()

                    val json = reader.readLine()?.trim() ?: ""
                    if (json.isNotEmpty()) {
                        Log.d(TAG, "Received ${json.length} bytes from $deviceName")
                        val parsed = parseResponse(json)
                        result = parsed.first
                        status = parsed.second
                        break
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Failed to connect to $deviceName: ${e.message}")
                } finally {
                    try { socket?.close() } catch (_: Exception) { }
                }
            }

            if (status == FetchStatus.NO_DEVICE) {
                Log.w(TAG, "No Catima phone found among bonded devices")
            }
            onResult(result, status)
        }.start()
    }

    private fun parseResponse(json: String): Pair<String?, FetchStatus> {
        return try {
            val obj = JSONObject(json)
            val version = obj.getInt("version")
            if (version != WearProtocol.PROTOCOL_VERSION) {
                Log.w(TAG, "Unsupported protocol version: $version")
                null to FetchStatus.WATCH_OUTDATED
            } else {
                val cards = obj.getJSONArray("cards").toString()
                cards to FetchStatus.SUCCESS
            }
        } catch (_: Exception) {
            try {
                JSONArray(json)
                Log.w(TAG, "Phone app returned a legacy response; update Catima on your phone")
                null to FetchStatus.PHONE_OUTDATED
            } catch (_: Exception) {
                Log.w(TAG, "Unparseable response from phone")
                null to FetchStatus.NO_DEVICE
            }
        }
    }
}
