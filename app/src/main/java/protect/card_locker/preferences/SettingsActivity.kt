package protect.card_locker.preferences

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import protect.card_locker.BuildConfig
import protect.card_locker.CatimaAppCompatActivity
import protect.card_locker.MainActivity
import protect.card_locker.R
import protect.card_locker.Utils
import protect.card_locker.databinding.SettingsActivityBinding
import protect.card_locker.shared.BluetoothPermissionHelper
import protect.card_locker.shared.WearBluetoothSecurity
import protect.card_locker.wearos.BluetoothPairingNotificationManager
import protect.card_locker.wearos.WearSyncPermissionRequester

class SettingsActivity : CatimaAppCompatActivity() {

    private lateinit var binding: SettingsActivityBinding
    private lateinit var fragment: SettingsFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingsActivityBinding.inflate(layoutInflater)
        setTitle(R.string.settings)
        setContentView(binding.root)
        Utils.applyWindowInsets(binding.root)
        val toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        enableToolbarBackButton()

        // Display the fragment as the main content.
        fragment = SettingsFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.settings_container, fragment)
            .commit()

        // restore reload main state
        if (savedInstanceState != null) {
            fragment.mReloadMain = savedInstanceState.getBoolean(RELOAD_MAIN_STATE)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishSettingsActivity()
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(RELOAD_MAIN_STATE, fragment.mReloadMain)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == android.R.id.home) {
            finishSettingsActivity()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun finishSettingsActivity() {
        if (fragment.mReloadMain) {
            val intent = Intent()
            intent.putExtra(MainActivity.RESTART_ACTIVITY_INTENT, true)
            setResult(RESULT_OK, intent)
        } else {
            setResult(RESULT_OK)
        }
        finish()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        var mReloadMain: Boolean = false

        private lateinit var wearSyncPermissionRequester: WearSyncPermissionRequester
        private var currentDevicesDialog: AlertDialog? = null
        private var currentDevicesDialogIsBlocked: Boolean = false
        private val devicesChangedReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != BluetoothPairingNotificationManager.ACTION_DEVICES_CHANGED) {
                    return
                }
                findPreference<Preference>(getString(R.string.settings_key_wear_sync_devices))?.let { updateWearSyncAllowedDevicesSummary(it) }
                findPreference<Preference>(getString(R.string.settings_key_wear_sync_blocked_devices))?.let { updateWearSyncBlockedDevicesSummary(it) }
                if (currentDevicesDialog?.isShowing != true) {
                    return
                }
                val isBlocked = currentDevicesDialogIsBlocked
                currentDevicesDialog?.dismiss()
                if (isBlocked) {
                    showWearSyncBlockedDevicesDialog()
                } else {
                    showWearSyncAllowedDevicesDialog()
                }
            }
        }

        override fun onResume() {
            super.onResume()
            wearSyncPermissionRequester.synchronize()
            findPreference<Preference>(getString(R.string.settings_key_wear_sync_devices))?.let { updateWearSyncAllowedDevicesSummary(it) }
            findPreference<Preference>(getString(R.string.settings_key_wear_sync_blocked_devices))?.let { updateWearSyncBlockedDevicesSummary(it) }
            ContextCompat.registerReceiver(
                requireContext(),
                devicesChangedReceiver,
                IntentFilter(BluetoothPairingNotificationManager.ACTION_DEVICES_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }

        override fun onPause() {
            super.onPause()
            requireContext().unregisterReceiver(devicesChangedReceiver)
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            wearSyncPermissionRequester = WearSyncPermissionRequester(this, requireContext())

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences)

            setupThemePreference()
            setupOledDarkPreference()
            setupLocalePreference()
            setupCrashReporterPreference()
            setupWearSyncPreference()
            setupWearSyncDevicePreferences()
        }

        private fun setupThemePreference() {
            val themePreference = findPreference<ListPreference>(getString(R.string.settings_key_theme))
            themePreference!!.setOnPreferenceChangeListener { _, o ->
                when (o.toString()) {
                    getString(R.string.settings_key_light_theme) -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    }
                    getString(R.string.settings_key_dark_theme) -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    }
                    else -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    }
                }
                true
            }
        }

        private fun setupOledDarkPreference() {
            val oledDarkPreference = findPreference<Preference>(getString(R.string.settings_key_oled_dark))
            oledDarkPreference!!.setOnPreferenceChangeListener { _, _ ->
                refreshActivity()
                true
            }
        }

        private fun setupLocalePreference() {
            val localePreference =
                findPreference<ListPreference>(getString(R.string.settings_key_locale))!!
            localePreference.let {
                val entryValues = it.entryValues
                val entries = entryValues.map { entry ->
                    if (entry.isEmpty()) {
                        getString(R.string.settings_system_locale)
                    } else {
                        val entryLocale = Utils.stringToLocale(entry.toString())
                        entryLocale.getDisplayName(entryLocale)
                    }
                }
                it.entries = entries.toTypedArray()

                // Make locale picker preference in sync with system settings
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val sysLocale = AppCompatDelegate.getApplicationLocales()[0]
                    if (sysLocale == null) {
                        // Corresponds to "System"
                        it.value = ""
                    } else {
                        // Need to set preference's value to one of localePreference.getEntryValues() to match the locale.
                        // Locale.toLanguageTag() theoretically should be one of the values in localePreference.getEntryValues()...
                        // But it doesn't work for some locales. so trying something more heavyweight.

                        // Obtain all locales supported by the app.
                        val appLocales = entryValues.map { entry -> Utils.stringToLocale(entry.toString()) }
                        // Get the app locale that best matches the system one
                        val bestMatchLocale = Utils.getBestMatchLocale(appLocales, sysLocale)
                        // Get its index in supported locales
                        val index = appLocales.indexOf(bestMatchLocale)
                        // Set preference value to entry value at that index
                        it.value = entryValues[index].toString()
                    }
                }
            }

            localePreference.setOnPreferenceChangeListener { _, newValue ->
                // See corresponding comment in Utils.updateBaseContextLocale for Android 6- notes
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    refreshActivity()
                    return@setOnPreferenceChangeListener true
                }
                val newLocale = newValue as String
                // If newLocale is empty, that means "System" was selected
                AppCompatDelegate.setApplicationLocales(if (newLocale.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.create(Utils.stringToLocale(newLocale)))
                true
            }
        }

        private fun setupCrashReporterPreference() {
            // Hide crash reporter settings on builds it's not enabled on
            val crashReporterPreference = findPreference<Preference>("acra.enable")
            crashReporterPreference!!.isVisible = BuildConfig.useAcraCrashReporter
        }

        private fun setupWearSyncPreference() {
            val wearSyncPreference = findPreference<SwitchPreferenceCompat>(getString(R.string.settings_key_wear_sync))
            wearSyncPreference!!.setOnPreferenceChangeListener { preference, newValue ->
                val enabled = newValue as Boolean
                if (enabled) {
                    wearSyncPermissionRequester.onWearSyncChanged(true) { granted ->
                        if (granted) {
                            (preference as? SwitchPreferenceCompat)?.isChecked = true
                        } else {
                            showWearSyncPermissionDeniedSnackbar()
                        }
                    }
                    false
                } else {
                    wearSyncPermissionRequester.onWearSyncChanged(false)
                    true
                }
            }
        }

        private fun setupWearSyncDevicePreferences() {
            val allowedPreference = findPreference<Preference>(getString(R.string.settings_key_wear_sync_devices))
            allowedPreference!!.setOnPreferenceClickListener {
                showWearSyncAllowedDevicesDialog()
                true
            }
            updateWearSyncAllowedDevicesSummary(allowedPreference)

            val blockedPreference = findPreference<Preference>(getString(R.string.settings_key_wear_sync_blocked_devices))
            blockedPreference!!.setOnPreferenceClickListener {
                showWearSyncBlockedDevicesDialog()
                true
            }
            updateWearSyncBlockedDevicesSummary(blockedPreference)
        }

        private fun updateWearSyncAllowedDevicesSummary(preference: Preference) {
            val count = WearBluetoothSecurity.listTrustedDevices(requireContext()).size
            preference.summary = if (count == 0) {
                getString(R.string.settings_wear_sync_no_devices)
            } else {
                resources.getQuantityString(R.plurals.settings_wear_sync_devices_summary_count, count, count)
            }
        }

        private fun updateWearSyncBlockedDevicesSummary(preference: Preference) {
            val count = WearBluetoothSecurity.listBlockedDevices(requireContext()).size
            preference.summary = if (count == 0) {
                getString(R.string.settings_wear_sync_no_blocked_devices)
            } else {
                resources.getQuantityString(R.plurals.settings_wear_sync_blocked_devices_summary_count, count, count)
            }
        }

        @SuppressLint("MissingPermission")
        private fun showWearSyncDeviceListDialog(
            titleRes: Int,
            noDevicesRes: Int,
            messageRes: Int,
            addresses: Set<String>,
            isBlocked: Boolean,
            onRemove: (String, String) -> Unit
        ) {
            val context = requireContext()
            if (!BluetoothPermissionHelper.isBluetoothConnectGranted(context)) {
                showWearSyncPermissionDeniedSnackbar()
                return
            }
            val knownAddresses = addresses.toList()
            currentDevicesDialog?.dismiss()
            currentDevicesDialogIsBlocked = isBlocked
            if (knownAddresses.isEmpty()) {
                currentDevicesDialog = MaterialAlertDialogBuilder(context)
                    .setTitle(titleRes)
                    .setMessage(noDevicesRes)
                    .setOnDismissListener {
                        currentDevicesDialog = null
                        currentDevicesDialogIsBlocked = false
                    }
                    .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
                    .show()
                return
            }

            val adapter = context.getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager
            val bondedDevices = try {
                adapter?.adapter?.bondedDevices ?: emptySet()
            } catch (_: SecurityException) {
                emptySet<BluetoothDevice>()
            }
            val deviceMap = bondedDevices.associateBy { it.address }

            val content = View.inflate(context, R.layout.wear_sync_device_list, null)
            content.findViewById<TextView>(R.id.wear_sync_device_list_message).text = getString(messageRes)
            val container = content.findViewById<LinearLayout>(R.id.wear_sync_device_list_container)

            knownAddresses.forEach { address ->
                val name = deviceName(deviceMap[address], address)
                val itemView = LayoutInflater.from(context).inflate(R.layout.wear_sync_device_list_item, container, false) as TextView
                itemView.text = getString(R.string.settings_wear_sync_device_name, name, address)
                itemView.setOnClickListener { onRemove(address, name) }
                container.addView(itemView)
            }

            currentDevicesDialog = MaterialAlertDialogBuilder(context)
                .setTitle(titleRes)
                .setView(content)
                .setOnDismissListener {
                    currentDevicesDialog = null
                    currentDevicesDialogIsBlocked = false
                }
                .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .show()
        }

        private fun showWearSyncAllowedDevicesDialog() {
            showWearSyncDeviceListDialog(
                R.string.settings_wear_sync_devices,
                R.string.settings_wear_sync_no_devices,
                R.string.settings_wear_sync_tap_to_remove,
                WearBluetoothSecurity.listTrustedDevices(requireContext()),
                isBlocked = false
            ) { address, name -> confirmRemoveAllowedDevice(address, name) }
        }

        private fun showWearSyncBlockedDevicesDialog() {
            showWearSyncDeviceListDialog(
                R.string.settings_wear_sync_blocked_devices,
                R.string.settings_wear_sync_no_blocked_devices,
                R.string.settings_wear_sync_tap_to_remove,
                WearBluetoothSecurity.listBlockedDevices(requireContext()),
                isBlocked = true
            ) { address, name -> confirmRemoveBlockedDevice(address, name) }
        }

        private fun confirmRemoveAllowedDevice(address: String, name: String) {
            confirmRemoveDevice(
                address,
                name,
                R.string.settings_wear_sync_unpair_message,
                onRemove = { WearBluetoothSecurity.untrustDevice(requireContext(), address) }
            )
        }

        private fun confirmRemoveBlockedDevice(address: String, name: String) {
            confirmRemoveDevice(
                address,
                name,
                R.string.settings_wear_sync_unblock_message,
                onRemove = { WearBluetoothSecurity.unblockDevice(requireContext(), address) }
            )
        }

        private fun confirmRemoveDevice(address: String, name: String, messageRes: Int, onRemove: () -> Unit) {
            val context = requireContext()
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.settings_wear_sync_unpair_title)
                .setMessage(getString(messageRes, name))
                .setPositiveButton(R.string.settings_wear_sync_unpair) { dialog, _ ->
                    onRemove()
                    BluetoothPairingNotificationManager.cancel(context, address)
                    BluetoothPairingNotificationManager.notifyDevicesChanged(context)
                    findPreference<Preference>(getString(R.string.settings_key_wear_sync_devices))?.let { updateWearSyncAllowedDevicesSummary(it) }
                    findPreference<Preference>(getString(R.string.settings_key_wear_sync_blocked_devices))?.let { updateWearSyncBlockedDevicesSummary(it) }
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .show()
        }

        private fun deviceName(device: BluetoothDevice?, fallback: String): String {
            return try { device?.name } catch (_: SecurityException) { null } ?: fallback
        }

        private fun showWearSyncPermissionDeniedSnackbar() {
            Snackbar.make(
                requireView(),
                R.string.wear_sync_permission_required,
                Snackbar.LENGTH_LONG
            ).setAction(R.string.open_settings) {
                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", requireContext().packageName, null)
                    )
                )
            }.show()
        }

        private fun refreshActivity() {
            mReloadMain = true
            activity?.recreate()
        }
    }

    companion object {
        private const val RELOAD_MAIN_STATE = "mReloadMain"
    }
}
