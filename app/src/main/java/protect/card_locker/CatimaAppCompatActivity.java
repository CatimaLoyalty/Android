package protect.card_locker;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.TypedValue;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public class CatimaAppCompatActivity extends AppCompatActivity {

    SharedPreferences pref;

    @Override
    protected void attachBaseContext(Context base) {
        // Apply chosen language
        super.attachBaseContext(Utils.updateBaseContextLocale(base));
    }

    @Override
    public Resources.Theme getTheme() {
        Resources.Theme theme = super.getTheme();
        pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String themeName = pref.getString("pref_theme_color", getString(R.string.settings_key_catima_theme));
        if (themeName.equals(getString(R.string.settings_key_brown_theme))){
            theme.applyStyle(R.style.AppTheme_brown, true);
        } else if(themeName.equals(getString(R.string.settings_key_pink_theme))){
            theme.applyStyle(R.style.AppTheme_pink, true);
        } else if(themeName.equals(getString(R.string.settings_key_magenta_theme))){
            theme.applyStyle(R.style.AppTheme_magenta, true);
        } else if(themeName.equals(getString(R.string.settings_key_violet_theme))){
            theme.applyStyle(R.style.AppTheme_violet, true);
        } else if(themeName.equals(getString(R.string.settings_key_blue_theme))){
            theme.applyStyle(R.style.AppTheme_blue, true);
        } else if(themeName.equals(getString(R.string.settings_key_sky_blue_theme))){
            theme.applyStyle(R.style.AppTheme_sky_blue, true);
        } else if(themeName.equals(getString(R.string.settings_key_green_theme))){
            theme.applyStyle(R.style.AppTheme_green, true);
        } else if(themeName.equals(getString(R.string.settings_key_grey_theme))){
            theme.applyStyle(R.style.AppTheme_grey, true);
        } else {
            theme.applyStyle(R.style.AppTheme_NoActionBar, true);
        }

        return theme;
    }

    public int getThemeColor(){
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getTheme();
        theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
        return typedValue.data;
    }
}
