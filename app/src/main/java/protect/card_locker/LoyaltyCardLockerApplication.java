package protect.card_locker;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDexApplication;

import protect.card_locker.preferences.Settings;

public class LoyaltyCardLockerApplication extends MultiDexApplication {
    public void onCreate() {
        super.onCreate();

        Settings settings = new Settings(getApplicationContext());
        AppCompatDelegate.setDefaultNightMode(settings.getTheme());
    }
}
