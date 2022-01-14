package protect.card_locker.preferences;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import nl.invissvenska.numberpickerpreference.NumberDialogPreference;
import nl.invissvenska.numberpickerpreference.NumberPickerPreferenceDialogFragment;
import protect.card_locker.CatimaAppCompatActivity;
import protect.card_locker.MainActivity;
import protect.card_locker.R;
import protect.card_locker.Utils;

public class SettingsActivity extends CatimaAppCompatActivity {

    private final static String RELOAD_MAIN_STATE = "mReloadMain";
    private SettingsFragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.settings);
        setContentView(R.layout.settings_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

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

            // Show pretty names
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

            Preference themePreference = findPreference(getResources().getString(R.string.settings_key_theme));
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

            localePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                refreshActivity(true);
                return true;
            });
        }

        private void refreshActivity(boolean reloadMain) {
            mReloadMain = reloadMain || mReloadMain;
            Activity activity = getActivity();
            if (activity != null) {
                activity.recreate();
            }
        }

        @Override
        public void onDisplayPreferenceDialog(Preference preference) {
            if (preference instanceof NumberDialogPreference) {
                NumberDialogPreference dialogPreference = (NumberDialogPreference) preference;
                DialogFragment dialogFragment = NumberPickerPreferenceDialogFragment
                        .newInstance(
                                dialogPreference.getKey(),
                                dialogPreference.getMinValue(),
                                dialogPreference.getMaxValue(),
                                dialogPreference.getStepValue(),
                                dialogPreference.getUnitText()
                        );
                dialogFragment.setTargetFragment(this, 0);
                dialogFragment.show(getParentFragmentManager(), DIALOG_FRAGMENT_TAG);
            } else {
                super.onDisplayPreferenceDialog(preference);
            }
        }
    }
}

