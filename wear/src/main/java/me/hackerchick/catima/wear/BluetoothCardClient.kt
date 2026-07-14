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
        NO_DEVICE,
        PERMISSION_DENIED,
        BLUETOOTH_DISABLED
    }

    fun fetchCards(context: Context, onResult: (cards: String?, status: FetchStatus) -> Unit) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "BLUETOOTH_CONNECT permission not granted")
            onResult(null, FetchStatus.PERMISSION_DENIED)
            return
        }

        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        if (adapter == null || !adapter.isEnabled) {
            Log.w(TAG, "Bluetooth not available or disabled")
            onResult(null, FetchStatus.BLUETOOTH_DISABLED)
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
                val fetchResult = fetchCardsFromDevice(device, deviceName)
                if (fetchResult.second != FetchStatus.NO_DEVICE) {
                    result = fetchResult.first
                    status = fetchResult.second
                    break
                }
            }

            if (status == FetchStatus.NO_DEVICE) {
                Log.w(TAG, "No Catima phone found among bonded devices")
            }
            onResult(result, status)
        }.start()
    }

    private fun fetchCardsFromDevice(
        device: android.bluetooth.BluetoothDevice,
        deviceName: String
    ): Pair<String?, FetchStatus> {
        var socket: BluetoothSocket? = null
        return try {
            socket = device.createRfcommSocketToServiceRecord(WearProtocol.BT_SERVICE_UUID)
            socket.connect()
            Log.d(TAG, "Connected to $deviceName, fetching page 0 to discover total")

            val allCards = JSONArray()
            var pageIndex = 0
            var totalPages = 1

            while (pageIndex < totalPages) {
                if (pageIndex > 0) {
                    try { socket?.close() } catch (_: Exception) { }
                    socket = device.createRfcommSocketToServiceRecord(WearProtocol.BT_SERVICE_UUID)
                    socket.connect()
                    Log.d(TAG, "Reconnected to $deviceName for page $pageIndex")
                }

                val writer = PrintWriter(OutputStreamWriter(socket!!.outputStream, "UTF-8"), false)
                val reader = BufferedReader(InputStreamReader(socket.inputStream, "UTF-8"))

                writer.print("${WearProtocol.BT_CMD_CARDS_PAGE_PREFIX}$pageIndex\n")
                writer.flush()

                val json = reader.readLine()?.trim() ?: ""
                if (json.isEmpty()) {
                    Log.w(TAG, "Empty response for page $pageIndex from $deviceName")
                    return null to FetchStatus.NO_DEVICE
                }

                val parsed = parsePageResponse(json, pageIndex)
                when (parsed.second) {
                    FetchStatus.SUCCESS -> {
                        val pageCards = JSONArray(parsed.first!!)
                        for (i in 0 until pageCards.length()) {
                            allCards.put(pageCards.getJSONObject(i))
                        }
                        if (pageIndex == 0) {
                            totalPages = parsed.third
                            Log.d(TAG, "Total pages: $totalPages")
                        }
                        pageIndex++
                    }
                    FetchStatus.PHONE_OUTDATED -> return null to FetchStatus.PHONE_OUTDATED
                    FetchStatus.WATCH_OUTDATED -> return null to FetchStatus.WATCH_OUTDATED
                    else -> return null to FetchStatus.NO_DEVICE
                }
            }

            Log.d(TAG, "Fetched ${allCards.length()} cards in $totalPages page(s) from $deviceName")
            allCards.toString() to FetchStatus.SUCCESS
        } catch (e: Exception) {
            Log.d(TAG, "Failed to connect to $deviceName: ${e.message}")
            null to FetchStatus.NO_DEVICE
        } finally {
            try { socket?.close() } catch (_: Exception) { }
        }
    }

    private fun parsePageResponse(json: String, expectedPage: Int): Triple<String?, FetchStatus, Int> {
        return try {
            val obj = JSONObject(json)
            val version = obj.getInt("version")
            if (version != WearProtocol.PROTOCOL_VERSION) {
                Log.w(TAG, "Unsupported protocol version: $version")
                Triple(null, FetchStatus.WATCH_OUTDATED, 1)
            } else {
                val cards = obj.getJSONArray("cards").toString()
                val totalPages = obj.optInt("totalPages", 1)
                Triple(cards, FetchStatus.SUCCESS, totalPages)
            }
        } catch (_: Exception) {
            try {
                JSONArray(json)
                Log.w(TAG, "Phone app returned a legacy response; update Catima on your phone")
                Triple(null, FetchStatus.PHONE_OUTDATED, 1)
            } catch (_: Exception) {
                Log.w(TAG, "Unparseable response from phone for page $expectedPage")
                Triple(null, FetchStatus.NO_DEVICE, 1)
            }
        }
    }
}
