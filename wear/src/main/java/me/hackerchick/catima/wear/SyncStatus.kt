package me.hackerchick.catima.wear

import androidx.annotation.StringRes

enum class SyncStatus(@StringRes val labelRes: Int?) {
    LOADING(null),
    SYNCING(R.string.syncing),
    PHONE_NOT_REACHABLE(R.string.sync_failed),
    VERSION_INCOMPATIBLE(R.string.version_incompatible),
    UNAUTHORIZED(R.string.unauthorized),
    PERMISSION_DENIED(R.string.bluetooth_permission_denied),
    BLUETOOTH_DISABLED(R.string.bluetooth_disabled),
    SYNC_ERROR(R.string.sync_error),
    OK(null),
}
