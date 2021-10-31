package protect.card_locker.preferences;


import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import nl.invissvenska.numberpickerpreference.NumberDialogPreference;
import nl.invissvenska.numberpickerpreference.NumberPickerPreferenceDialogFragment;
import protect.card_locker.CatimaAppCompatActivity;
import protect.card_locker.R;
import protect.card_locker.Utils;
import protect.card_locker.databinding.SettingsActivityBinding;

public class SettingsActivity extends CatimaAppCompatActivity
{
    private SettingsActivityBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding = SettingsActivityBinding.inflate(getLayoutInflater());
        setTitle(R.string.settings);
        setContentView(binding.getRoot());
        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Display the fragment as the main content.
        SettingsFragment fragment = new SettingsFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_container, fragment)
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if(id == android.R.id.home)
        {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat
    {
        private static final String DIALOG_FRAGMENT_TAG = "SettingsFragment";

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

                FragmentActivity activity = getActivity();
                if (activity != null) {
                    ActivityCompat.recreate(activity);
                }
                return true;
            });

            Preference colorPreference = findPreference(getResources().getString(R.string.setting_key_theme_color));
            assert colorPreference != null;
            colorPreference.setOnPreferenceChangeListener((preference, o) -> {
                FragmentActivity activity = getActivity();
                if (activity != null) {
                    ActivityCompat.recreate(activity);
                }
                return true;
            });
            localePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                // Refresh the activity
                Intent intent = new Intent(getContext(), SettingsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                getContext().startActivity(intent);

                return true;
            });
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

