package protect.card_locker.wearos

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import protect.card_locker.R

class FakeServiceStartContext(base: Context) : ContextWrapper(base) {
    var startedServiceIntent: Intent? = null
        private set

    override fun startForegroundService(service: Intent): ComponentName? {
        startedServiceIntent = service
        return null
    }

    override fun startService(service: Intent): ComponentName? {
        startedServiceIntent = service
        return null
    }
}

@RunWith(RobolectricTestRunner::class)
class BluetoothStateReceiverTest {

    @Test
    fun bluetoothOn_syncEnabled_startsService() {
        val realContext = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(realContext).grantPermissions(
            "android.permission.BLUETOOTH_CONNECT",
            "android.permission.POST_NOTIFICATIONS"
        )
        val prefs = PreferenceManager.getDefaultSharedPreferences(realContext)
        prefs.edit().putBoolean(realContext.getString(R.string.settings_key_wear_sync), true).commit()

        val fakeContext = FakeServiceStartContext(realContext)

        val intent = Intent(BluetoothAdapter.ACTION_STATE_CHANGED).apply {
            putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON)
        }
        BluetoothStateReceiver().onReceive(fakeContext, intent)

        assertEquals(
            BluetoothServerService::class.java.name,
            fakeContext.startedServiceIntent?.component?.className
        )
    }

    @Test
    fun bluetoothOff_doesNotStartService() {
        val realContext = ApplicationProvider.getApplicationContext<Application>()
        val prefs = PreferenceManager.getDefaultSharedPreferences(realContext)
        prefs.edit().putBoolean(realContext.getString(R.string.settings_key_wear_sync), true).commit()

        val fakeContext = FakeServiceStartContext(realContext)

        val intent = Intent(BluetoothAdapter.ACTION_STATE_CHANGED).apply {
            putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
        }
        BluetoothStateReceiver().onReceive(fakeContext, intent)

        assertNull(fakeContext.startedServiceIntent)
    }
}