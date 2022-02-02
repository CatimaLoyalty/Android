package protect.card_locker;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.color.DynamicColors;

import protect.card_locker.preferences.Settings;

public class LoyaltyCardLockerApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Settings settings = new Settings(this);
        AppCompatDelegate.setDefaultNightMode(settings.getTheme());
    }
}
