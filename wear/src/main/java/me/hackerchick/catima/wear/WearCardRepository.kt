package me.hackerchick.catima.wear

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object WearCardRepository {
    private const val TAG = "WearCardRepository"

    private val _cards = MutableStateFlow<List<WearCard>?>(null)
    val cards: StateFlow<List<WearCard>?> = _cards.asStateFlow()

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    private val _phoneNotReachable = MutableStateFlow(false)
    val phoneNotReachable: StateFlow<Boolean> = _phoneNotReachable.asStateFlow()

    private val _phoneOutdated = MutableStateFlow(false)
    val phoneOutdated: StateFlow<Boolean> = _phoneOutdated.asStateFlow()

    private val _watchOutdated = MutableStateFlow(false)
    val watchOutdated: StateFlow<Boolean> = _watchOutdated.asStateFlow()

    private val _permissionDenied = MutableStateFlow(false)
    val permissionDenied: StateFlow<Boolean> = _permissionDenied.asStateFlow()

    private val _bluetoothDisabled = MutableStateFlow(false)
    val bluetoothDisabled: StateFlow<Boolean> = _bluetoothDisabled.asStateFlow()

    fun loadCache(context: Context) {
        val cached = WearCardStore.load(context)
        if (cached != null && _cards.value == null) {
            _cards.value = cached
            Log.d(TAG, "Loaded ${cached.size} cards from cache")
        }
    }

    fun setSyncing(value: Boolean) {
        _syncing.value = value
        if (value) {
            _phoneNotReachable.value = false
            _phoneOutdated.value = false
            _watchOutdated.value = false
            _permissionDenied.value = false
            _bluetoothDisabled.value = false
        }
    }

    fun setPhoneNotReachable() {
        _syncing.value = false
        _phoneNotReachable.value = true
        _phoneOutdated.value = false
        _watchOutdated.value = false
        _permissionDenied.value = false
        _bluetoothDisabled.value = false
    }

    fun setPhoneOutdated() {
        _syncing.value = false
        _phoneNotReachable.value = false
        _phoneOutdated.value = true
        _watchOutdated.value = false
        _permissionDenied.value = false
        _bluetoothDisabled.value = false
    }

    fun setWatchOutdated() {
        _syncing.value = false
        _phoneNotReachable.value = false
        _phoneOutdated.value = false
        _watchOutdated.value = true
        _permissionDenied.value = false
        _bluetoothDisabled.value = false
    }

    fun setPermissionDenied() {
        _syncing.value = false
        _phoneNotReachable.value = false
        _phoneOutdated.value = false
        _watchOutdated.value = false
        _permissionDenied.value = true
        _bluetoothDisabled.value = false
    }

    fun setBluetoothDisabled() {
        _syncing.value = false
        _phoneNotReachable.value = false
        _phoneOutdated.value = false
        _watchOutdated.value = false
        _permissionDenied.value = false
        _bluetoothDisabled.value = true
    }

    fun updateCards(context: Context, json: String) {
        _syncing.value = false
        _phoneNotReachable.value = false
        _phoneOutdated.value = false
        _watchOutdated.value = false
        _permissionDenied.value = false
        _bluetoothDisabled.value = false
        try {
            val incoming = WearCard.listFromJson(json)
            _cards.value = incoming
            WearCardStore.save(context, incoming)
            Log.d(TAG, "Cards updated: ${incoming.size} cards")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse cards JSON", e)
        }
    }
}
