package protect.card_locker;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;
import protect.card_locker.preferences.Settings;

public class LoyaltyCardLockerApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Settings settings = new Settings(this);
        AppCompatDelegate.setDefaultNightMode(settings.getTheme());
    }
}
