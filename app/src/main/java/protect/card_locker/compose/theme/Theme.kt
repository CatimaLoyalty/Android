package protect.card_locker.compose.theme

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import protect.card_locker.R
import protect.card_locker.preferences.Settings

@Composable
fun CatimaTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val settings = Settings(context)

    val isDynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val lightTheme = if (isDynamicColorSupported) {
        dynamicLightColorScheme(context)
    } else {
        lightColorScheme(primary = colorResource(id = R.color.md_theme_light_primary))
    }

    val darkTheme = if (isDynamicColorSupported) {
        dynamicDarkColorScheme(context)
    } else {
        darkColorScheme(primary = colorResource(id = R.color.md_theme_dark_primary))
    }

    val colorScheme = when (settings.theme) {
        AppCompatDelegate.MODE_NIGHT_NO -> lightTheme
        AppCompatDelegate.MODE_NIGHT_YES -> darkTheme
        else -> if (isSystemInDarkTheme()) darkTheme else lightTheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}