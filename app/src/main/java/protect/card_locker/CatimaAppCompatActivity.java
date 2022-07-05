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
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.patchColors(this);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // material 3 designer does not consider status bar colors
        // XXX changing this in onCreate causes issues with the splash screen activity, so doing this here
        boolean darkMode = Utils.isDarkModeEnabled(this);
        if (Build.VERSION.SDK_INT >= 23) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().getDecorView().setSystemUiVisibility(darkMode ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        } else {
            // icons are always white back then
            getWindow().setStatusBarColor(darkMode ? Color.TRANSPARENT : Color.argb(127, 0, 0, 0));
        }
        // XXX android 9 and below has a nasty rendering bug if the theme was patched earlier
        Utils.postPatchColors(this);
    }
}
