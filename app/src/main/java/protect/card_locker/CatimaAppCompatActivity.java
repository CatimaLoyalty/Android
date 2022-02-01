package protect.card_locker;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class CatimaAppCompatActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context base) {
        // Apply chosen language
        super.attachBaseContext(Utils.updateBaseContextLocale(base));
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // material you themes uses background color for top bar, let the layout underneath show through
        if (Build.VERSION.SDK_INT >= 23) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().getDecorView().setSystemUiVisibility(Utils.isDarkModeEnabled(this) ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        } else {
            // icons are always white back then
            getWindow().setStatusBarColor(Utils.isDarkModeEnabled(this) ? Color.TRANSPARENT : Color.argb(127, 0, 0, 0));
        }
    }
}
