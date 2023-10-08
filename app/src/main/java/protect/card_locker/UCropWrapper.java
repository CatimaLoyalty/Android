package protect.card_locker;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.textview.MaterialTextView;
import com.yalantis.ucrop.UCropActivity;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.WindowInsetsControllerCompat;

public class UCropWrapper extends UCropActivity {
    public static final String UCROP_TOOLBAR_TYPEFACE_STYLE = "ucop_toolbar_typeface_style";

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        boolean darkMode = Utils.isDarkModeEnabled(this);
        Window window = getWindow();
        // setup status bar to look like the rest of the app
        if (Build.VERSION.SDK_INT >= 23) {
            if (window != null) {
                View decorView = window.getDecorView();
                WindowInsetsControllerCompat wic = new WindowInsetsControllerCompat(window, decorView);
                wic.setAppearanceLightStatusBars(!darkMode);
            }
        } else {
            // icons are always white back then
            if (window != null && !darkMode) {
                window.setStatusBarColor(ColorUtils.compositeColors(Color.argb(127, 0, 0, 0), window.getStatusBarColor()));
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
                        int colorSurface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface, ContextCompat.getColor(this, R.color.md_theme_light_surface));
                        int colorOnSurface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, ContextCompat.getColor(this, R.color.md_theme_light_onSurface));

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

        // change toolbar font
        check = findViewById(com.yalantis.ucrop.R.id.toolbar_title);
        if (check instanceof MaterialTextView) {
            MaterialTextView toolbarTextview = (MaterialTextView) check;
            Intent intent = getIntent();
            int style = intent.getIntExtra(UCROP_TOOLBAR_TYPEFACE_STYLE, -1);
            if (style != -1) {
                toolbarTextview.setTypeface(Typeface.defaultFromStyle(style));
            }
        }
    }
}
