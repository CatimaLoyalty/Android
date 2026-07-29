package protect.card_locker.shared

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import java.security.SecureRandom

object WearBluetoothSecurity {

    private const val PREFS_NAME = "catima_wear_bt_security"
    private const val PREF_TRUSTED_DEVICES = "trusted_devices"
    private const val PREF_BLOCKED_DEVICES = "blocked_devices"
    private const val TOKEN_PREFIX = "token_"
    private const val TOKEN_SIZE = 32

    private val secureRandom = SecureRandom()

    fun isDeviceTrusted(context: Context, address: String): Boolean =
        trustedSet(context).contains(address)

    fun isDeviceBlocked(context: Context, address: String): Boolean =
        blockedSet(context).contains(address)

    fun trustDevice(context: Context, address: String, token: String? = null): String {
        val deviceToken = token ?: generateToken()
        val prefs = prefs(context)
        prefs.edit {
            val trusted = HashSet(prefs.getStringSet(PREF_TRUSTED_DEVICES, emptySet()) ?: emptySet())
            trusted.add(address)
            putStringSet(PREF_TRUSTED_DEVICES, trusted)
            val blocked = HashSet(prefs.getStringSet(PREF_BLOCKED_DEVICES, emptySet()) ?: emptySet())
            blocked.remove(address)
            putStringSet(PREF_BLOCKED_DEVICES, blocked)
            putString(deviceTokenPref(address), deviceToken)
        }
        return deviceToken
    }

    fun untrustDevice(context: Context, address: String) {
        val prefs = prefs(context)
        prefs.edit {
            val trusted = HashSet(prefs.getStringSet(PREF_TRUSTED_DEVICES, emptySet()) ?: emptySet())
            trusted.remove(address)
            putStringSet(PREF_TRUSTED_DEVICES, trusted)
            remove(deviceTokenPref(address))
        }
    }

    fun unblockDevice(context: Context, address: String) {
        val prefs = prefs(context)
        prefs.edit {
            val blocked = HashSet(prefs.getStringSet(PREF_BLOCKED_DEVICES, emptySet()) ?: emptySet())
            blocked.remove(address)
            putStringSet(PREF_BLOCKED_DEVICES, blocked)
        }
    }

    fun forgetDevice(context: Context, address: String) {
        untrustDevice(context, address)
        unblockDevice(context, address)
    }

    fun blockDevice(context: Context, address: String) {
        val prefs = prefs(context)
        prefs.edit {
            val blocked = HashSet(prefs.getStringSet(PREF_BLOCKED_DEVICES, emptySet()) ?: emptySet())
            blocked.add(address)
            putStringSet(PREF_BLOCKED_DEVICES, blocked)
            val trusted = HashSet(prefs.getStringSet(PREF_TRUSTED_DEVICES, emptySet()) ?: emptySet())
            trusted.remove(address)
            putStringSet(PREF_TRUSTED_DEVICES, trusted)
            remove(deviceTokenPref(address))
        }
    }

    fun getDeviceToken(context: Context, address: String): String? =
        prefs(context).getString(deviceTokenPref(address), null)

    fun setDeviceToken(context: Context, address: String, token: String) {
        prefs(context).edit {
            putString(deviceTokenPref(address), token)
        }
    }

    fun listTrustedDevices(context: Context): Set<String> =
        HashSet(prefs(context).getStringSet(PREF_TRUSTED_DEVICES, emptySet()) ?: emptySet())

    fun listBlockedDevices(context: Context): Set<String> =
        HashSet(prefs(context).getStringSet(PREF_BLOCKED_DEVICES, emptySet()) ?: emptySet())

    fun generateToken(): String {
        val token = ByteArray(TOKEN_SIZE)
        secureRandom.nextBytes(token)
        return Base64.encodeToString(token, Base64.NO_WRAP)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun trustedSet(context: Context): Set<String> =
        prefs(context).getStringSet(PREF_TRUSTED_DEVICES, emptySet()) ?: emptySet()

    private fun blockedSet(context: Context): Set<String> =
        prefs(context).getStringSet(PREF_BLOCKED_DEVICES, emptySet()) ?: emptySet()

    private fun deviceTokenPref(address: String) = "$TOKEN_PREFIX$address"
}
