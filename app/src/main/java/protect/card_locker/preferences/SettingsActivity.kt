package protect.card_locker.preferences

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.color.DynamicColors
import protect.card_locker.BuildConfig
import protect.card_locker.CatimaAppCompatActivity
import protect.card_locker.MainActivity
import protect.card_locker.R
import protect.card_locker.Utils
import protect.card_locker.databinding.SettingsActivityBinding

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

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences)

            // Show pretty names and summaries
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

            val themeColorPreference = findPreference<ListPreference>(getString(R.string.setting_key_theme_color))
            themeColorPreference!!.setOnPreferenceChangeListener { _, _ ->
                refreshActivity(true)
                true
            }
            if (!DynamicColors.isDynamicColorAvailable()) {
                themeColorPreference.setEntryValues(R.array.color_values_no_dynamic)
                themeColorPreference.setEntries(R.array.color_value_strings_no_dynamic)
            }

            val oledDarkPreference = findPreference<Preference>(getString(R.string.settings_key_oled_dark))
            oledDarkPreference!!.setOnPreferenceChangeListener { _, _ ->
                refreshActivity(true)
                true
            }

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
                    refreshActivity(true)
                    return@setOnPreferenceChangeListener true
                }
                val newLocale = newValue as String
                // If newLocale is empty, that means "System" was selected
                AppCompatDelegate.setApplicationLocales(if (newLocale.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.create(Utils.stringToLocale(newLocale)))
                true
            }

            // Disable content provider on SDK < 23 since dangerous permissions
            // are granted at install-time
            val contentProviderReadPreference = findPreference<Preference>(getString(R.string.settings_key_allow_content_provider_read))
            contentProviderReadPreference!!.isVisible =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

            // Hide crash reporter settings on builds it's not enabled on
            val crashReporterPreference = findPreference<Preference>("acra.enable")
            crashReporterPreference!!.isVisible = BuildConfig.useAcraCrashReporter
        }

        private fun refreshActivity(reloadMain: Boolean) {
            mReloadMain = reloadMain || mReloadMain
            activity?.recreate()
        }
    }

    companion object {
        private const val RELOAD_MAIN_STATE = "mReloadMain"
    }
}
