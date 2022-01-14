package protect.card_locker;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.TypedValue;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import java.util.HashMap;

public class CatimaAppCompatActivity extends AppCompatActivity {

    SharedPreferences pref;
    HashMap<String, Integer> supportedThemes;

    @Override
    protected void attachBaseContext(Context base) {
        // Apply chosen language
        super.attachBaseContext(Utils.updateBaseContextLocale(base));
    }
}
