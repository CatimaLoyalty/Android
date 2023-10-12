package protect.card_locker;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;

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
        Window window = getWindow();
        if (window != null) {
            boolean darkMode = Utils.isDarkModeEnabled(this);
            if (Build.VERSION.SDK_INT >= 23) {
                View decorView = window.getDecorView();
                WindowInsetsControllerCompat wic = new WindowInsetsControllerCompat(window, decorView);
                wic.setAppearanceLightStatusBars(!darkMode);
                window.setStatusBarColor(Color.TRANSPARENT);
            } else {
                // icons are always white back then
                window.setStatusBarColor(darkMode ? Color.TRANSPARENT : Color.argb(127, 0, 0, 0));
            }
        }
        // XXX android 9 and below has a nasty rendering bug if the theme was patched earlier
        Utils.postPatchColors(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.setNavigationBarColor(this, null, Utils.resolveBackgroundColor(this), !Utils.isDarkModeEnabled(this));
    }

    protected void enableToolbarBackButton() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public void onMockedRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    }
}
