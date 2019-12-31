package protect.card_locker;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

import protect.card_locker.preferences.Settings;

public class LoyaltyCardLockerApplication extends Application {
    public void onCreate() {
        super.onCreate();

        Settings settings = new Settings(getApplicationContext());
        AppCompatDelegate.setDefaultNightMode(settings.getTheme());
    }
}
