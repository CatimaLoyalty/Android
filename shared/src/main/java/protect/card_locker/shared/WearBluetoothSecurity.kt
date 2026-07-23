package protect.card_locker.shared

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object WearBluetoothSecurity {

    private const val PREFS_NAME = "catima_wear_bt_security"
    private const val PREF_TRUSTED_DEVICES = "trusted_devices"
    private const val PREF_BLOCKED_DEVICES = "blocked_devices"
    private const val KEY_PREFIX = "key_"

    private const val AES_KEY_SIZE = 32
    private const val NONCE_SIZE = 12
    private const val TAG_LENGTH = 128

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val ALGORITHM = "AES"

    private val secureRandom = SecureRandom()

    fun isDeviceTrusted(context: Context, address: String): Boolean =
        trustedSet(context).contains(address)

    fun isDeviceBlocked(context: Context, address: String): Boolean =
        blockedSet(context).contains(address)

    fun trustDevice(context: Context, address: String, key: String? = null): String {
        val deviceKey = key ?: generateKey()
        val prefs = prefs(context)
        prefs.edit {
            val trusted = HashSet(prefs.getStringSet(PREF_TRUSTED_DEVICES, emptySet()) ?: emptySet())
            trusted.add(address)
            putStringSet(PREF_TRUSTED_DEVICES, trusted)
            putString(deviceKeyPref(address), deviceKey)
        }
        return deviceKey
    }

    fun untrustDevice(context: Context, address: String) {
        val prefs = prefs(context)
        prefs.edit {
            val trusted = HashSet(prefs.getStringSet(PREF_TRUSTED_DEVICES, emptySet()) ?: emptySet())
            trusted.remove(address)
            putStringSet(PREF_TRUSTED_DEVICES, trusted)
            remove(deviceKeyPref(address))
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
            remove(deviceKeyPref(address))
        }
    }

    fun getDeviceKey(context: Context, address: String): String? =
        prefs(context).getString(deviceKeyPref(address), null)

    fun setDeviceKey(context: Context, address: String, key: String) {
        prefs(context).edit { putString(deviceKeyPref(address), key) }
    }

    fun listTrustedDevices(context: Context): Set<String> =
        HashSet(prefs(context).getStringSet(PREF_TRUSTED_DEVICES, emptySet()) ?: emptySet())

    fun listBlockedDevices(context: Context): Set<String> =
        HashSet(prefs(context).getStringSet(PREF_BLOCKED_DEVICES, emptySet()) ?: emptySet())

    fun generateKey(): String {
        val key = ByteArray(AES_KEY_SIZE)
        secureRandom.nextBytes(key)
        return Base64.encodeToString(key, Base64.NO_WRAP)
    }

    fun encrypt(plaintext: String, keyBase64: String): String {
        val keyBytes = Base64.decode(keyBase64, Base64.NO_WRAP)
        val nonce = ByteArray(NONCE_SIZE)
        secureRandom.nextBytes(nonce)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, ALGORITHM), GCMParameterSpec(TAG_LENGTH, nonce))
        val cipherText = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        val nonceB64 = Base64.encodeToString(nonce, Base64.NO_WRAP)
        val cipherB64 = Base64.encodeToString(cipherText, Base64.NO_WRAP)
        return "$nonceB64:$cipherB64"
    }

    fun decrypt(cipherText: String, keyBase64: String): String? {
        return try {
            val separator = cipherText.indexOf(':')
            if (separator == -1) return null

            val nonceB64 = cipherText.substring(0, separator)
            val cipherB64 = cipherText.substring(separator + 1)

            val nonce = Base64.decode(nonceB64, Base64.NO_WRAP)
            val encrypted = Base64.decode(cipherB64, Base64.NO_WRAP)
            val keyBytes = Base64.decode(keyBase64, Base64.NO_WRAP)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, ALGORITHM), GCMParameterSpec(TAG_LENGTH, nonce))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun trustedSet(context: Context): Set<String> =
        prefs(context).getStringSet(PREF_TRUSTED_DEVICES, emptySet()) ?: emptySet()

    private fun blockedSet(context: Context): Set<String> =
        prefs(context).getStringSet(PREF_BLOCKED_DEVICES, emptySet()) ?: emptySet()

    private fun deviceKeyPref(address: String) = "$KEY_PREFIX$address"
}
