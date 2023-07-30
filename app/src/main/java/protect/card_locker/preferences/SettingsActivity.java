package protect.card_locker.preferences;


import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.os.LocaleListCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.color.DynamicColors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import protect.card_locker.CatimaAppCompatActivity;
import protect.card_locker.MainActivity;
import protect.card_locker.R;
import protect.card_locker.Utils;
import protect.card_locker.databinding.SettingsActivityBinding;

public class SettingsActivity extends CatimaAppCompatActivity {

    private SettingsActivityBinding binding;
    private final static String RELOAD_MAIN_STATE = "mReloadMain";
    private SettingsFragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = SettingsActivityBinding.inflate(getLayoutInflater());
        setTitle(R.string.settings);
        setContentView(binding.getRoot());
        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        enableToolbarBackButton();

        // Display the fragment as the main content.
        fragment = new SettingsFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_container, fragment)
                .commit();

        // restore reload main state
        if (savedInstanceState != null) {
            fragment.mReloadMain = savedInstanceState.getBoolean(RELOAD_MAIN_STATE);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(RELOAD_MAIN_STATE, fragment.mReloadMain);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finishSettingsActivity();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        finishSettingsActivity();
    }

    private void finishSettingsActivity() {
        if (fragment.mReloadMain) {
            Intent intent = new Intent();
            intent.putExtra(MainActivity.RESTART_ACTIVITY_INTENT, true);
            setResult(Activity.RESULT_OK, intent);
        } else {
            setResult(Activity.RESULT_OK);
        }
        finish();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        private static final String DIALOG_FRAGMENT_TAG = "SettingsFragment";

        public boolean mReloadMain;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            // Show pretty names and summaries
            ListPreference themePreference = findPreference(getResources().getString(R.string.settings_key_theme));
            assert themePreference != null;
            themePreference.setOnPreferenceChangeListener((preference, o) -> {
                if (o.toString().equals(getResources().getString(R.string.settings_key_light_theme))) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                } else if (o.toString().equals(getResources().getString(R.string.settings_key_dark_theme))) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                }

                return true;
            });

            ListPreference themeColorPreference = findPreference(getResources().getString(R.string.setting_key_theme_color));
            assert themeColorPreference != null;
            themeColorPreference.setOnPreferenceChangeListener((preference, o) -> {
                refreshActivity(true);
                return true;
            });
            if (!DynamicColors.isDynamicColorAvailable()) {
                themeColorPreference.setEntryValues(R.array.color_values_no_dynamic);
                themeColorPreference.setEntries(R.array.color_value_strings_no_dynamic);
            }

            Preference oledDarkPreference = findPreference(getResources().getString(R.string.settings_key_oled_dark));
            assert oledDarkPreference != null;
            oledDarkPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                refreshActivity(true);
                return true;
            });

            ListPreference localePreference = findPreference(getResources().getString(R.string.settings_key_locale));
            assert localePreference != null;
            CharSequence[] entryValues = localePreference.getEntryValues();
            List<CharSequence> entries = new ArrayList<>();
            for (CharSequence entry : entryValues) {
                if (entry.length() == 0) {
                    entries.add(getResources().getString(R.string.settings_system_locale));
                } else {
                    Locale entryLocale = Utils.stringToLocale(entry.toString());
                    entries.add(entryLocale.getDisplayName(entryLocale));
                }
            }
            localePreference.setEntries(entries.toArray(new CharSequence[entryValues.length]));
            // Make locale picker preference in sync with system settings
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Locale sysLocale = AppCompatDelegate.getApplicationLocales().get(0);
                if (sysLocale == null) {
                    // Corresponds to "System"
                    localePreference.setValue("");
                } else {
                    // Need to set preference's value to one of localePreference.getEntryValues() to match the locale.
                    // Locale.toLanguageTag() theoretically should be one of the values in localePreference.getEntryValues()...
                    // But it doesn't work for some locales. so trying something more heavyweight.

                    // Obtain all locales supported by the app.
                    List<Locale> appLocales = Arrays.stream(localePreference.getEntryValues())
                            .map(Objects::toString)
                            .map(Utils::stringToLocale)
                            .collect(Collectors.toList());
                    // Get the app locale that best matches the system one
                    Locale bestMatchLocale = Utils.getBestMatchLocale(appLocales, sysLocale);
                    // Get its index in supported locales
                    int index = appLocales.indexOf(bestMatchLocale);
                    // Set preference value to entry value at that index
                    localePreference.setValue(localePreference.getEntryValues()[index].toString());
                }
            }

            localePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                //See corresponding comment in Utils.updateBaseContextLocale for Android 6- notes
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    refreshActivity(true);
                    return true;
                }
                String newLocale = (String) newValue;
                // If newLocale is empty, that means "System" was selected
                AppCompatDelegate.setApplicationLocales(newLocale.isEmpty() ? LocaleListCompat.getEmptyLocaleList() : LocaleListCompat.create(Utils.stringToLocale(newLocale)));
                return true;
            });

            // Disable content provider on SDK < 23 since dangerous permissions
            // are granted at install-time
            Preference contentProviderReadPreference = findPreference(getResources().getString(R.string.settings_key_allow_content_provider_read));
            assert contentProviderReadPreference != null;
            contentProviderReadPreference.setVisible(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);
        }

        private void refreshActivity(boolean reloadMain) {
            mReloadMain = reloadMain || mReloadMain;
            Activity activity = getActivity();
            if (activity != null) {
                activity.recreate();
            }
        }
    }
}
