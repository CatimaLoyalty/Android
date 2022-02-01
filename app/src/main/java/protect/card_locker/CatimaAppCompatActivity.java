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
        // XXX on the splash screen activity, aka the main activity, this has to be executed after applying dynamic colors, not before
        // so running this only on non main for now
        if (!this.getClass().getSimpleName().equals(MainActivity.class.getSimpleName())) {
            Utils.patchOledDarkTheme(this);
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // material 3 designer does not consider status bar colors
        // XXX changing this in onCreate causes issues with the splash screen activity, so doing this here
        if (Build.VERSION.SDK_INT >= 23) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().getDecorView().setSystemUiVisibility(Utils.isDarkModeEnabled(this) ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        } else {
            // icons are always white back then
            getWindow().setStatusBarColor(Utils.isDarkModeEnabled(this) ? Color.TRANSPARENT : Color.argb(127, 0, 0, 0));
        }
        // XXX android 9 and below has a nasty rendering bug if the theme was patched earlier
        // the splash screen activity needs the fix regardless to solve a dynamic color api issue
        if (!this.getClass().getSimpleName().equals(MainActivity.class.getSimpleName())) {
            Utils.postPatchOledDarkTheme(this);
        }
    }
}
