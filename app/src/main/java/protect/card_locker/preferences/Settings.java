package protect.card_locker.preferences;

import static android.content.res.Configuration.ORIENTATION_PORTRAIT;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.IntegerRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import java.util.Locale;

import protect.card_locker.R;
import protect.card_locker.Utils;

public class Settings {
    private static final String TAG = "Catima";
    private final Context mContext;
    private final SharedPreferences mSettings;

    public Settings(Context context) {
        mContext = context.getApplicationContext();
        mSettings = PreferenceManager.getDefaultSharedPreferences(context);
    }

    private String getResString(@StringRes int resId) {
        return mContext.getString(resId);
    }

    private int getResInt(@IntegerRes int resId) {
        return mContext.getResources().getInteger(resId);
    }

    private String getString(@StringRes int keyId, String defaultValue) {
        return mSettings.getString(getResString(keyId), defaultValue);
    }

    private int getInt(@StringRes int keyId, @IntegerRes int defaultId) {
        return mSettings.getInt(getResString(keyId), getResInt(defaultId));
    }

    private boolean getBoolean(@StringRes int keyId, boolean defaultValue) {
        return mSettings.getBoolean(getResString(keyId), defaultValue);
    }

    @Nullable
    public Locale getLocale() {
        String value = getString(R.string.settings_key_locale, "");

        if (value.isEmpty()) {
            return null;
        }

        return Utils.stringToLocale(value);
    }

    public int getTheme() {
        String value = getString(R.string.settings_key_theme, getResString(R.string.settings_key_system_theme));

        if (value.equals(getResString(R.string.settings_key_light_theme))) {
            return AppCompatDelegate.MODE_NIGHT_NO;
        } else if (value.equals(getResString(R.string.settings_key_dark_theme))) {
            return AppCompatDelegate.MODE_NIGHT_YES;
        }

        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }

    public boolean useMaxBrightnessDisplayingBarcode() {
        return getBoolean(R.string.settings_key_display_barcode_max_brightness, true);
    }

    public boolean getKeepScreenOn() {
        return getBoolean(R.string.settings_key_keep_screen_on, true);
    }

    public boolean getDisableLockscreenWhileViewingCard() {
        return getBoolean(R.string.settings_key_disable_lockscreen_while_viewing_card, true);
    }

    public boolean getAllowContentProviderRead() {
        return getBoolean(R.string.settings_key_allow_content_provider_read, true);
    }

    public boolean getOledDark() {
        return getBoolean(R.string.settings_key_oled_dark, false);
    }

    public String getColor() {
        return getString(R.string.setting_key_theme_color, mContext.getResources().getString(R.string.settings_key_system_theme));
    }

    public int getPreferredColumnCount() {
        var defaultSymbol = mContext.getResources().getString(R.string.settings_key_automatic_column_count);
        var defaultColumnCount = mContext.getResources().getInteger(R.integer.main_view_card_columns);
        var orientation = mContext.getResources().getConfiguration().orientation;
        var columnCountPrefKey = orientation == ORIENTATION_PORTRAIT ? R.string.setting_key_column_count_portrait : R.string.setting_key_column_count_landscape;
        var columnCountSetting = getString(columnCountPrefKey, defaultSymbol);
        try {
            // the pref may be unset or explicitly set to default
            return columnCountSetting.equals(defaultSymbol) ? defaultColumnCount : Integer.parseInt(columnCountSetting);
        } catch (NumberFormatException nfe) {
            Log.e(TAG, "Failed to parseInt the column count pref", nfe);
            return defaultColumnCount;
        }
    }

    public boolean useVolumeKeysForNavigation() {
        return getBoolean(R.string.settings_key_use_volume_keys_navigation, false);
    }
}
