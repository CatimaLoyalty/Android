package protect.card_locker.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

import androidx.annotation.IntegerRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;
import protect.card_locker.R;
import protect.card_locker.Utils;

public class Settings {
    private final Context mContext;
    private SharedPreferences mSettings;

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

    public Locale getLocale() {
        String value = getString(R.string.settings_key_locale, "");

        if (value.length() == 0) {
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

    public double getFontSizeScale() {
        return getInt(R.string.settings_key_max_font_size_scale, R.integer.settings_max_font_size_scale_pct) / 100.0;
    }

    public int getSmallFont() {
        return 14;
    }

    public int getMediumFont() {
        return 28;
    }

    public int getLargeFont() {
        return 40;
    }

    public int getFontSizeMin(int fontSize) {
        return (int) (Math.round(fontSize / 2.0) - 1);
    }

    public int getFontSizeMax(int fontSize) {
        return (int) Math.round(fontSize * getFontSizeScale());
    }

    public boolean useMaxBrightnessDisplayingBarcode() {
        return getBoolean(R.string.settings_key_display_barcode_max_brightness, true);
    }

    public String getCardViewOrientation() {
        return getString(R.string.settings_key_card_orientation, getResString(R.string.settings_key_follow_system_orientation));
    }

    public boolean getKeepScreenOn() {
        return getBoolean(R.string.settings_key_keep_screen_on, true);
    }

    public boolean getDisableLockscreenWhileViewingCard() {
        return getBoolean(R.string.settings_key_disable_lockscreen_while_viewing_card, true);
    }

    public boolean getOledDark() {
        return getBoolean(R.string.settings_key_oled_dark, false);
    }

    public String getColor() {
        return getString(R.string.setting_key_theme_color, mContext.getResources().getString(R.string.settings_key_system_theme));
    }
}
