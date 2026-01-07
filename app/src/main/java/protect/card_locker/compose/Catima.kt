package protect.card_locker.compose

import androidx.activity.OnBackPressedDispatcher
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import protect.card_locker.R
import protect.card_locker.preferences.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatimaTopAppBar(title: String, onBackPressedDispatcher: OnBackPressedDispatcher?) {
    // Use pure black in OLED theme
    val context = LocalContext.current
    val settings = Settings(context)
    val isDarkMode = when (settings.theme) {
        AppCompatDelegate.MODE_NIGHT_NO -> false
        AppCompatDelegate.MODE_NIGHT_YES -> true
        else -> isSystemInDarkTheme()
    }

    val appBarColors = if (isDarkMode && settings.oledDark) {
        TopAppBarDefaults.topAppBarColors().copy(containerColor = Color.Black)
    } else {
        TopAppBarDefaults.topAppBarColors()
    }

    TopAppBar(
        modifier = Modifier.testTag("topbar_catima"),
        title = { Text(text = title) },
        colors = appBarColors,
        navigationIcon = {
            if (onBackPressedDispatcher != null) {
                IconButton(onClick = { onBackPressedDispatcher.onBackPressed() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }
        }
    )
}