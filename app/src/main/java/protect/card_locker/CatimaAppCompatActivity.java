package protect.card_locker;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;
import android.util.TypedValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public class CatimaAppCompatActivity extends AppCompatActivity {

    SharedPreferences pref;
    HashMap<String, Integer> supportedThemes;

    @Override
    protected void attachBaseContext(Context base) {
        // Apply chosen language
        super.attachBaseContext(Utils.updateBaseContextLocale(base));
    }

    @Override
    public Resources.Theme getTheme() {
        if (supportedThemes == null) {
            supportedThemes = new HashMap<>();
            supportedThemes.put(getString(R.string.settings_key_blue_theme), R.style.AppTheme_blue);
            supportedThemes.put(getString(R.string.settings_key_brown_theme), R.style.AppTheme_brown);
            supportedThemes.put(getString(R.string.settings_key_green_theme), R.style.AppTheme_green);
            supportedThemes.put(getString(R.string.settings_key_grey_theme), R.style.AppTheme_grey);
            supportedThemes.put(getString(R.string.settings_key_magenta_theme), R.style.AppTheme_magenta);
            supportedThemes.put(getString(R.string.settings_key_pink_theme), R.style.AppTheme_pink);
            supportedThemes.put(getString(R.string.settings_key_sky_blue_theme), R.style.AppTheme_sky_blue);
            supportedThemes.put(getString(R.string.settings_key_violet_theme), R.style.AppTheme_violet);
        }

        Resources.Theme theme = super.getTheme();
        pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String themeName = pref.getString(getString(R.string.setting_key_theme_color), getString(R.string.settings_key_catima_theme));

        theme.applyStyle(Utils.mapGetOrDefault(supportedThemes, themeName, R.style.AppTheme_NoActionBar), true);

        return theme;
    }

    public int getThemeColor() {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getTheme();
        theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
        return typedValue.data;
    }
}
