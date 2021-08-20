package protect.card_locker.preferences;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import protect.card_locker.R;

public class myAppCompatActivity extends AppCompatActivity {

    SharedPreferences pref;
//
        @Override
    public Resources.Theme getTheme() {
        Resources.Theme theme = super.getTheme();
            pref = PreferenceManager
                    .getDefaultSharedPreferences(getApplicationContext());
            String themeName = pref.getString("pref_theme_color", "App theme");
            switch (themeName){
                case "Pink theme":
                    theme.applyStyle(R.style.AppTheme_pink, true);
                    break;
                case "Red theme":
                    theme.applyStyle(R.style.AppTheme_red, true);
                    break;
                case "Dark pink theme":
                    theme.applyStyle(R.style.AppTheme_dark_pink, true);
                    break;
                case "Violet theme":
                    theme.applyStyle(R.style.AppTheme_violet, true);
                    break;
                case "Blue theme":
                    theme.applyStyle(R.style.AppTheme_blue, true);
                    break;
                case "Sky blue theme":
                    theme.applyStyle(R.style.AppTheme_sky_blue, true);
                    break;
                case "Green theme":
                    theme.applyStyle(R.style.AppTheme_green, true);
                    break;
                case "Grey theme":
                    theme.applyStyle(R.style.AppTheme_grey, true);
                    break;
                case "Brown theme":
                    theme.applyStyle(R.style.AppTheme_brown, true);
                    break;
                default:
                    theme.applyStyle(R.style.AppTheme_NoActionBar, true);

            }
        // you could also use a switch if you have many themes that could apply
        return theme;
    }
}
