package me.hackerchick.catima.wear

import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONArray
import org.json.JSONObject
import protect.card_locker.shared.BluetoothPermissionHelper
import protect.card_locker.shared.WearBluetoothProtocol
import protect.card_locker.shared.WearBluetoothSecurity
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

        val completed = AtomicBoolean(false)
        val handler = Handler(Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (completed.compareAndSet(false, true)) {
                Log.w(TAG, "Bluetooth card sync timed out")
                onResult(null, SyncStatus.PHONE_NOT_REACHABLE)
            }
        }
        handler.postDelayed(timeoutRunnable, 20000)

        Thread {
            var result: String? = null
            var status = SyncStatus.PHONE_NOT_REACHABLE
            try {
                for (device in bondedDevices) {
                    val deviceName = try { device.name } catch (_: SecurityException) { "unknown" }
                    Log.d(TAG, "Trying $deviceName")
                    val fetchResult = fetchCardsFromDevice(context, device, deviceName)
                    if (fetchResult.second != SyncStatus.PHONE_NOT_REACHABLE) {
                        result = fetchResult.first
                        status = fetchResult.second
                        break
                    }
                }

                if (status == SyncStatus.PHONE_NOT_REACHABLE) {
                    Log.w(TAG, "Bluetooth sync failed, phone not reachable")
                }
            } finally {
                handler.removeCallbacks(timeoutRunnable)
                if (completed.compareAndSet(false, true)) {
                    onResult(result, status)
                }
            }
        }.start()
    }

    private fun fetchCardsFromDevice(
        context: Context,
        device: android.bluetooth.BluetoothDevice,
        deviceName: String
    ): Pair<String?, SyncStatus> {
        var socket: BluetoothSocket? = null
        return try {
            socket = device.createRfcommSocketToServiceRecord(WearBluetoothProtocol.BT_SERVICE_UUID)
            socket.connect()
            val supportedVersions = requestSupportedVersions(socket)
                ?: return null to SyncStatus.PHONE_NOT_REACHABLE
            var majorVersionIsSupported = false
            var mostRecentMinorVersion = -1
            run breaking@{
                supportedVersions.forEach {
                    val supportedVersionParts = it.split('.')
                    if (supportedVersionParts[0] == WearBluetoothProtocol.PROTOCOL_VERSION.toString()) {
                        majorVersionIsSupported = true
                        mostRecentMinorVersion = supportedVersionParts[1].toInt()
                        return@breaking
                    }
                }
            }
            if (!majorVersionIsSupported) {
                Log.w(TAG, "Phone does not support major API version ${WearBluetoothProtocol.PROTOCOL_VERSION}")
                return null to SyncStatus.VERSION_INCOMPATIBLE
            }
            socket.close()
            socket = null
            Log.d(TAG, "Connected to $deviceName with major API version ${WearBluetoothProtocol.PROTOCOL_VERSION}, phone supports up to minor API version ${mostRecentMinorVersion}, we can use up to ${WearBluetoothProtocol.PROTOCOL_MINOR_VERSION}")

            socket = device.createRfcommSocketToServiceRecord(WearBluetoothProtocol.BT_SERVICE_UUID)
            socket.connect()

            val writer = PrintWriter(OutputStreamWriter(socket.outputStream, "UTF-8"), false)
            val reader = BufferedReader(InputStreamReader(socket.inputStream, "UTF-8"))

            val token = getOrCreateToken(context, device.address)

            val allCards = JSONArray()
            var pageIndex = 0
            var totalPages = 1

            while (pageIndex < totalPages) {
                val command = "${WearBluetoothProtocol.BT_CMD_CARDS_PAGE_PREFIX}$pageIndex"
                writer.println(command)
                sendToken(writer, token)
                writer.flush()

                val response = reader.readLine()?.trim() ?: ""
                if (response.isEmpty()) {
                    Log.w(TAG, "Empty response for page $pageIndex from $deviceName")
                    return null to SyncStatus.PHONE_NOT_REACHABLE
                }

                if (response == WearBluetoothProtocol.BT_RESPONSE_NOT_AUTHORIZED) {
                    Log.w(TAG, "Device $deviceName not authorized on phone")
                    return null to SyncStatus.UNAUTHORIZED
                }

                val parsed = parsePageResponse(response, pageIndex)
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
                pageIndex++
            }

            socket.close()
            socket = null

            Log.d(TAG, "Fetched ${allCards.length()} cards in $totalPages page(s) from $deviceName")
            allCards.toString() to SyncStatus.OK
        } catch (e: Exception) {
            Log.d(TAG, "Failed to connect to $deviceName: ${e.message}")
            null to SyncStatus.PHONE_NOT_REACHABLE
        } finally {
            try { socket?.close() } catch (_: Exception) { }
        }
    }

    private fun getOrCreateToken(context: Context, address: String): String {
        return WearBluetoothSecurity.getDeviceToken(context, address)
            ?: WearBluetoothSecurity.generateToken().also {
                WearBluetoothSecurity.setDeviceToken(context, address, it)
            }
    }

    private fun sendToken(writer: PrintWriter, token: String) {
        writer.println("${WearBluetoothProtocol.BT_CMD_TOKEN_PREFIX}$token")
    }

    private fun requestSupportedVersions(socket: BluetoothSocket): Set<String>? {
        val writer = PrintWriter(OutputStreamWriter(socket.outputStream, "UTF-8"), false)
        val reader = BufferedReader(InputStreamReader(socket.inputStream, "UTF-8"))
        writer.print("${WearBluetoothProtocol.BT_CMD_VERSIONS}\n")
        writer.flush()
        val response = reader.readLine()?.trim() ?: return null
        return try {
            val versions = JSONArray(response)
            buildSet {
                for (index in 0 until versions.length()) {
                    add(versions.getString(index))
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
