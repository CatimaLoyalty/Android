package protect.card_locker.wearos

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import protect.card_locker.R

@RunWith(RobolectricTestRunner::class)
class WearSyncServiceManagerTest {

    @Test
    fun onPermissionResult_denied_disablesWearSync() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putBoolean(context.getString(R.string.settings_key_wear_sync), true).commit()

        WearSyncServiceManager.onPermissionResult(context, false)

        assertFalse(prefs.getBoolean(context.getString(R.string.settings_key_wear_sync), true))
    }

    @Test
    fun onPermissionResult_granted_keepsWearSyncEnabled() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putBoolean(context.getString(R.string.settings_key_wear_sync), true).commit()

        WearSyncServiceManager.onPermissionResult(context, true)

        assertTrue(prefs.getBoolean(context.getString(R.string.settings_key_wear_sync), false))
    }
}
