package me.hackerchick.catima.wear

import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import protect.card_locker.shared.BluetoothPermissionHelper
import protect.card_locker.shared.WearBluetoothProtocol
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter

object BluetoothCardClient {

    private const val TAG = "CatimaBtClient"

    fun fetchCards(context: Context, onResult: (cards: String?, status: SyncStatus) -> Unit) {
        if (!BluetoothPermissionHelper.isBluetoothConnectGranted(context)) {
            Log.w(TAG, "BLUETOOTH_CONNECT permission not granted")
            onResult(null, SyncStatus.PERMISSION_DENIED)
            return
        }

        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        if (adapter == null || !adapter.isEnabled) {
            Log.w(TAG, "Bluetooth not available or disabled")
            onResult(null, SyncStatus.BLUETOOTH_DISABLED)
            return
        }

        val bondedDevices = try {
            adapter.bondedDevices
        } catch (e: SecurityException) {
            Log.w(TAG, "BLUETOOTH_CONNECT permission rejected", e)
            emptySet<android.bluetooth.BluetoothDevice>()
        }
        if (bondedDevices.isEmpty()) {
            Log.w(TAG, "No bonded devices found")
            onResult(null, SyncStatus.PHONE_NOT_REACHABLE)
            return
        }

        Log.d(TAG, "Trying ${bondedDevices.size} bonded device(s)")

        Thread {
            var result: String? = null
            var status = SyncStatus.PHONE_NOT_REACHABLE
            for (device in bondedDevices) {
                val deviceName = try { device.name } catch (_: SecurityException) { "unknown" }
                Log.d(TAG, "Trying $deviceName")
                val fetchResult = fetchCardsFromDevice(device, deviceName)
                if (fetchResult.second != SyncStatus.PHONE_NOT_REACHABLE) {
                    result = fetchResult.first
                    status = fetchResult.second
                    break
                }
            }

            if (status == SyncStatus.PHONE_NOT_REACHABLE) {
                Log.w(TAG, "Bluetooth sync failed, phone not reachable")
            }
            onResult(result, status)
        }.start()
    }

    private fun fetchCardsFromDevice(
        device: android.bluetooth.BluetoothDevice,
        deviceName: String
    ): Pair<String?, SyncStatus> {
        var socket: BluetoothSocket? = null
        return try {
            socket = device.createRfcommSocketToServiceRecord(WearBluetoothProtocol.BT_SERVICE_UUID)
            socket.connect()
            val supportedVersions = requestSupportedVersions(socket)
                ?: return null to SyncStatus.PHONE_NOT_REACHABLE
            if (WearBluetoothProtocol.PROTOCOL_VERSION !in supportedVersions) {
                Log.w(TAG, "Phone does not support API version ${WearBluetoothProtocol.PROTOCOL_VERSION}")
                return null to SyncStatus.WATCH_OUTDATED
            }
            socket.close()
            socket = null
            Log.d(TAG, "Connected to $deviceName with API version ${WearBluetoothProtocol.PROTOCOL_VERSION}")

            val allCards = JSONArray()
            var pageIndex = 0
            var totalPages = 1

            while (pageIndex < totalPages) {
                socket = device.createRfcommSocketToServiceRecord(WearBluetoothProtocol.BT_SERVICE_UUID)
                socket.connect()
                val writer = PrintWriter(OutputStreamWriter(socket.outputStream, "UTF-8"), false)
                val reader = BufferedReader(InputStreamReader(socket.inputStream, "UTF-8"))

                writer.print("${WearBluetoothProtocol.BT_CMD_CARDS_PAGE_PREFIX}$pageIndex\n")
                writer.flush()

                val json = reader.readLine()?.trim() ?: ""
                if (json.isEmpty()) {
                    Log.w(TAG, "Empty response for page $pageIndex from $deviceName")
                    return null to SyncStatus.PHONE_NOT_REACHABLE
                }

                val parsed = parsePageResponse(json, pageIndex)
                if (parsed.second != SyncStatus.OK) {
                    return null to parsed.second
                }
                val pageCards = JSONArray(parsed.first!!)
                for (i in 0 until pageCards.length()) {
                    allCards.put(pageCards.getJSONObject(i))
                }
                if (pageIndex == 0) {
                    totalPages = parsed.third
                    Log.d(TAG, "Total pages: $totalPages")
                }
                socket.close()
                socket = null
                pageIndex++
            }

            Log.d(TAG, "Fetched ${allCards.length()} cards in $totalPages page(s) from $deviceName")
            allCards.toString() to SyncStatus.OK
        } catch (e: Exception) {
            Log.d(TAG, "Failed to connect to $deviceName: ${e.message}")
            null to SyncStatus.PHONE_NOT_REACHABLE
        } finally {
            try { socket?.close() } catch (_: Exception) { }
        }
    }

    private fun requestSupportedVersions(socket: BluetoothSocket): Set<Int>? {
        val writer = PrintWriter(OutputStreamWriter(socket.outputStream, "UTF-8"), false)
        val reader = BufferedReader(InputStreamReader(socket.inputStream, "UTF-8"))
        writer.print("${WearBluetoothProtocol.BT_CMD_VERSIONS}\n")
        writer.flush()
        val response = reader.readLine()?.trim() ?: return null
        return try {
            val versions = JSONArray(response)
            buildSet {
                for (index in 0 until versions.length()) {
                    add(versions.getInt(index))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Invalid versions response from phone", e)
            null
        }
    }

    private fun parsePageResponse(json: String, expectedPage: Int): Triple<String?, SyncStatus, Int> {
        return try {
            val obj = JSONObject(json)
            val cards = obj.getJSONArray("cards").toString()
            val totalPages = obj.optInt("totalPages", 1)
            Triple(cards, SyncStatus.OK, totalPages)
        } catch (_: Exception) {
            Log.w(TAG, "Unparseable response from phone for page $expectedPage")
            Triple(null, SyncStatus.PHONE_NOT_REACHABLE, 1)
        }
    }
}
