package protect.card_locker;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.google.android.material.color.MaterialColors;
import com.yalantis.ucrop.UCropActivity;

public class UCropWrapper extends UCropActivity {
    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        boolean darkMode = Utils.isDarkModeEnabled(this);
        // setup status bar to look like the rest of the app
        if (Build.VERSION.SDK_INT >= 23) {
            getWindow().getDecorView().setSystemUiVisibility(darkMode ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        } else {
            // icons are always white back then
            if (!darkMode) {
                getWindow().setStatusBarColor(ColorUtils.compositeColors(Color.argb(127, 0, 0, 0), getWindow().getStatusBarColor()));
            }
        }

        // find and check views that we wish to color modify
        // for when we update ucrop or switch to another cropper
        View check = findViewById(com.yalantis.ucrop.R.id.wrapper_controls);
        if (check instanceof FrameLayout) {
            FrameLayout controls = (FrameLayout) check;
            check = findViewById(com.yalantis.ucrop.R.id.wrapper_states);
            if (check instanceof LinearLayout) {
                LinearLayout states = (LinearLayout) check;
                for (int i = 0; i < controls.getChildCount(); i++) {
                    check = controls.getChildAt(i);
                    if (check instanceof AppCompatImageView) {
                        AppCompatImageView controlsBackgroundImage = (AppCompatImageView) check;
                        // everything gathered and are as expected, now perform color patching
                        Utils.patchColors(this);
                        int colorSurface = MaterialColors.getColor(this, R.attr.colorSurface, ContextCompat.getColor(this, R.color.md_theme_light_surface));
                        int colorOnSurface = MaterialColors.getColor(this, R.attr.colorOnSurface, ContextCompat.getColor(this, R.color.md_theme_light_onSurface));

                        Drawable controlsBackgroundImageDrawable = controlsBackgroundImage.getBackground();
                        controlsBackgroundImageDrawable.mutate();
                        controlsBackgroundImageDrawable.setTint(darkMode ? colorOnSurface : colorSurface);
                        controlsBackgroundImage.setBackgroundDrawable(controlsBackgroundImageDrawable);

                        states.setBackgroundColor(darkMode ? colorSurface : colorOnSurface);
                        break;
                    }
                }
            }
        }
    }
}
