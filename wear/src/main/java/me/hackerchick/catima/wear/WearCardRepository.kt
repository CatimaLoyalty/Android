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

    private val _syncStatus = MutableStateFlow(SyncStatus.LOADING)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    fun loadCache(context: Context) {
        val cached = WearCardStore.load(context)
        if (cached != null && _cards.value == null) {
            _cards.value = cached
            Log.d(TAG, "Loaded ${cached.size} cards from cache")
        }
    }

    fun setSyncing(value: Boolean) {
        _syncStatus.value = if (value) SyncStatus.SYNCING else SyncStatus.OK
    }

    fun setPhoneNotReachable() {
        _syncStatus.value = SyncStatus.PHONE_NOT_REACHABLE
    }

    fun setPhoneOutdated() {
        _syncStatus.value = SyncStatus.PHONE_OUTDATED
    }

    fun setWatchOutdated() {
        _syncStatus.value = SyncStatus.WATCH_OUTDATED
    }

    fun setPermissionDenied() {
        _syncStatus.value = SyncStatus.PERMISSION_DENIED
    }

    fun setBluetoothDisabled() {
        _syncStatus.value = SyncStatus.BLUETOOTH_DISABLED
    }

    fun updateCards(context: Context, json: String) {
        try {
            val incoming = WearCard.listFromJson(json)
            _cards.value = incoming
            _syncStatus.value = SyncStatus.OK
            WearCardStore.save(context, incoming)
            Log.d(TAG, "Cards updated: ${incoming.size} cards")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse cards JSON", e)
            _syncStatus.value = SyncStatus.SYNC_ERROR
        }
    }
}
