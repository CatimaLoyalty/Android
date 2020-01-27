package protect.card_locker.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.annotation.IntegerRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDelegate;

import protect.card_locker.R;

public class Settings
{
    private Context context;
    private SharedPreferences settings;

    public Settings(Context context)
    {
        this.context = context;
        this.settings = PreferenceManager.getDefaultSharedPreferences(context);
    }

    private String getResString(@StringRes int resId)
    {
        return context.getString(resId);
    }

    private int getResInt(@IntegerRes int resId)
    {
        return context.getResources().getInteger(resId);
    }

    private String getString(@StringRes int keyId, String defaultValue)
    {
        return settings.getString(getResString(keyId), defaultValue);
    }

    private int getInt(@StringRes int keyId, @IntegerRes int defaultId)
    {
        return settings.getInt(getResString(keyId), getResInt(defaultId));
    }

    private boolean getBoolean(@StringRes int keyId, boolean defaultValue)
    {
        return settings.getBoolean(getResString(keyId), defaultValue);
    }

    public int getTheme()
    {
        String value = getString(R.string.settings_key_theme, getResString(R.string.settings_key_system_theme));

        if(value.equals(getResString(R.string.settings_key_light_theme)))
        {
            return AppCompatDelegate.MODE_NIGHT_NO;
        }
        else if(value.equals(getResString(R.string.settings_key_dark_theme)))
        {
            return AppCompatDelegate.MODE_NIGHT_YES;
        }

        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }

    public int getCardTitleListFontSize()
    {
        return getInt(R.string.settings_key_card_title_list_font_size, R.integer.settings_card_title_list_font_size_sp);
    }

    public int getCardNoteListFontSize()
    {
        return getInt(R.string.settings_key_card_note_list_font_size, R.integer.settings_card_note_list_font_size_sp);
    }

    public int getCardTitleFontSize()
    {
        return getInt(R.string.settings_key_card_title_font_size, R.integer.settings_card_title_font_size_sp);
    }

    public int getCardIdFontSize()
    {
        return getInt(R.string.settings_key_card_id_font_size, R.integer.settings_card_id_font_size_sp);
    }

    public int getCardNoteFontSize()
    {
        return getInt(R.string.settings_key_card_note_font_size, R.integer.settings_card_note_font_size_sp);
    }

    public boolean useMaxBrightnessDisplayingBarcode()
    {
        return getBoolean(R.string.settings_key_display_barcode_max_brightness, true);
    }

    public boolean getLockBarcodeScreenOrientation()
    {
        return getBoolean(R.string.settings_key_lock_barcode_orientation, false);
    }
}
