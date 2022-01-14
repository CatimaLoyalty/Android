package protect.card_locker;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

public class CatimaAppCompatActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context base) {
        // Apply chosen language
        super.attachBaseContext(Utils.updateBaseContextLocale(base));
    }
}
